# AGENTS.md

Guidance for coding agents working in this repository.

## Project intent

Build a reusable Android Java library that translates Markdown with Google ML Kit while preserving Markdown structure.

Primary roadmap source: `ML-markdown-lib.TODO.md`

## Repository layout

- `library/` — core Android library module (main deliverable)
- `sample/` — Android app used for manual verification
- `docs/api.md` — public API draft
- `docs/translation-strategy.md` — AST/token translation strategy

## Build, test, format

Use Gradle wrapper from repo root.

- Build sample app:
  - `./gradlew :sample:assembleDebug`
- Run library unit tests:
  - `./gradlew :library:testDebugUnitTest`
- Run formatting checks:
  - `./gradlew spotlessCheck`
- Auto-format Java + XML:
  - `./gradlew spotlessApply`

## Formatting rules

- Java formatting is enforced by Spotless + `googleJavaFormat` (AOSP style).
- XML formatting is enforced by Spotless (`eclipseWtp('xml')`).
- Run `spotlessApply` before finalizing changes.

## Implementation expectations

1. Keep library API small and stable.
2. Prefer AST/token-stream Markdown processing over regex-only logic.
3. Use regex fallback only for robustness/edge cases.
4. Preserve Markdown structure (headings, lists, quotes, code, links, spacing).
5. Keep Android/ML Kit integration separated from pure Java markdown logic where possible.

## Language/SDK constraints

- Java 17
- Android minSdk 24
- compileSdk 34

## Safety notes for changes

- Avoid introducing app-specific UI/business logic into `library/`.
- Keep sample UI behavior aligned with current UX requirements (toggle raw/rendered mode, model download/delete flow, etc.).
- Prefer additive/refactoring-safe changes with tests when modifying translation pipeline behavior.

## Validation checklist before handoff

1. `./gradlew spotlessCheck`
2. `./gradlew :library:testDebugUnitTest`
3. `./gradlew :sample:assembleDebug`

If device testing is requested:

- `adb install -r sample/build/outputs/apk/debug/sample-debug.apk`
