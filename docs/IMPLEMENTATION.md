# Implementation plan and API research

1. Scaffold Kotlin + IntelliJ Platform Gradle Plugin 2.19.0, Gradle 9, JDK 21.
2. Target the installed stable WebStorm 2025.3.1.1 SDK, restrict declared compatibility to 253 until further verification.
3. Store project state through PersistentStateComponent in .idea/projectPins.xml. Keep immutable snapshots at the service boundary and list order as pin order.
4. Use VirtualFilePointerManager pointers owned by per-pin disposables to follow VFS moves/renames. Convert pointer URLs to project-relative paths when saving. External targets retain URLs. Offline moves cannot be tracked.
5. Use ProjectView.changeView + selectCB to select/expand through the platform, and FileEditorManager.openFile only for enabled file pins. No tree traversal or settings mutation.
6. Register a secondary left tool window containing compact buttons. The documented tool-window API offers a stable adjacent panel; no documented public insertion slot for arbitrary chips inside the existing Project toolbar was found. Avoid modifying its Swing hierarchy.
7. Add a ProjectViewPopupMenu action, native DialogWrapper editor, per-button management menus, keyboard menu access, and explicit reorder actions.
8. Exercise persistence, order, settings, resolution, missing targets and pointer tracking using platform tests. Build ZIP, run verifier, launch a sandbox IDE if available.

Sources reviewed:
- https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
- https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html
- https://plugins.jetbrains.com/docs/intellij/action-system.html
- https://plugins.jetbrains.com/docs/intellij/tool-windows.html
- https://github.com/JetBrains/intellij-community/blob/master/platform/lang-impl/src/com/intellij/ide/projectView/ProjectView.java
- Installed SDK and matching JetBrains source for ProjectView selection and VirtualFilePointer APIs.

The platform's programmatic selection API is used instead of synthesizing mouse/key events, which can trigger single-click editor navigation. Verify with Project View single-click opening enabled during manual QA.
