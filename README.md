# tdlib-kmp

Kotlin Multiplatform library wrapping [TDLib](https://github.com/tdlib/td) (Telegram Database Library).

Architecture inspired by [JetBrains/skiko](https://github.com/JetBrains/skiko).

## Supported platforms

| Platform | Target | Linking |
|----------|--------|---------|
| Android  | `arm64-v8a`, `armeabi-v7a`, `x86` | JNI shared (`libtdjsonjava.so`) |
| JVM      | host OS | JNI shared (`libtdjson`, extracted from classpath) |
| iOS      | `arm64`, `simulatorArm64` | cinterop + static |
| macOS    | `arm64`, `x64` | cinterop + static |
| Linux    | `x64`, `arm64` | cinterop + static |

## Project structure

```
tdlib-kmp/
├── build.gradle.kts                    — Root project (plugin declarations)
├── settings.gradle.kts                 — Includes :tdlib submodule
├── buildSrc/src/main/kotlin/
│   ├── TdlibPlatform.kt               — OS/Arch detection
│   ├── TdlibDependencies.kt           — Artifact download tasks
│   ├── TdlibProjectContext.kt         — Central project context
│   ├── TdlibNativeConfiguration.kt    — Native target config (cinterop, static linking)
│   ├── TdlibJvmConfiguration.kt       — JVM/Android JNI loading
│   ├── TdlibSourceHierarchy.kt        — Source set hierarchy
│   └── WriteTdlibCInteropDef.kt       — Dynamic .def file generation
├── tdlib/                              — Core library module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/io/github/xephosbot/tdlib/
│       │   ├── TdLib.kt                — Public API
│       │   └── NativeBridge.kt         — expect declarations
│       ├── nativeMain/kotlin/io/github/xephosbot/tdlib/
│       │   └── NativeBridge.native.kt  — cinterop implementation
│       ├── jvmMain/kotlin/io/github/xephosbot/tdlib/
│       │   ├── NativeBridge.jvm.kt     — JNI implementation
│       │   └── TdLibLoader.kt          — Native lib extractor/loader
│       ├── androidMain/kotlin/io/github/xephosbot/tdlib/
│       │   └── NativeBridge.android.kt — Android JNI implementation
│       └── commonTest/kotlin/
│           └── TdLibTest.kt            — Shared tests
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
import io.github.xephosbot.tdlib.TdLib

val clientId = TdLib.createClientId()
TdLib.send(clientId, """{"@type":"getOption","name":"version"}""")
val response = TdLib.receive(10.0)
val syncResult = TdLib.execute("""{"@type":"getOption","name":"version"}""")
```

## Building

```bash
# Build all targets (dependencies are downloaded lazily)
./gradlew :tdlib:build
```

## TDLib version

Current: **1.8.62** (from [xephosbot/td-pack](https://github.com/xephosbot/td-pack/releases))
