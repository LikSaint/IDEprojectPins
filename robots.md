# Project Pins: guide for AI-assisted development

This file is the working context for future AI coding sessions. Read it together with `README.md`, `CHANGELOG.md`, and the source before changing the project. The source is authoritative when documentation and behavior disagree.

## Product contract

Project Pins is a JetBrains IDE plugin that puts one compact horizontal shortcut row directly above the standard **Project** tree. It is not a separate tool window and should not consume title-toolbar space.

Users create a pin from the Project View context menu. Pins target one file or folder and have a custom name. The visible icon uses the native target icon plus an uppercase badge of at most four characters.

Required interaction:

- Single left click reveals the target in the Project tree. It opens a file only when that pin's `openInEditor` setting is enabled.
- Double left click always opens a file, then moves the caret and scroll position to the top. Directories are only revealed.
- New file pins default `openInEditor` to `true`; users can clear it.
- Directory dialogs do not show the editor option and directory state must always persist it as `false`.
- Right click uses a native IntelliJ popup with exactly **Редактировать…** and **Удалить**, separated by a divider.
- The same resolved file or directory cannot be added twice.
- Hover must look like a native IntelliJ toolbar action.
- The row is hidden when empty and remains a single horizontally scrollable line when full.
- A missing target remains editable/removable, shows a warning icon, and produces a non-blocking notification on navigation.

Do not silently change these interaction rules while addressing an unrelated task.

## Current technical baseline

- Plugin version: `0.3.7` in `build.gradle.kts`.
- Kotlin: `2.2.20`.
- Gradle wrapper: `9.1.0`.
- IntelliJ Platform Gradle Plugin: `2.19.0`.
- Java toolchain: JDK 21.
- Development SDK: WebStorm `2025.3.1.1`.
- Declared IDE range: `sinceBuild = 253`, `untilBuild = 253.*`.
- Plugin ID: `dev.projectpins`.
- Main package: `dev.projectpins`.
- State file: `.idea/projectPins.xml` per project.

Do not widen compatibility based only on successful compilation. Run Plugin Verifier and manual UI checks on the target IDE line.

## Source map

- `src/main/resources/META-INF/plugin.xml`: plugin descriptor, notification group, startup activity, and Project View context action.
- `ProjectPin.kt`: mutable XML state beans. List position is pin order.
- `ProjectPinsService.kt`: persistence, defensive snapshots, duplicate detection, path conversion, pointer lifecycle, ordering, and change events.
- `PinToProjectPinsAction.kt`: context action for creating pins.
- `PinEditorDialog.kt`: native create/edit dialog. The checkbox exists only for files.
- `ProjectPinsToolbarActionGroup.kt`: despite the historical filename, this now contains the Project View installer, pins row, buttons, popup, hover painting, and badge icon.
- `ProjectPinsNavigator.kt`: Project tree reveal, optional editor opening, and double-click scroll-to-top.
- `ProjectPinsServiceTest.kt`: platform fixture coverage for the service.
- `docs/ACCEPTANCE.md`: UI regression checklist.
- `docs/IMPLEMENTATION.md`: design details and platform risks.

There must be no registered or source-level `ProjectPinsToolWindowFactory`. That was the old separate-bar implementation.

## Architecture invariants

### Preserve the Project tree

`ProjectPinsProjectViewInstaller` wraps the original Project View component. The platform shares that component among multiple `Content` entries. Create exactly one wrapper per project, store it under `WRAPPER_KEY`, and assign that same wrapper to all current and newly added contents.

Never create one wrapper per content and never keep moving the original component among wrappers. That failure mode produces an empty gray Project View. After touching installer code, test every Project View tab, reopening the tool window, and reopening the project.

The insertion is Swing integration because there is no public extension point for the requested row location. Keep the code small and isolated. Prefer documented IntelliJ APIs everywhere else. Do not restore a separate tool window unless the product requirement explicitly changes.

### Keep state ownership in the service

Callers must not mutate stored beans directly. `pins()` and `state` return copies. All writes go through service methods and publish `ProjectPinsService.TOPIC` so the row refreshes.

Each pin owns a disposable `VirtualFilePointer`. Dispose and recreate it when the target path changes; dispose it on pin removal. Project-relative targets are stored as relative paths, `.` means project root, and external targets remain full URLs.

