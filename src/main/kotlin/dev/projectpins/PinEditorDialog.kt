package dev.projectpins

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class PinEditorDialog(project: Project, name: String, directory: Boolean, open: Boolean,
                      path: String? = null) : DialogWrapper(project) {
    private val nameField = JBTextField(name, 28)
    private val pathField = path?.let { JBTextField(it, 28) }
    private val openField = if (directory) null else JBCheckBox("Open in editor", open)
    val pinName: String get() = nameField.text.trim()
    val openInEditor: Boolean get() = openField?.isSelected == true
    val targetPath: String? get() = pathField?.text?.trim()

    init {
        title = if (path == null) "Pin to Project Pins" else "Edit Project Pin"
        setOKButtonText(if (path == null) "Pin" else "Save")
        init()
    }
    override fun createCenterPanel(): JComponent = panel {
        row("Name:") { cell(nameField).focused() }
        pathField?.let { field -> row("Target:") { cell(field).comment("Project-relative path or file:// URL. Edit to repair a missing target.") } }
        openField?.let { field -> row { cell(field) } }
    }
    override fun getPreferredFocusedComponent(): JComponent = nameField
    override fun doValidate(): ValidationInfo? = when {
        pinName.isEmpty() -> ValidationInfo("Enter a pin name.", nameField)
        pathField != null && targetPath.isNullOrEmpty() -> ValidationInfo("Enter a target path.", pathField)
        else -> null
    }
}
