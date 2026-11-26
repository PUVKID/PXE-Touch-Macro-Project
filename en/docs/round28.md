# Revision 28 · 2.8.5 Recognition Recovery

**English** · [한국어](../../28차/상세설명.md) · [English overview](../README.md)

Version 2.8.5 repairs a recognition regression: comparing only the complete OCR line could miss the independent word `Adobe` in `Adobe 15`. Candidates for independent words and multi-element phrases are restored. The 2.8.4 interface, scrolling and macro controls remain in place. **The existing default final revision 26 is preserved**; the latest revision 28 is not a claim of completed long-duration or game-speed validation.

| Reading level | Read |
|---|---|
| LEVEL 1 - Beginner | [Text, preview and controls](#user-content-reading-level-1) |
| LEVEL 2 - Explorer | [Recognition and execution flow](#user-content-reading-level-2) |
| LEVEL 3 - Practitioner | [Installation, settings and building](#user-content-reading-level-3) |
| LEVEL 4 - Advanced | [Matching, storage and cancellation](#user-content-reading-level-4) |
| LEVEL 5 - Expert | [Regression evidence and limits](#user-content-reading-level-5) |

<a id="reading-level-1"></a>

## LEVEL 1 - Beginner

### Find the intended word before running a macro

A macro is a saved sequence of touches, waits, gestures and back actions. OCR reads text from the screen image. PXE includes the Korean OCR model locally; screen images do not need to be sent away for OCR. Set a word, define where to look, choose a macro and check the preview before starting.

| Screen text and target | Expected behavior | Reason |
|---|---|---|
| Target `Adobe`, screen `Adobe 15` | Find the independent word | Other words may be next to it |
| Target `Adobe`, screen `Adobe Pro` | Find the independent word | A separated word is valid |
| Target `Adobe`, screen `AdobePro` or `Adobe15` | Reject the substring | The target is attached to another word/number |
| Target `Adobe`, screen `어도비` or `ADOBE` | Reject the different spelling | No translation or case folding |

Add a target to the horizontal shortcut list to select it from the app or lower arrow panel. The lower counter shows current/total repetitions and can be dragged to move the widget. **Pause, resume and stop are in the app or upper control window**, not the lower progress widget.

Cancelling a text edit leaves the previous task and settings in place. Applying a valid target replaces the applicable running text-detection task. A new target is not a rewrite of the saved general macro action list. OCR can still misread letters or miss text on complex screens.

[한국어 · same level](../../28차/상세설명.md#user-content-reading-level-1) · [Project guide · Korean](../../docs/읽기안내/레벨1.md) · [Complete shared behavior](#user-content-shared-contract)

<a id="reading-level-2"></a>

## LEVEL 2 - Explorer

### Follow recognition, regions and execution

Screen-information recognition compares the complete accessibility node `text`. It does not use `contentDescription`. OCR instead constructs exact word/phrase candidates from the recognized original line. **AUTO can fall back from screen information to OCR**, so these two recognition units remain distinct.

| Stage | Current behavior | Boundary |
|---|---|---|
| Screen information | Compare the complete node text | A word inside a longer node is not automatically the whole-node match |
| OCR candidates | Align elements with the original line and check word boundaries | Do not invent omitted/replaced characters |
| Region filtering | Check a completed candidate's coordinates | Cropping `Pro` outside a region must not turn `AdobePro` into `Adobe` |
| List detection | Search up to six priority regions, enter, run the configured macro repeats, return | Waiting after completion and refresh timing are separate settings |
| Change target | Save the shared shortcut/target settings and replace the applicable execution generation | Do not accumulate a new screen query while the previous query is still running |

The app and expanded lower panel share one shortcut store. Registration edits the list; selecting a shortcut saves both the first detection target and standalone text target. Duplicate prevention preserves its saved lock and reappearance conditions. An OCR error is not treated as evidence that text is truly absent.

The automatic downward movement takes **0.9 seconds across 22.5% of screen height**. Pull-to-refresh remains **45% over 0.9 seconds**. Recorded gestures use their saved paths and durations; these automatic-motion constants do not rewrite recorded gestures.

[한국어 · same level](../../28차/상세설명.md#user-content-reading-level-2) · [Project guide · Korean](../../docs/읽기안내/레벨2.md) · [Full features and changes](#user-content-shared-contract)

<a id="reading-level-3"></a>

## LEVEL 3 - Practitioner

### Install, configure and check the actual screen

The universal local APK supports **Android 11+**, ARM64 and x86_64, with the existing package ID and signing certificate. Install as an update rather than uninstalling. Export important macros first: this revision did not compare every user's saved data across every update from revision 16 to 28.

| Step | Set or inspect | Expected outcome |
|---|---|---|
| Permissions | Accessibility, overlay and the screen-capture permission flow used for OCR | Required services can read/act on the screen |
| Detection | Target text, up to six regions, macro and repeat count | Preview shows the intended original word/phrase |
| Timing | Repetitions, completion wait, return and refresh intervals | Per-action repeats and full-macro repeats remain distinct |
| Change target | Lower arrow → edit text, or a registered shortcut | Valid text updates the applicable target task; cancel preserves it |
| Resume/stop | App or upper controls | Returning to the app may pause; resume continues the task |

Invalid text stays in the editing dialog without first stopping a valid running task. Recording and structural edits to the action list retain their existing stop conditions. Widget opacity is configurable. Expand the lower arrow to inspect overall progress; it does not contain pause/stop buttons.

### Build from public source

Use the actual Python executable, JDK 17, Android SDK, Gradle and separately held signing material. The existing scripts accept `JAVA_HOME`, `ANDROID_HOME`, `PXE_GRADLE` and `NEX_SIGNING_DIR`. The Rust build script also has documented Windows tool/SDK paths that must be reviewed on another PC; the environment is not automatically installed or portable.

From the revision's `source` folder:

```powershell
& './theme-engine/빌드.ps1' -Mode test
& './theme-engine/빌드.ps1' -Mode release
Copy-Item -LiteralPath './theme-engine/target/release/astryx-theme.exe' -Destination './theme-engine/astryx-theme.exe'
& './빌드.ps1' -Python $PythonExecutable
```

Set `$PythonExecutable` to the actual existing Python executable. Stop if a prerequisite command fails. `tools/build_release.py` invokes the Rust palette generator, checks the two palettes, synchronizes the actual build copy, and runs release assembly, unit tests and lint. `-PNEX_PC` includes ARM64 and x86_64. Rust generates Android color XML on the PC; APK users do not install Rust. Private signing keys and execution environments are not in the public source.

| Local delivery | Bytes | How to identify it |
|---|---|---|
| Universal APK | 32,732,799 | Version 2.8.5, versionCode 28, existing v2 signature |
| Installation ZIP | 14,951,973 | Embedded APK bytes match the standalone APK |
| Full source ZIP | 2,906,316 | 153 file entries checked against the local original source |

Exact SHA-256 values and signer fingerprint are in [package metadata](../../28차/파일정보.json). APK/ZIP files remain local deliveries; no new GitHub release assets were created. These sizes describe packages, not runtime speed or accuracy.

[한국어 · same level](../../28차/상세설명.md#user-content-reading-level-3) · [Project guide · Korean](../../docs/읽기안내/레벨3.md) · [Build instructions · Korean](../../28차/source/빌드방법.txt)

<a id="reading-level-4"></a>

## LEVEL 4 - Advanced

### Match original text, then apply geometry

`TextRules.elementStarts` maps OCR elements monotonically into the original line and allows only whitespace in the gaps. Missing, replaced or unexplained non-whitespace characters make the mapping fail. `elementSpans` accepts a target only when its original substring ends at an element boundary and `completeElement` accepts its word boundaries. Phrase whitespace and case remain exactly as supplied.

| Component | Responsibility | Failure/state boundary |
|---|---|---|
| `TextRules` | Original element locations, exact spans and Unicode-aware word boundaries | No partial word, translation, case folding or whitespace normalization at execution |
| OCR candidate selection | Union the participating element boxes before region filtering | If original mapping is uncertain, fall back to complete line/block comparison |
| `MacroService.replaceTargetWord` | Validate new configuration and standalone step, resolve the necessary macro, then save `word/watch_config` in one edit | Invalid input preserves the current settings and execution generation |
| `MacroService.activeTargetWord` | Expose the target in the running execution snapshot | Display follows the actual running target even when saved modes differ |
| `RunHud` / `TargetWords` | Shared JSON shortcut order, duplicate prevention and deletion | Rebuild shortcuts only when stored text or the active target changes |
| `QueryLane` / `readScreen` | Wait for the outstanding platform query to finish | Future cancellation alone does not terminate the platform call; retain cancellation checks, real timeout and stale-result rejection |

Unicode letters/digits, combining marks, connector punctuation and format characters participate in the boundary check. This prevents selecting a piece of an attached word, underscore-connected label or combining sequence. Legacy contains/regex/ignore-space/ignore-case settings remain readable where required, but the execution path prioritizes original literal equality via `prepareExact`.

The native UI separates full-width action descriptions from the title/more button, reduces redundant row boundaries and empty helper text, and retains popup width, keyboard resizing and Korean wrapping refinements. It is not a React runtime embedded in Android. Existing native screens and actual Rust-generated palette XML remain in use.

Cancelling the old query does not justify starting unbounded new threads. The code waits in short cancellable intervals until the old work is ready, while preserving the true unresponsive boundary and discarding results from older execution generations.

[한국어 · same level](../../28차/상세설명.md#user-content-reading-level-4) · [Project guide · Korean](../../docs/읽기안내/레벨4.md) · [Revision 28 source](../../28차/source/)

<a id="reading-level-5"></a>

## LEVEL 5 - Expert

### Separate regression recovery from performance claims

The comparison used the same synthetic `Adobe 15` image: the 2.8.4 matching path missed the target while the supplied 2.8.1 path found it. Twenty-seven OCR model/native files were byte-identical between those two packages, which helped isolate candidate selection from a changed recognition model. Version 2.8.5 restores exact independent candidates without changing the preserved UI, scrolling or macro controls.

| Evidence | Observed result | What it does not establish |
|---|---|---|
| Release build / unit / lint | Build succeeded; 112 tests, zero failures/errors/skips; lint 0 | Every device or future execution is error-free |
| Actual MLKit candidate methods | Four passed: independent word, multi-element phrase, attached-word/region boundaries and alternate-label/spelling exclusions | Four end-to-end touches on new phrases |
| Recognition and touch on the app's test screen | Two passed | User-game behavior or all real-world screen layouts |
| Package verification | 2.8.5/code 28, existing v2 signing certificate, local ZIP/APK fingerprints | Speed, long-running reliability or recognition accuracy percentage |
| Public comparison | Full original diff and readable split comparison retained | A new research paper or model-performance result |

The six unique device methods ran on the dedicated Android 12/API31 x86_64 AVD using isolated ADB port 5047. The user's BlueStacks, games and default ADB port 5037 were not operated; the dedicated AVD was closed afterward. Candidate tests ran actual MLKit on synthetic images and checked product candidates/coordinates. Only the other two methods checked recognition through actual touch execution on the app's own screen.

| Preserved observation or failure | Treatment |
|---|---|
| First A/B image separated the two labels onto different lines | Preserve failed reproduction assertion; use a single-line `Adobe 15` control to reproduce the actual difference |
| Missing `Hit` import in the new test | Preserve compilation failure, add the import and confirm the final build |
| 2.8.4 A/B assertion failed | Preserve as the observed regression; record 2.8.1 and final 2.8.5 success separately |
| Earlier test rejected `Adobe Pro` | Correct the expectation: independent `Adobe` is valid; attached `AdobePro` is excluded. Historical `WholeLine` method names do not define the current policy |

`Macro` and `CapturePacer` retain their prior definitions. `Step` changed the literal-match display/legacy-regex validation boundary, and `WatchConfig` changed the automatic downward endpoint; therefore a claim that all four files are identical to revisions 16/26 is not current. Capture intervals remain API30 **1,100 ms**, API31+ **500 ms**; existing capture/OCR concurrency limits, backoff and stale-response disposal remain.

`ScrollMotion.DURATION_MS=900`, `START_Y=75`, `END_Y=52.5` control automatic downward/list-retry motion. Pull-down remains 30→75; recorded SWIPE/GESTURE actions keep their own saved durations and paths. Earlier motion/device evidence and the preserved [2.8.4 design screenshots](../../28차/화면_확인/2.8.4/README.md) are historical evidence, not newly captured 2.8.5 UI validation or new timing measurements.

The reported local Graphify Java-AST analysis had 1,634 nodes and 5,544 edges; XML/binaries and some Gradle inputs were outside that extractor's scope. A graph count is not recognition accuracy or performance evidence. Windows Rust is the build-time palette generator, not a Rust replacement for Android OCR.

**Long-term uninterrupted operation, the user's 15-repeat game completion time, all-device success and 99% recognition accuracy remain unverified.** Earlier 75-operation/five-segment measurements belong to their recorded revisions and are not a new 2.8.5 benchmark.

[한국어 · same level](../../28차/상세설명.md#user-content-reading-level-5) · [Project guide · Korean](../../docs/읽기안내/레벨5.md) · [Verification record](../../28차/검증_결과.md) · [Full readable comparison](../../28차/전체변경_분할/변경_1.md)

<a id="shared-contract"></a>

## Shared Recognition, Features and Design

These are the full shared conditions. Level-specific explanations above do not replace them.

### Original-text recognition

OCR compares exact original independent words or phrases. `Adobe` in `Adobe 15` or `Adobe Pro` is valid; the substring in `AdobePro`/`Adobe15`, translated `어도비`, and differently cased `ADOBE` are not. Multi-element phrases preserve their original internal whitespace and case. If OCR elements cannot be aligned exactly with the original text, the program falls back to complete line/block comparison without inventing or joining characters.

Screen-information mode compares complete node text and ignores `contentDescription`. AUTO tries OCR when screen information does not find the target. These are different comparison units. Saved contains/regex/ignore-space/ignore-case options are not applied to the current execution match. OCR misrecognition and screen-dependent omissions remain possible.

| Feature | Current behavior | Setting or limit |
|---|---|---|
| General macros | Record, touch, swipe, curve/multi-touch gesture, wait, back, branch and repeat | Preserve saved order; distinguish action repeat from full-macro repeat |
| Recognition | Offline Korean OCR, screen information, AUTO fallback | No translation between `Adobe` and `어도비` |
| List detection | Search up to six priority areas, enter, run the registered macro (for example 15 repeats), return | Repetitions, priorities, return, completion wait and refresh timing are configurable |
| Duplicate prevention | Saved lock and reappearance rules | Reading failure is distinct from actual absence |
| Execution controls | Pause/resume/stop in app and upper controls | No pause/stop in lower progress widget |
| Target shortcuts | Add, suppress duplicates, delete and select from shared horizontal lists | Up to 30 entries shared across text/detection tabs and expanded lower widget |
| Storage and display | Macro import/export, theme, widget position/opacity | Update without uninstalling; export important original macros first |

| Preserved design | Value | Scope |
|---|---|---|
| Background/cards | Light `#F3F3EF` / `#FBFCF8`; dark `#181C19` / `#232924` | Low-contrast structure retained from 2.8.4 |
| Accent | Light `#505C52`; dark `#BCCDBB` | Selection, focus and primary actions; no blue accent |
| Typography | Title 24sp; primary input 20sp; choices 14sp; action title 15sp / description 13sp | System selection text also 14sp; NanumGothic 700 retained |
| Action rows | Full-width description; minimum 112dp; title/description up to two lines; more button 64dp | Ellipsis in the row; complete content in the editor |
| Control icons | 24dp vectors centered in matching 48dp buttons | Shared palette instead of font symbols |
| Logo | Rounded light `P` on charcoal | Launcher, header, collapsed widget and notification |
| Lower panel | `1/15`, arrow, movement, target edit and shortcuts | Pause/stop stay in app/upper controls |

| Revision-28 behavior retained by 2.8.5 | Implementation | Separate boundary |
|---|---|---|
| Automatic movement | Downward/list-retry 0.9s over 22.5% height | Pull-down 45% over 0.9s; recorded gestures unchanged |
| Internal layout | Simple rows/dividers, full-width action description, input/popup spacing | Light/dark saved choice and existing native rendering |
| Lower target edit | Arrow beside `1/15` → edit → apply | Drag counter/position saving and touches outside the panel retained |
| Shortcut selection | Save first detection and standalone target together | Running applicable modes restart; general macro list unchanged |
| Query race handling | Cancellable wait for previous query's actual completion | No query accumulation; real unresponsive limit and stale-result rejection retained |
| Progress widget | Collapsed 104×44dp; expanded width 280dp; progress/time/target/shortcuts | Upper/app pause-stop retained; lower pause-stop removed |

[Korean full contract](../../28차/상세설명.md#user-content-pxe28-current-contract) · [Revision 28 source](../../28차/source/) · [Previous revision 27 · Korean](../../27차/상세설명.md) · [Original revision 26 English and earlier history](../README-revision26.md)
