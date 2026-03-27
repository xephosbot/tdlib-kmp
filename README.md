# tdlib-kmp

Kotlin Multiplatform library wrapping [TDLib](https://github.com/tdlib/td) (Telegram Database Library).

Architecture inspired by [JetBrains/skiko](https://github.com/JetBrains/skiko).

## Supported platforms

| Platform | Target | Linking |
|----------|--------|---------|
| Android  | `arm64-v8a` | JNI shared (`libtdjni.so`) |
| JVM      | host OS | JNI shared (extracted from classpath) |
| iOS      | `arm64`, `simulatorArm64` | cinterop + static |
| macOS    | `arm64`, `x64` | cinterop + static |
| Linux    | `x64`, `arm64` | cinterop + static |

## Project structure

```
tdlib-kmp/
├── buildSrc/src/main/kotlin/
│   ├── TdlibPlatform.kt              — OS/Arch detection
│   ├── TdlibDependencies.kt          — Artifact download tasks
│   ├── TdlibProjectContext.kt        — Central project context
│   ├── TdlibNativeConfiguration.kt   — Native target config (cinterop, static linking)
│   ├── TdlibJvmConfiguration.kt      — JVM/Android JNI loading
│   ├── TdlibSourceHierarchy.kt       — Source set hierarchy
│   └── WriteTdlibCInteropDef.kt      — Dynamic .def file generation
├── src/
│   ├── commonMain/kotlin/org/xephosbot/tdlib/
│   │   ├── TdLib.kt                   — Public API
│   │   └── NativeBridge.kt            — expect declarations
│   ├── nativeMain/kotlin/org/xephosbot/tdlib/
│   │   └── NativeBridge.native.kt     — cinterop implementation
│   ├── jvmMain/kotlin/org/xephosbot/tdlib/
│   │   ├── NativeBridge.jvm.kt        — JNI implementation
│   │   └── TdLibLoader.kt             — Native lib extractor/loader
│   ├── androidMain/kotlin/org/xephosbot/tdlib/
│   │   └── NativeBridge.android.kt    — Android JNI implementation
│   └── commonTest/kotlin/
│       └── TdLibTest.kt               — Shared tests
└── .github/workflows/
    ├── build.yml                       — CI build
    └── test.yml                        — CI test on PR
```

## Usage

```kotlin
dependencies {
    implementation("org.xephosbot:tdlib-kmp:<version>")
}
```

```kotlin
import org.xephosbot.tdlib.TdLib

val clientId = TdLib.createClientId()
TdLib.send(clientId, """{"@type":"getOption","name":"version"}""")
val response = TdLib.receive(10.0)
val syncResult = TdLib.execute("""{"@type":"getOption","name":"version"}""")
```

## Building

```bash
# Download all TDLib native artifacts
./gradlew downloadAllTdlib

# Build all targets
./gradlew build
```

## TDLib version

Current: **1.8.62** (from [xephosbot/td-pack](https://github.com/xephosbot/td-pack/releases))
