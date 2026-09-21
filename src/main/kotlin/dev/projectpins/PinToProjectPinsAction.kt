package dev.projectpins

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType

class PinToProjectPinsAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = e.project != null && files?.size == 1 && files[0].isValid
    }
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val service = ProjectPinsService.getInstance(project)
        service.findByTarget(file)?.let { existing ->
            NotificationGroupManager.getInstance().getNotificationGroup("Project Pins")
                .createNotification("Этот файл или папка уже добавлены как «${existing.name}».", NotificationType.INFORMATION)
                .notify(project)
            return
        }
        val dialog = PinEditorDialog(project, file.name, file.isDirectory, false)
        if (dialog.showAndGet() && file.isValid) {
            ProjectPinsService.getInstance(project).add(file, dialog.pinName, dialog.openInEditor)
            ToolWindowManager.getInstance(project).getToolWindow("Project Pins")?.show()
        }
    }
}
