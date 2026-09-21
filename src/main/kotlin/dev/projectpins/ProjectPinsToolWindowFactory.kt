package dev.projectpins

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class ProjectPinsToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = PinsPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        content.setDisposer(panel)
        toolWindow.contentManager.addContent(content)
    }
}

private class PinsPanel(private val project: Project) : JBPanel<PinsPanel>(BorderLayout()), Disposable {
    private val service = ProjectPinsService.getInstance(project)
    private val chips = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(4)))
    init {
        // One compact, horizontally scrollable row; it never grows taller as pins are added.
        add(JBScrollPane(chips, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.NORTH)
        project.messageBus.connect(this).subscribe(ProjectPinsService.TOPIC, PinsListener { refresh() })
        refresh()
    }
    private fun refresh() {
        chips.removeAll()
        val pins = service.pins()
        if (pins.isEmpty()) chips.add(JBLabel("Right-click a file or folder in Project → Pin to Project Pins"))
        pins.forEach { pin ->
            val file = service.resolve(pin.id)
            val icon = when {
                file == null -> AllIcons.General.Warning
                file.isDirectory -> AllIcons.Nodes.Folder
                else -> file.fileType.icon
            }
            val button = JButton(pin.name, icon).apply {
                margin = JBUI.insets(2, 6)
                toolTipText = "${if (file == null) "Missing target: " else ""}${pin.targetPath} · Right-click to manage"
                accessibleContext.accessibleDescription = toolTipText
                addActionListener { ProjectPinsNavigator.navigate(project, pin.id) }
            }
            fun popup() {
                ActionManager.getInstance().createActionPopupMenu("ProjectPins.Menu", menu(pin)).component.show(button, 0, button.height)
            }
            button.addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) { if (e.isPopupTrigger) popup() }
                override fun mouseReleased(e: MouseEvent) { if (e.isPopupTrigger) popup() }
            })
            button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("shift F10"), "manage")
            button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("CONTEXT_MENU"), "manage")
            button.actionMap.put("manage", object : AbstractAction() { override fun actionPerformed(e: ActionEvent) = popup() })
            chips.add(button)
        }
        chips.revalidate()
        chips.repaint()
    }
    private fun menu(pin: ProjectPin): DefaultActionGroup {
        val group = DefaultActionGroup()
        fun action(label: String, enabled: Boolean = true, run: () -> Unit) {
            group.add(object : DumbAwareAction(label) {
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
                override fun update(e: AnActionEvent) { e.presentation.isEnabled = enabled }
                override fun actionPerformed(e: AnActionEvent) = run()
            })
        }
        action("Go to") { ProjectPinsNavigator.navigate(project, pin.id) }
        action("Rename…") {
            val name = Messages.showInputDialog(project, "Pin name:", "Rename Project Pin", null, pin.name, null)
            if (!name.isNullOrBlank()) service.update(pin.id, name, pin.openInEditor)
        }
        action("Edit pin settings…") {
            val dialog = PinEditorDialog(project, pin.name, pin.directory, pin.openInEditor, pin.targetPath)
            if (dialog.showAndGet()) service.update(pin.id, dialog.pinName, dialog.openInEditor, dialog.targetPath)
        }
        if (!pin.directory) group.add(object : DumbAwareToggleAction("Open in editor") {
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
            override fun isSelected(e: AnActionEvent) = service.pins().find { it.id == pin.id }?.openInEditor == true
            override fun setSelected(e: AnActionEvent, state: Boolean) = service.update(pin.id, pin.name, state)
        })
        group.addSeparator()
        val index = service.pins().indexOfFirst { it.id == pin.id }
        action("Move left", index > 0) { service.move(pin.id, -1) }
        action("Move right", index < service.pins().lastIndex) { service.move(pin.id, 1) }
        group.addSeparator()
        action("Remove") { service.remove(pin.id) }
        return group
    }
    override fun dispose() = Unit
}