Duplicate checks compare current pointer URLs and expanded stored paths. Recheck validity after a modal dialog before adding a target.

### Respect UI threading and lifecycle

Action `update()` work runs on BGT where declared. Swing changes and message-bus UI refreshes belong on EDT. Bind listeners and pointers to project, tool-window, row, or pin disposables. Check `project.isDisposed` and `VirtualFile.isValid` after asynchronous boundaries.

Do not traverse or synthesize clicks in the Project tree. Use `ProjectView.changeView` and `ProjectView.selectCB`. Keep reveal-only navigation from forcing Project tree focus because IDE autoscroll settings may open the selected file.

### Keep the icon adaptive

`PinIcon` takes at most four trimmed uppercase characters. It measures the rendered string and reduces the font only until it fits. Test narrow and wide glyphs rather than choosing font size from character count alone. Keep enough width for the badge without adding large side gaps.

Use IntelliJ theme colors such as `JBColor` and `JBUI.CurrentTheme`; do not hard-code a UI that works only in Darcula. Account for `JBUI.scale` in dimensions and painting.

## Safe change workflow

1. Inspect `git status` and the relevant diff first. The user may have valuable uncommitted work; never reset, checkout, or overwrite it.
2. Read the affected source and search all references with `rg` before editing.
3. Make the smallest coherent change. Remove dead paths when they directly contradict the active architecture.
4. Update `README.md`, `CHANGELOG.md`, this file, and the manual checklist when behavior or build instructions change.
5. Compile before packaging. Run focused tests for service/state changes and manual sandbox checks for Project View UI changes.
6. Report exactly what was verified. Do not claim a ZIP, test pass, verifier result, commit, or push unless it happened in the current working tree.

Never commit IDE-local files, credentials, Marketplace tokens, build output, or a machine-specific SDK path. Do not publish or push unless the user asks.

## Build and verification commands

On a machine with WebStorm installed:

```sh
export JAVA_HOME=/path/to/jdk-21
./gradlew --no-daemon compileKotlin -PlocalIdePath=/Applications/WebStorm.app
./gradlew --no-daemon test -PlocalIdePath=/Applications/WebStorm.app
./gradlew --no-daemon buildPlugin -PlocalIdePath=/Applications/WebStorm.app
./gradlew --no-daemon verifyPlugin -PlocalIdePath=/Applications/WebStorm.app
./gradlew --no-daemon runIde -PlocalIdePath=/Applications/WebStorm.app
```

Append `--offline` only when all dependencies and the selected SDK are already cached. The ZIP is generated as `build/distributions/ProjectPins-<version>.zip`. Install it through **Settings → Plugins → gear menu → Install Plugin from Disk**.

Minimum validation by change type:

- Documentation only: inspect rendered Markdown and verify all versions, paths, class names, and commands against the tree.
- Service or persistence: `compileKotlin` plus platform tests; add a meaningful fixture test for a new invariant.
- Button, popup, icon, installer, or navigation UI: `compileKotlin`, `buildPlugin`, then the relevant sections of `docs/ACCEPTANCE.md` in `runIde` or the target IDE.
- Compatibility/version change: tests, `buildPlugin`, Plugin Verifier, sandbox UI checks, changelog, descriptor notes, and installable ZIP inspection.

## Known limitations

- Project View placement depends on platform Swing ownership and is the most likely source of IDE-version regressions.
- Pointer tracking cannot recover every move made while the IDE is closed or every delete/create replacement.
- External URLs are machine-specific.
- There is no drag reorder UI, multi-selection creation, localization bundle, remote-development validation, or automatic missing-target search.
- The current visible popup does not expose ordering even though `ProjectPinsService.move` still exists and service tests cover it.

## Release hygiene

Before a public release, replace the vendor placeholder, add a license and plugin icon, add current screenshots and support/privacy links, confirm plugin-ID ownership, and configure signing if Marketplace requires it. Keep `JETBRAINS_MARKETPLACE_TOKEN` and signing material in environment variables or CI secrets.

Update the version in `build.gradle.kts`, add release notes to `CHANGELOG.md`, and keep `pluginConfiguration.changeNotes` aligned with the packaged release. The current `main` branch may contain uncommitted post-release work, so derive claims from `git status`, the diff, and actual build output rather than the last commit alone.
