# Manual acceptance checklist

Run an isolated `runIde` sandbox with a sample project containing nested source files and folders. Repeat the navigation checks with Project View's single-click opening and autoscroll/select-opened-file options both enabled and disabled.

- Create a file pin. Confirm **Open in editor** is selected by default, the pin appears in the row, and trying to pin the same target again shows a notification without adding a duplicate.
- Create a folder pin. Confirm the dialog has no editor checkbox and clicking the pin selects and expands to the folder without opening an editor.
- Clear **Open in editor** for a file pin. Single-click it after selecting another editor tab; the Project tree reveals the target without changing tabs.
- Enable **Open in editor** for a file pin. Single-click it and confirm the tree reveals it and the editor opens.
- Double-click a file pin with the option disabled. Confirm the file opens and its caret and scroll position move to the first line. Repeat when the file is already open and scrolled down.
- Hover each button and confirm its background matches native toolbar-button hover styling.
- Test labels with 1, 2, 3, and 4 characters, including narrow (`i18n`) and wide (`MMMM`) glyphs. Confirm the badge is centered, readable, and does not clip.
- Right-click a pin. Confirm the popup uses native IDE styling and contains only **Edit…** and **Remove**.
- Edit the label, file-only preference, and target path. Confirm changes appear immediately and a missing target can be repaired.
- Remove pins from the beginning, middle, and end. Confirm the row disappears after removing the last pin.
- Add enough pins to overflow horizontally. Confirm the row stays one line high and the Project tree retains its normal width and content.
- Switch between every Project View tab/content. Collapse and reopen the tool window and project. Confirm the same tree remains visible and interactive; no blank gray view appears.
- Rename a pinned file and move its parent inside the IDE, then restart. Confirm the pin still resolves and its custom label remains unchanged.
- Delete a target. Confirm its icon changes to a warning, clicking it reports a non-blocking notification, and editing/removing it still works.
- Restart the sandbox. Confirm paths, names, order, and file preferences survive in `.idea/projectPins.xml`.
- Exercise the plugin during indexing and inspect `idea.log` after project close for threading or disposal errors.

Record executed checks and gaps in `VERIFICATION.md` when preparing a release; this checklist itself is not proof of execution.
