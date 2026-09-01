# Omok App — Development Progress Log

This file tracks progress on the omok Android app in beginner-friendly language, updated at each meaningful development stage. For the original v1 build story (Phase 0–6), see `llm-wiki/raw/android_app_dev/14_omok_project_journal.md` — that's a one-time retrospective; this file is the ongoing log going forward.

---

## 2026-08-25 — Release signing setup (getting ready for Google Play)

**Goal:** Before an app can go on the Google Play Store, Google requires every release build to be cryptographically "signed" with a private key, proving updates really come from the same developer. This step set that up for the first time.

### What "signing" actually means (the short version)
Think of it like a wax seal on a letter. You create a private key (the seal) once, and every release build gets stamped with it. Google Play checks the stamp on every update — if it doesn't match the original, the update is rejected. That's a security feature: it stops anyone else from pushing a fake "update" to your app.

### What was done, step by step

1. **Found a JDK with the `keytool` utility.**
   `keytool` is the program that generates signing keys — it ships with any Java Development Kit (JDK), not just Android Studio's own copy. We located one that Gradle had already downloaded at
   `C:\Users\gbear\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2\bin\keytool.exe`.

2. **Generated an "upload keystore".**
   A *keystore* is a small encrypted file that holds one or more private keys. We generated a new RSA 2048-bit key, valid for ~27 years (10,000 days — the standard recommendation, since re-issuing later is disruptive), stored at:
   `omok/keystore/omok-upload.jks` (alias: `omok-upload`)

   Two random, strong passwords were generated for it — one for the keystore file itself (`storePassword`), one for the individual key inside it (`keyPassword`). Note: modern keystores (`PKCS12` format, the current default) don't actually support two *different* passwords — Android just reuses the store password for both, and `keytool` warned us about exactly that.

3. **Stored the passwords in `omok/keystore.properties`** (not the build file itself), so the actual secrets aren't hard-coded into `build.gradle.kts`.

4. **Told Gradle to use this keystore for release builds**, in `omok/app/build.gradle.kts`:
   - Added a small snippet that reads `keystore.properties` into a `Properties` object.
   - Added a `signingConfigs { create("release") { ... } }` block that fills in `storeFile` / `storePassword` / `keyAlias` / `keyPassword` from that file.
   - Told the existing `release` build type to actually use it: `signingConfig = signingConfigs.getByName("release")`.

5. **Protected the secrets from being committed to Git.**
   Added `keystore.properties` and `/keystore` to `omok/.gitignore`. If these were committed to a public (or even private-but-shared) repo, anyone with access could impersonate this app's updates.

6. **Verified it actually works**, two ways:
   - `./gradlew signingReport` — confirmed the `release` variant now resolves to `omok-upload.jks` (previously it would have shown no signing config, or fallen back to the debug key, which Google Play refuses to accept for publishing).
   - `./gradlew bundleRelease` — built a real release `.aab` (Android App Bundle) and confirmed the `signReleaseBundle` task ran successfully, producing `app/build/outputs/bundle/release/app-release.aab`.

### ⚠️ Important thing to remember
**Back up `omok/keystore/omok-upload.jks` and `omok/keystore.properties` somewhere outside this machine** (e.g. a password manager attachment, encrypted cloud backup). If this keystore is ever lost, there is no way to publish an update to the same Play Store listing again — Google cannot reset or recover it. This isn't optional busywork; it's the single point of failure for all future updates.

### Still open (not done yet)
- Everything else from the original Play Store publishing checklist (store listing, privacy policy, Data Safety form, closed testing with 12 testers for 14 days, etc.) — signing was just the first, code-level piece.

---

## 2026-08-26 — Turned on app optimization (R8 code + resource shrinking)

**Goal:** Close out the item left open above — enable code/resource shrinking for release builds, and confirm nothing breaks.

### What R8 optimization actually does (short version)
Think of it as a strict editor for your compiled app:
- **Code shrinking** — deletes any class/method/field that nothing in the app actually calls (dead code), and renames the rest to short names like `a`, `b`, `c`. Smaller app, and harder for someone to read your logic if they decompile it.
- **Resource shrinking** — deletes images/strings/layouts that R8 can prove are never referenced.

**The risk to know about:** R8 only sees what your code *statically* calls. If something is reached indirectly (reflection, a class looked up by a string name, etc.), R8 can't tell it's actually used and may delete it — which builds fine but crashes at runtime. This is why "turn it on and hope" is bad practice; you build a real release artifact and test it.

**Why this app was low-risk:** checked `gradle/libs.versions.toml` first — omok only depends on Compose, ViewModel, core-ktx, and DataStore *Preferences*. No reflection-based libraries (no Gson/Moshi/kotlinx-serialization, no `@Parcelize`). So there was nothing obviously fragile for R8 to trip over.

### What was done, step by step

1. **Located AGP 9.3+'s optimization DSL.** This project uses a very recent Android Gradle Plugin (9.3.2) with a newer, simplified syntax: one `optimization { enable = true }` block turns on *both* code and resource shrinking together (older AGP versions needed separate `isMinifyEnabled` / `isShrinkResources` flags). Confirmed this via Android's official docs before touching anything, since guessing wrong on build-file syntax wastes a lot of time.
   - Bonus: from AGP 9.3+, this also auto-includes Android's default recommended keep rules — no need to manually reference `proguard-android-optimize.txt` like older projects do.
   - Any custom keep rules (exceptions telling R8 "don't touch this") would go in `omok/app/src/main/keepRules/*.keep` — none were needed here.

