# Project Pins

Small project-scoped shortcuts to files and folders in JetBrains IDEs. Right-click a file or folder in the Project tree, choose **Pin to Project Pins**, give it a name, and click **Pin**. Pins appear as compact icon buttons in the Project View toolbar.

Click a pin to expand its ancestors and select it in the standard Project tree. Pins are exposed as icon-only actions in the Project View toolbar. Each icon keeps the native file/folder icon and adds a small 1–4 character badge from the pin name; the font scales down as the badge gets longer. The full name and path are available as the tooltip.

Right-click a button (or focus it and press Shift+F10) to go to, rename, edit settings/repair the target path, toggle editor opening, move left/right, or remove it. Missing targets display a warning icon and report a non-blocking notification when clicked.

## Screenshots

Placeholder: add screenshots of the create dialog, compact panel, and revealed Project tree after final UI acceptance testing.

## Development

Requires JDK 21, network access for first dependency resolution, and macOS/Linux/Windows. The Gradle wrapper supplies Gradle 9.1.0. Built with Kotlin 2.2.20 and IntelliJ Platform Gradle Plugin 2.19.0. The baseline is WebStorm 2025.3.1.1 (build 253); only IntelliJ Platform APIs are used. Other 253-based IDEs should be evaluated with Plugin Verifier before distributing to them.

```sh
export JAVA_HOME=/path/to/jdk-21
./gradlew build
./gradlew test
./gradlew buildPlugin
./gradlew verifyPlugin
./gradlew runIde
```

To use an existing WebStorm installation instead of downloading the SDK, append `-PlocalIdePath=/Applications/WebStorm.app` to these commands. Do not commit machine-specific SDK paths. `runIde` uses an isolated sandbox under `build/idea-sandbox`.

The installable archive is `build/distributions/ProjectPins-0.1.0.zip`. In your IDE, open **Settings → Plugins → gear menu → Install Plugin from Disk**, choose the ZIP, and restart if prompted.

## Architecture

- `ProjectPinsService`: project-level persistence, ordered snapshots, VFS pointers and change notifications.
- `ProjectPin` / `PinsState`: simple XML-serializable beans, defensively copied by the service.
- `ProjectPinsNavigator`: resolves the target, invokes `ProjectView.selectCB`, and optionally invokes `FileEditorManager.openFile` after selection completes.
- `PinToProjectPinsAction` / `PinEditorDialog`: Project View context action and native IntelliJ dialog.
- `ProjectPinsToolbarActionGroup`: native Project View toolbar group with icon-only actions and short text badges.

No reflection, Project tree traversal or Swing injection into another tool window. Changes run on the EDT; cached VFS lookups do not synchronously refresh the filesystem. The platform handles asynchronous tree expansion. VFS listeners and pointer lifetimes are bound to disposables.

## State and paths

Pins are stored in `.idea/projectPins.xml` through `PersistentStateComponent`. Each pin stores its ID, name, target path, directory flag and editor preference. List position determines order. Paths under the project base directory are relative, including `.` for the root; external targets use a VFS URL. Treat external URLs as machine-specific.

`VirtualFilePointerManager` tracks IDE-observed file and parent-folder renames/moves. Current pointer URLs are converted back to relative paths on state save, preserving tracked moves across restarts. Removing a pin disposes its pointer; closing the project disposes the service and its children. Custom names remain unchanged after a rename.

## Verification and limitations

Platform fixture tests cover XML round trips, ordering and boundaries, settings and snapshot isolation, missing targets and repair, relative/external path resolution, and pointer tracking through rename/move and serialization. See [manual acceptance checklist](docs/ACCEPTANCE.md) and [implementation decisions](docs/IMPLEMENTATION.md).

- The panel uses a supported separate secondary tool window; it is not injected into the Project toolbar. Resize/dock it using normal IDE controls. Many pins scroll horizontally.
- Reveal-only pins keep keyboard focus in the pins panel to avoid triggering Project View's autoscroll-to-source. The selected tree row may therefore use the inactive selection color.
- Moves while the IDE is closed, external moves reported as delete/create, excluded/hidden targets, and unavailable external roots may not be recoverable. Edit a pin's target path or remove it.
- Paths outside the project directory use URLs, even for external module content roots. The MVP does not assign portable identifiers to arbitrary content roots.
- No drag-and-drop reorder, multi-selection pin creation, localization, remote-development/split-mode support, or automatic offline target search.
- Declared compatibility is intentionally limited to build 253 until newer platform lines are verified.

## Marketplace preparation

Before publication, replace the vendor placeholder in `plugin.xml`, confirm ownership of the plugin ID, choose/add a license, provide a plugin icon, real screenshots, support links and a privacy description. Update the version in `build.gradle.kts` and maintain release notes in `CHANGELOG.md`; copy the release's notes into `pluginConfiguration.changeNotes` for the packaged descriptor.

Run `build`, `verifyPlugin`, and the manual acceptance checklist against every advertised IDE/version. Expand `pluginVerification.ides` and the compatibility range together. Inspect verifier reports under `build/reports/pluginVerifier`.

Publishing is configured but is not part of the build:

```sh
export JETBRAINS_MARKETPLACE_TOKEN='your-token'
./gradlew publishPlugin
```

Read the token from an environment variable or CI secret only. Never commit it. Add Marketplace signing via environment-backed `intellijPlatform.signing` certificate/private-key/password properties if required by your release process. No publication is performed automatically.
