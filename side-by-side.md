# Side-by-Side Compare Mode — Implementation Plan

## Goal
Add a compare icon to the right of the **Render markdown** toggle that opens a dedicated landscape activity showing source and translated markdown side-by-side, with synchronized vertical scrolling and consistent light/dark styling.

## Requirements
1. Add icon on the right of `Render markdown` toggle.
2. On click, open new activity in **landscape** orientation.
3. Show source + translated markdown side-by-side.
4. If vertically scrollable, both panes must scroll in sync.
5. Keep current textbox font/background colors in light/dark mode.

---

## Step 1 — Main screen entry point (icon + launch)

### 1.1 Update main layout
- File: `sample/src/main/res/layout/activity_main.xml`
- Replace single `SwitchMaterial` row with horizontal container:
  - `SwitchMaterial` (`@id/renderModeToggle`) takes weight `1`.
  - Add compare icon button at right (`@id/compareModeButton`).

### 1.2 Add resources
- File: `sample/src/main/res/values/strings.xml`
  - `compare_mode_icon_content_description`
  - `compare_screen_title`
- File: `sample/src/main/res/drawable/ic_compare_side_by_side.xml`
  - vector icon for compare mode.

### 1.3 Wire click behavior
- File: `sample/src/main/java/io/github/godsarmy/mlmarkdown/sample/MainActivity.java`
  - Add field: `ImageButton compareModeButton`.
  - Bind in `bindViews()`.
  - Add click listener in `setupActions()`.
  - Add method `openSideBySideCompare()` to launch activity with extras:
    - source markdown from `originalMarkdownInput`
    - translated markdown from `translatedMarkdownRaw`

---

## Step 2 — New landscape compare screen

### 2.1 Register activity in manifest
- File: `sample/src/main/AndroidManifest.xml`
- Add activity entry:
  - `android:name=".SideBySideCompareActivity"`
  - `android:screenOrientation="landscape"`
  - `android:label="@string/compare_screen_title"`

### 2.2 Create activity class
- File: `sample/src/main/java/io/github/godsarmy/mlmarkdown/sample/SideBySideCompareActivity.java`
- Responsibilities:
  - static `createIntent(Context, String, String)` helper.
  - read source/translated extras in `onCreate`.
  - bind views and display both markdown strings.

### 2.3 Create side-by-side layout
- File: `sample/src/main/res/layout/activity_side_by_side_compare.xml`
- Horizontal parent with 2 equal panes (`layout_weight=1` each).
- Left pane:
  - label: `@string/source_markdown_label`
  - readonly text area `@id/compareSourceText`
- Right pane:
  - label: `@string/label_output_markdown`
  - readonly text area `@id/compareTranslatedText`

---

## Step 3 — Synchronized vertical scrolling

### 3.1 Add two-way scroll sync logic
- File: `SideBySideCompareActivity.java`
- Add:
  - `boolean syncingScroll` guard to prevent recursive listener loops.
  - `setOnScrollChangeListener` on both text views.
  - helper to clamp and mirror scroll position to opposite view.
  - helper to compute max scroll range based on layout/content height.

### 3.2 Expected behavior
- Scrolling left updates right by same vertical offset.
- Scrolling right updates left by same vertical offset.
- Offset is clamped to each pane’s scrollable range.

---

## Step 4 — Preserve visual consistency (light/dark)

Use existing resources from current editor UI:

- Pane background:
  - `@drawable/preview_box_background`
  - already has day/night variants.
- Text color:
  - `@color/mlkit_on_background`
- Hint/secondary text:
  - `@color/mlkit_on_surface_variant`
- Root background:
  - `@color/mlkit_bg_canvas`

This keeps compare mode consistent with current text boxes in both light and dark mode.

---

## Step 5 — Verification checklist

### Functional checks
- Compare icon appears to the right of `Render markdown` toggle.
- Tapping icon opens compare activity in landscape orientation.
- Source and translated markdown are shown side-by-side.
- Vertical scrolling in one pane synchronizes the other pane.
- Colors/background match existing main screen in light/dark mode.

### Build/quality checks
Run from repo root:

1. `./gradlew spotlessApply`
2. `./gradlew spotlessCheck`
3. `./gradlew :sample:lintDebug`
4. `./gradlew :sample:assembleDebug`

Optional full validation:

5. `./gradlew :library:testDebugUnitTest`

---

## Current implementation status (tracked)

- ✅ Step 1 completed
- ✅ Step 2 completed
- ✅ Step 3 completed
- ✅ Step 4 completed (resource reuse)
- ⏳ Lint cleanup in progress for pre-existing AppCompat tint issues in source selector row XMLs
