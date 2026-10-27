package dev.projectpins

import com.intellij.ide.projectView.ProjectView
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager

object ProjectPinsNavigator {
    fun openFileAndScrollToTop(project: Project, id: String) {
        val service = ProjectPinsService.getInstance(project)
        val file = service.resolve(id) ?: return
        if (file.isDirectory) return
        val manager = FileEditorManager.getInstance(project)
        manager.openFile(file, true)
        val editor = manager.selectedTextEditor ?: return
        manager.runWhenLoaded(editor) {
            editor.caretModel.moveToOffset(0)
            editor.scrollingModel.scrollTo(LogicalPosition(0, 0), ScrollType.MAKE_VISIBLE)
            editor.scrollingModel.scrollVertically(0)
        }
    }

    fun navigate(project: Project, id: String) {
        if (project.isDisposed) return
        val service = ProjectPinsService.getInstance(project)
        val pin = service.pins().find { it.id == id } ?: return
        val file = service.resolve(id)
        if (file == null) {
            notify(project, "Target not found: ${pin.targetPath}. Right-click the pin to edit its path or remove it.")
            return
        }
        val manager = ToolWindowManager.getInstance(project)
        val toolWindow = manager.getToolWindow(ToolWindowId.PROJECT_VIEW)
        if (toolWindow == null) {
            notify(project, "Project View is not available in this project.")
            return
        }
        val select = Runnable {
            if (!project.isDisposed && file.isValid) {
                val view = ProjectView.getInstance(project)
                view.changeView("ProjectPane")
                // Platform selection expands ancestors and does not simulate a click on a tree node.
                view.selectCB(file, file, pin.openInEditor && !file.isDirectory)
                    .doWhenDone {
                        if (!project.isDisposed && file.isValid && !file.isDirectory && pin.openInEditor) {
                            FileEditorManager.getInstance(project).openFile(file, false)
                        }
                    }
                    .doWhenRejected(Runnable { notify(project, "Cannot reveal ${pin.name} in Project View. Check that the target is visible in the project.") })
            }
        }
        // Keep focus on the pin for reveal-only navigation: Project View
        // autoscroll-to-source responds to selection while its tree has focus.
        if (pin.openInEditor && !file.isDirectory) toolWindow.activate(select, true)
        else toolWindow.show(select)
    }

    private fun notify(project: Project, message: String) {
        if (!project.isDisposed) NotificationGroupManager.getInstance().getNotificationGroup("Project Pins")
            .createNotification(message, NotificationType.WARNING).notify(project)
    }
}
