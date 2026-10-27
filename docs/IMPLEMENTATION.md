# Implementation notes

The plugin targets WebStorm 2025.3 / IntelliJ Platform build 253 and uses platform APIs available from that SDK, except that placement of the pins row necessarily wraps the existing Project tool-window Swing component.

## State

`ProjectPinsService` is a project-level `PersistentStateComponent`. It stores `.idea/projectPins.xml`, returns defensive copies, and treats list position as display order. Project-local URLs are converted to relative paths on save; external targets retain full VFS URLs.

Each pin owns a disposable `VirtualFilePointer`. It follows moves and renames observed by the running IDE. A changed pointer URL is normalized back to a relative path by `getState()`. Offline moves and delete/create replacements cannot be guaranteed.

The service publishes `ProjectPinsService.TOPIC` on the UI thread after mutations and relevant VFS events. UI components subscribe with their own disposable.

## Project View integration

`ProjectPinsProjectViewInstaller` is a `ProjectActivity`. After the Project tool window exists, it creates one wrapper containing:

1. `ProjectPinsRow` in `BorderLayout.NORTH`.
2. The original shared Project View component in `BorderLayout.CENTER`.

The wrapper is stored in project user data and assigned to every `Content`. The Project View uses a shared component for its tabs; creating a wrapper per content reparents that shared component and can leave the tree blank. Preserve the one-wrapper invariant.

The row uses `InplaceButton` and the current IntelliJ action-button hover color. A native `JBPopupFactory` action popup supplies edit and remove actions. Badge font size is reduced only until the measured text fits within the width allocated for one to four characters.

## Navigation

Single-click navigation resolves the pointer, activates or shows the Project tool window, switches to `ProjectPane`, and calls `ProjectView.selectCB`. For file pins with `openInEditor`, it opens the file after selection succeeds. Reveal-only pins avoid forcing tree focus because Project View's autoscroll settings can otherwise open a file.

Double-click calls `openFileAndScrollToTop`: files are opened regardless of `openInEditor`, then the selected text editor is moved to offset zero and vertically scrolled to zero after loading. Directories ignore the second-click action beyond normal navigation.

## Dialog and duplicates

The create action accepts exactly one valid `VirtualFile`. `findByTarget` prevents duplicate paths and reports an informational notification. New file pins default to opening in the editor. Directories never expose or persist this option. Editing can change the label, stored path, and the file-only option.

## Compatibility risks

The row location has no dedicated public extension point, so changes to Project tool-window content ownership are the main upgrade risk. On every new platform line, verify tree visibility, all Project View tabs, content switching, project reopening, hover painting, popup positioning, and disposal errors before widening `untilBuild`.