2. **Flipped the flag** in `omok/app/build.gradle.kts`: `enable = false` → `enable = true`, with a comment explaining what it does for future-me.

3. **Verified, in order of increasing strictness:**
   - `./gradlew testDebugUnitTest` — the existing JVM unit tests (`WinCheckerTest`, `EvaluatorTest`, `AiTest`, `SelfPlayTest`, `GameRulesTest`, `LevelConfigTest`) all still pass. (Note: these test the `game`/`ai` logic directly on the JVM, not through R8, so they don't by themselves prove R8 didn't break anything — but they confirm the underlying logic is still correct.)
   - `./gradlew bundleRelease` — this is where R8 actually runs (`minifyReleaseWithR8` task). Build succeeded, and Android's mandatory `lintVitalRelease` check (a strict lint pass that release builds cannot skip) also passed.
   - `./gradlew assembleRelease` — built an installable `.apk` version too, so a real device can be tested (see below).

4. **Concrete proof it worked:** the release `.aab` shrank from **~8.97 MB → ~3.46 MB** (about 61% smaller) — clear evidence the shrinking actually did something, not just that the flag was accepted.

### What's NOT verified yet
No emulator or physical device was available in this environment (`adb devices` returned empty, no AVDs configured), so **the optimized build has not been run on an actual device yet.** The signed `app-release.apk` was sent to you directly — please install it on your phone and play at least one full game (place stones, trigger a win, check level up/down, sound on/off, close and reopen the app to confirm your saved level persists). If R8 broke something, this is where it would show up as a crash, not in the build log.

### Still open (not done yet)
- Manual on-device confirmation of the optimized build (above).
- Store listing, privacy policy, Data Safety form, closed testing (12 testers / 14 days), content rating — the non-code parts of the original Play Store checklist.

---

## 2026-08-26 — Added Undo ("무르기")

**Goal:** Add the take-back-a-move feature that v1 intentionally skipped (see the original journal: "이동 기록은 이미 있어서 ~20줄이면 추가 가능" — "move history already exists, so ~20 lines should do it").

### Why it really was that simple
`Board` (`game/Board.kt`) is **immutable** — `place()` never changes the board you call it on, it always returns a brand-new `Board` with one more stone on it. That means every board state that ever existed during a game is still sitting untouched in memory somewhere. So "undo" doesn't need any special "erase a stone" logic — it just needs to **remember old `Board` objects and hand one back later.** That's exactly what a *stack* (a last-in-first-out list — think of a stack of plates, you only ever add/remove from the top) is for.

### Design decision: undo rewinds one full turn, not one stone
This is player-vs-AI, not player-vs-player. If Undo only removed *your* stone, the AI would suddenly be "reacting" to a board it never actually played against — confusing and glitchy. So Undo always rewinds to the board exactly as it was **right before your last move**, discarding both your move and the AI's reply to it. Tap it twice to go back two full turns. Because it's a stack, this multi-step rewind comes for free — no extra code needed beyond "keep popping."

### What was done, step by step

1. **`GameViewModel.kt`** — added `private val undoHistory = ArrayDeque<Board>()`.
   - In `onPlayerTap()`, right before your move is applied, the *current* board gets pushed onto this stack (`undoHistory.addLast(state.board)`) — that's the snapshot to come back to later.
   - Added `fun undo()`: pops the most recent snapshot off the stack and makes it the current board again, resets whose turn it is back to the player, and clears any "game over" result (so you can keep playing).
   - Guard: `undo()` does nothing while `aiThinking == true`. Reason: the AI's move is being calculated on a background thread (`Dispatchers.Default`, see `requestAiMove()`); if you rewound *while* that calculation was still running, it would land a moment later and silently resurrect a stone you just undid.
   - `resetGame()` and `chooseLevelUp()` (both start a fresh board) now also clear `undoHistory` — otherwise you could theoretically undo *into* the previous finished game.
   - Added `canUndo: Boolean` to `GameUiState` so the UI knows when the button should be tappable.

2. **`GameScreen.kt`** — added a full-width "↩️ 무르기" `OutlinedButton` above the existing button row, wired to `viewModel.undo()`, `enabled = uiState.canUndo && !uiState.aiThinking`.

### Verification
- `./gradlew testDebugUnitTest` — existing tests still pass (this feature doesn't touch `game`/`ai` logic, so no surprise there).
- `./gradlew assembleDebug` — compiles cleanly.
- `./gradlew assembleRelease` — R8-optimized build (from the previous entry) still succeeds with the new code, including `lintVitalRelease`.
- **Not verified by me:** actual on-device play. `GameViewModel` extends `AndroidViewModel`, which needs a real (or emulated) Android runtime to instantiate — this project has no Robolectric/instrumented-test setup, so this class isn't unit-testable on plain JVM the way `game`/`ai` are. The signed release APK was sent for a manual test: play a couple of moves, tap 무르기, confirm the board rewinds and it's your turn again; confirm the button is greyed out on move 1 and stays disabled while "생각 중…" is showing.

### Still open
- Manual confirmation of Undo on a real device (above).
- Everything from the Play Store checklist still open from the previous entries.
