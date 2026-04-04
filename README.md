# tdlib-kmp

Kotlin Multiplatform library wrapping [TDLib](https://github.com/tdlib/td) (Telegram Database Library).

Architecture inspired by [JetBrains/skiko](https://github.com/JetBrains/skiko).

## Supported platforms

| Platform | Target | Linking |
|----------|--------|---------|
| Android  | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` | JNI shared (`libtdjsonjava.so`) |
| JVM      | Windows x64/arm64, Linux x64/arm64, macOS x64/arm64 | JNI shared (fat JAR, extracted at runtime) |
| iOS      | `arm64`, `simulatorArm64` | cinterop + static |
| macOS    | `arm64`, `x64` | cinterop + static |
| Linux    | `x64`, `arm64` | cinterop + static |

## Project structure

```
tdlib-kmp/
├── build.gradle.kts                    — Root project (plugin declarations)
├── settings.gradle.kts                 — Includes :tdlib submodule
├── build-logic/src/main/kotlin/
│   ├── TdlibPlatform.kt               — Platform enums, path helpers (nativeLocalDir / jniLocalDir / jvmResourceDir)
│   ├── TdlibDependencies.kt           — Artifact download & extract tasks
│   ├── TdlibProjectContext.kt         — Central project context
│   ├── TdlibConventionPlugin.kt       — Convention plugin wiring all targets
│   ├── TdlibNativeConfiguration.kt    — Native target config (cinterop, static linking)
│   ├── TdlibJvmConfiguration.kt       — JVM/Android JNI staging & AAR packaging
│   ├── TdlibSourceHierarchy.kt        — Source set hierarchy
│   └── WriteTdlibCInteropDef.kt       — Dynamic .def file generation
├── tdlib/                              — Core library module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/io/xbot/tdlib/
│       │   ├── TdLib.kt                — Public API
│       │   └── NativeBridge.kt         — expect declarations
│       ├── nativeMain/kotlin/io/xbot/tdlib/
│       │   └── NativeBridge.native.kt  — cinterop implementation
│       ├── jvmMain/kotlin/io/xbot/tdlib/
│       │   ├── NativeBridge.jvm.kt     — JNI implementation
│       │   └── TdLibLoader.kt          — Native lib extractor/loader
│       ├── androidMain/kotlin/io/xbot/tdlib/
│       │   └── NativeBridge.android.kt — Android JNI implementation
│       └── commonTest/kotlin/
│           └── TdLibTest.kt            — Shared tests
├── prebuilds/natives/                  — Extracted native artifacts (gitignored, survives ./gradlew clean)
│   ├── linux-x86_64[-jni]/             — Linux desktop & JNI libs
│   ├── macos-arm64[-jni]/              — macOS desktop & JNI libs
│   ├── windows-x64-jni/               — Windows JNI libs
│   ├── ios-arm64[-simulator]/          — iOS static libs
│   └── android-{abi}/                  — Android .so files
└── .github/workflows/
    ├── build.yml                        — CI build
    └── test.yml                         — CI test on PR
```

## Usage

```kotlin
dependencies {
    implementation("io.github.xephosbot:tdlib-kmp:<version>")
}
```

```kotlin
import io.xbot.tdlib.TdLib

val clientId = TdLib.createClientId()
TdLib.send(clientId, """{"@type":"getOption","name":"version"}""")
val response = TdLib.receive(10.0)
val syncResult = TdLib.execute("""{"@type":"getOption","name":"version"}""")
```

## Building

```bash
# Build all targets (native artifacts are downloaded and extracted lazily on first build)
./gradlew :tdlib:build
```

Extracted artifacts are cached in `prebuilds/natives/` at the project root and **survive
`./gradlew clean`**. Download archives are placed in `build/downloads/` and are wiped by
`clean` — they will be re-fetched if `prebuilds/natives/` is missing.

## TDLib version

Current: **1.8.62** (from [xephosbot/td-pack](https://github.com/xephosbot/td-pack/releases))
