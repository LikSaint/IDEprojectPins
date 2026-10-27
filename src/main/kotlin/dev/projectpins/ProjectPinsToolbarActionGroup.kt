package dev.projectpins

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.JBColor
import com.intellij.ui.InplaceButton
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

/** Adds one compact Project Pins row above the standard Project View content. */
class ProjectPinsProjectViewInstaller : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        val manager = ToolWindowManager.getInstance(project)
        project.messageBus.connect(project).subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun toolWindowsRegistered(ids: List<String>, toolWindowManager: ToolWindowManager) {
                if (ToolWindowId.PROJECT_VIEW in ids) toolWindowManager.invokeLater { install(project) }
            }
        })
        manager.invokeLater { install(project) }
    }

    private fun install(project: Project) {
        if (project.isDisposed) return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW) ?: return
        val contents = toolWindow.contentManager.contents
        if (contents.isEmpty()) return
        val wrapper = project.getUserData(WRAPPER_KEY) ?: createWrapper(project, toolWindow, contents.first().component)
        if (project.getUserData(LISTENER_INSTALLED_KEY) != true) {
            val listener = object : ContentManagerListener {
                override fun contentAdded(event: ContentManagerEvent) {
                    event.content.component = wrapper
                }
            }
            toolWindow.contentManager.addContentManagerListener(listener)
            Disposer.register(project) {
                if (!toolWindow.contentManager.isDisposed) {
                    toolWindow.contentManager.removeContentManagerListener(listener)
                }
            }
            project.putUserData(LISTENER_INSTALLED_KEY, true)
        }
        contents.forEach { it.component = wrapper }
    }

    private fun createWrapper(project: Project, toolWindow: ToolWindow, original: JComponent): JComponent {
        val row = ProjectPinsRow(project)
        val wrapper = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(row, BorderLayout.NORTH)
            add(original, BorderLayout.CENTER)
        }
        project.putUserData(WRAPPER_KEY, wrapper)
        Disposer.register(toolWindow.disposable, row)
        return wrapper
    }

    companion object {
        private val WRAPPER_KEY = Key.create<JComponent>("ProjectPins.projectViewWrapper")
        private val LISTENER_INSTALLED_KEY = Key.create<Boolean>("ProjectPins.projectViewListenerInstalled")
    }
}

private class ProjectPinsRow(private val project: Project) : JPanel(BorderLayout()), Disposable {
    private val service = ProjectPinsService.getInstance(project)
    private val buttons = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(2), JBUI.scale(2))).apply {
        isOpaque = false
    }

    init {
        isOpaque = false
        border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)
        val scrollPane = JBScrollPane(
            buttons,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED,
        ).apply {
            isOpaque = false
            viewport.isOpaque = false
            border = JBUI.Borders.empty()
            preferredSize = Dimension(0, JBUI.scale(28))
        }
        add(scrollPane, BorderLayout.CENTER)
        project.messageBus.connect(this).subscribe(ProjectPinsService.TOPIC, PinsListener { refresh() })
        refresh()
    }

    private fun refresh() {
        buttons.removeAll()
        val pins = service.pins()
        pins.forEach { pin -> buttons.add(createButton(pin)) }
        isVisible = pins.isNotEmpty()
        revalidate()
        repaint()
    }

    private fun createButton(pin: ProjectPin): InplaceButton {
        val file = service.resolve(pin.id)
        val tooltip = "${pin.name} · ${pin.targetPath}"
        val icon = PinIcon(file, pin.name, file == null)
        val button = object : InplaceButton(tooltip, icon, {
            ProjectPinsNavigator.navigate(project, pin.id)
        }) {
            override fun paintHover(graphics: Graphics) {
                super.paintHover(graphics, JBUI.CurrentTheme.ActionButton.hoverBackground())
            }
        }.apply {
            preferredSize = Dimension(icon.iconWidth + JBUI.scale(8), icon.iconHeight + JBUI.scale(4))
            minimumSize = preferredSize
            maximumSize = preferredSize
            isFocusable = false
            accessibleContext.accessibleName = pin.name
            accessibleContext.accessibleDescription = tooltip
            setHoveringEnabled(true)
        }
        button.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) { if (event.isPopupTrigger) showMenu(button, pin.id, event) }
            override fun mouseReleased(event: MouseEvent) { if (event.isPopupTrigger) showMenu(button, pin.id, event) }
            override fun mouseClicked(event: MouseEvent) {
                if (event.button == MouseEvent.BUTTON1 && event.clickCount == 2) {
                    ProjectPinsNavigator.openFileAndScrollToTop(project, pin.id)
                }
            }
        })
        return button
    }

    private fun showMenu(button: JComponent, pinId: String, event: MouseEvent) {
        val pin = service.pins().find { it.id == pinId } ?: return
        val actions = DefaultActionGroup().apply {
            add(object : DumbAwareAction("Редактировать…") {
                override fun actionPerformed(event: AnActionEvent) {
                    val currentPin = service.pins().find { it.id == pin.id } ?: return
                    val dialog = PinEditorDialog(
                        project,
                        currentPin.name,
                        currentPin.directory,
                        currentPin.openInEditor,
                        currentPin.targetPath,
                    )
                    if (dialog.showAndGet()) {
                        service.update(currentPin.id, dialog.pinName, dialog.openInEditor, dialog.targetPath)
                    }
                }
            })
            addSeparator()
            add(object : DumbAwareAction("Удалить") {
                override fun actionPerformed(event: AnActionEvent) {
                    service.remove(pin.id)
                }
            })
        }
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            actions,
            DataManager.getInstance().getDataContext(button),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false,
        )
        popup.show(RelativePoint(event))
    }

    override fun dispose() = Unit
}

