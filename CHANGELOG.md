# Changelog

## 0.3.7

- Enable “Open immediately” by default for new file pins.
- Keep the option hidden for directory pins.

## 0.3.6

- Double-clicking a file pin opens the file even when “Open immediately” is disabled, then scrolls it to the first line.

## 0.3.5

- Double-clicking a pin scrolls its already-open file to the first line.

## 0.3.4

- Size pin badges from their measured text width so 3–4 character labels remain readable.
- Allow the pin button to widen slightly for longer labels.

## 0.3.3

- Replace the Swing context menu with the native IntelliJ action popup.

## 0.3.2

- Paint the native IntelliJ action-button background while hovering over a pin.

## 0.3.1

- Preserve the shared Project View tree when adding the pins row.
- Use IntelliJ's native hover treatment for pin buttons.

## 0.3.0

- Show pins in a compact dedicated row above the Project tree.
- Restore normal spacing in the Project View title toolbar.
- Keep right-click editing, open-on-click toggling, reordering and removal in the pin row.

## 0.2.0

- Move Project Pins into the native Project View toolbar as icon-only buttons with 1–4 character badges.
- Remove the separate Project Pins tool window registration.

## 0.1.0

- Pin files and folders from the Project tree with custom names.
- Reveal targets with optional per-file editor opening.
- Save project-relative pins, order and settings across restarts.
- Track IDE-observed target moves and renames using VFS pointers.
- Rename, repair, reorder and remove pins from a compact native panel.
