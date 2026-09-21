# Manual acceptance checklist

Run in an isolated `runIde` sandbox. Create a sample project with `src/app/features/auth/auth.service.ts`, another editor file, and a directory pin. Repeat navigation with Project View “Open Files with Single Click” and “Always Select Opened File” both off and on.

- A: Pin the TS file unchecked. Open another editor, collapse the tree, then click the pin. Ancestors expand and target selects. The open/selected editor tabs are unchanged. Focus may remain on the pin.
- B: Enable Open in editor. Click the pin after navigating away. The tree selects the file and its editor opens.
- C: Pin a folder. Dialog and context menu omit the editor toggle. Click the pin; it selects the directory without opening editors.
- D: Rename/reorder several pins and change preferences. Close/reopen the sandbox project, then restart the sandbox. Names, order, paths and preferences survive.
- E: Delete a pinned target. Its chip shows a warning; click gives a notification. Edit its path to an existing file; navigation recovers. Remove another broken pin.
- Rename a file and move its parent within the IDE, then restart. The pin still resolves; the custom label stays unchanged.
- Exercise Move Left/Right boundary states, Shift+F10 menu access and operation during indexing.
- Close projects with pins and inspect sandbox idea.log for disposal/threading errors.

Record completed checks and any gaps in VERIFICATION.md; this checklist alone is not proof of execution.