/** Native file/folder icon with a readable 1–4 character badge. */
private class PinIcon(private val file: VirtualFile?, label: String, private val broken: Boolean) : Icon {
    private val base: Icon = when {
        broken -> AllIcons.General.Warning
        file?.isDirectory == true -> AllIcons.Nodes.Folder
        file != null -> file.fileType.icon ?: AllIcons.FileTypes.Text
        else -> AllIcons.General.Warning
    }
    private val label = label.trim().take(4).ifEmpty { "?" }.uppercase()

    private val badgeWidth = when (label.length) {
        1 -> 18
        2 -> 20
        3 -> 23
        else -> 27
    }

    override fun getIconWidth() = JBUI.scale(badgeWidth)
    override fun getIconHeight() = JBUI.scale(20)

    override fun paintIcon(component: java.awt.Component?, graphicsSource: Graphics, x: Int, y: Int) {
        val graphics = graphicsSource.create() as Graphics2D
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val baseX = x + (iconWidth - base.iconWidth) / 2
            val baseY = y + (iconHeight - base.iconHeight) / 2
            base.paintIcon(component, graphics, baseX, baseY)
            val maxTextWidth = iconWidth - JBUI.scale(2)
            var size = 12
            while (size > 6) {
                graphics.font = graphics.font.deriveFont(Font.BOLD, JBUI.scale(size).toFloat())
                if (graphics.fontMetrics.stringWidth(label) + JBUI.scale(3) <= maxTextWidth) break
                size--
            }
            val metrics: FontMetrics = graphics.fontMetrics
            val width = metrics.stringWidth(label) + JBUI.scale(3)
            val height = metrics.height
            val left = x + iconWidth - width
            val top = y + iconHeight - height + JBUI.scale(1)
            graphics.color = if (broken) JBColor(Color(0xD14B4B), Color(0x8F3030))
                else JBColor(Color(0xD9E2EC), Color(0x566273))
            graphics.fill(RoundRectangle2D.Float(left.toFloat(), top.toFloat(), width.toFloat(), height.toFloat(), 5f, 5f))
            graphics.color = if (broken) JBColor.WHITE else JBColor(Color(0x1F2937), Color.WHITE)
            graphics.drawString(label, left + JBUI.scale(1), top + metrics.ascent)
        } finally {
            graphics.dispose()
        }
    }
}
