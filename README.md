# Project Pins

Project Pins adds a compact row of shortcuts directly above the standard **Project** tree in JetBrains IDEs. Right-click one file or folder in the tree, choose **Pin to Project Pins**, set its label, and create the pin.

Each button keeps the target's native file or folder icon and overlays up to four characters from the pin name. The badge width and font size are calculated from the actual text width. The full pin name and stored path are shown in the tooltip.

## Behaviour

- A single click selects the target and expands its parents in the standard Project tree.
- New file pins have **Open in editor** enabled by default. It can be disabled while creating or editing the pin.
- Folder pins never show or store the editor-opening option.
- A double click always opens a file, even if **Open in editor** is disabled, then moves its caret and scroll position to the top.
- Right-click opens a native IntelliJ popup with **Edit…** and **Remove**.
- The same file or folder cannot be pinned twice.
- Missing targets use a warning icon. Their stored path can be repaired through **Edit…**.
- Pins and their order are stored per project in `.idea/projectPins.xml`.
- IDE-observed file and parent-folder renames or moves are tracked with `VirtualFilePointer`.

The row is hidden when the project has no pins and scrolls horizontally when it runs out of space.

## Installation

Build the plugin with JDK 21 and the installed WebStorm SDK:

```sh
export JAVA_HOME=/path/to/jdk-21
./gradlew clean buildPlugin -PlocalIdePath=/Applications/WebStorm.app
```

The installable archive is written to `build/distributions/ProjectPins-<version>.zip` (currently `ProjectPins-0.3.7.zip`). In the IDE, open **Settings → Plugins → gear menu → Install Plugin from Disk**, select the ZIP, and restart the IDE if requested. Installing a newer ZIP replaces the installed version.

Without `-PlocalIdePath`, Gradle downloads the configured WebStorm SDK on first use:

```sh
./gradlew buildPlugin
```

## Development

The project uses JDK 21, Kotlin 2.2.20, Gradle 9.1.0, and IntelliJ Platform Gradle Plugin 2.19.0. Its baseline SDK is WebStorm 2025.3.1.1 and declared compatibility is limited to build `253.*`.

```sh
./gradlew compileKotlin -PlocalIdePath=/Applications/WebStorm.app
./gradlew test -PlocalIdePath=/Applications/WebStorm.app
./gradlew buildPlugin -PlocalIdePath=/Applications/WebStorm.app
./gradlew verifyPlugin -PlocalIdePath=/Applications/WebStorm.app
./gradlew runIde -PlocalIdePath=/Applications/WebStorm.app
```

Use `--offline` after dependencies are cached. `runIde` starts an isolated IDE under `build/idea-sandbox`. Do not commit a machine-specific SDK path.

## Architecture

- `ProjectPinsService` owns project state, ordered snapshots, VFS pointers, duplicate detection, and change notifications.
- `ProjectPin` and `PinsState` are XML-serializable state beans. Service callers receive defensive copies.
- `ProjectPinsProjectViewInstaller` wraps the shared Project View content once and inserts `ProjectPinsRow` above it. All Project View tabs must keep the same wrapper; wrapping each tab separately can make the tree disappear.
- `ProjectPinsRow` renders the horizontal row, native popup, hover state, and custom badges.
- `ProjectPinsNavigator` reveals targets through `ProjectView.selectCB`, optionally opens files, and handles double-click scrolling.
- `PinToProjectPinsAction` and `PinEditorDialog` provide the Project View context action and create/edit dialog.

The Project row is currently implemented through the Project tool window's Swing content because the IntelliJ Platform does not expose a public extension point for this exact location. Treat this integration as version-sensitive and verify it in a sandbox whenever the platform version changes.

## State and path rules

Paths inside the project are stored relative to the project root; `.` represents the root itself. Targets outside the project use their full VFS URL and may be machine-specific. List position is the saved display order.

Moves performed while the IDE is closed, delete-and-recreate operations, hidden or excluded files, and unavailable external roots may leave a pin unresolved. The user can edit the target path or remove the pin.

## Verification and release

Platform fixture tests cover persistence, ordering, settings, defensive snapshots, missing-target repair, relative/external paths, and VFS pointer tracking. UI behavior still requires the [manual acceptance checklist](docs/ACCEPTANCE.md). See [implementation notes](docs/IMPLEMENTATION.md) and [AI development guide](robots.md).

Before publication, replace the vendor placeholder in `plugin.xml`, confirm ownership of the plugin ID, add a license, plugin icon, screenshots, support links, and a privacy description. Update the version and changelog, then run tests, `buildPlugin`, Plugin Verifier, and the manual checks against every advertised IDE version.

Publishing requires `JETBRAINS_MARKETPLACE_TOKEN`; keep it in an environment variable or CI secret:

```sh
./gradlew publishPlugin
```
