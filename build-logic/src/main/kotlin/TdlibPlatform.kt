/*
 * Platform model for tdlib-kmp.
 *
 * Contains:
 *  - TdlibOS / TdlibArch enums and their Konan converters.
 *  - TdlibTarget data class used as the single key for all path decisions.
 *  - Centralised path helpers on TdlibTarget:
 *      nativeLocalDir()  — local dir for K/N static artifacts  (e.g. linux-x86_64)
 *      jniLocalDir()     — local dir for JNI shared artifacts  (e.g. linux-x86_64-jni)
 *      jvmResourceDir()  — JAR resource path for fat JVM JAR  (e.g. natives/linux_64)
 */
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

enum class TdlibOS(val id: String) {
    Windows("windows"),
    Linux("linux"),
    MacOS("macos"),
    Android("android"),
    IOS("ios");
}

enum class TdlibArch(val id: String) {
    X64("x64"),
    Arm64("arm64");
}

fun Family.toTdlibOS(): TdlibOS = when (this) {
    Family.MINGW -> TdlibOS.Windows
    Family.LINUX -> TdlibOS.Linux
    Family.OSX -> TdlibOS.MacOS
    Family.ANDROID -> TdlibOS.Android
    Family.IOS -> TdlibOS.IOS
    else -> error("Unsupported family: $this")
}

fun Architecture.toTdlibArch(): TdlibArch = when (this) {
    Architecture.X64 -> TdlibArch.X64
    Architecture.ARM64 -> TdlibArch.Arm64
    else -> error("Unsupported arch: $this")
}

internal data class TdlibTarget(val os: TdlibOS, val arch: TdlibArch)

/**
 * True when this Konan target is an iOS/watchOS/tvOS simulator.
 *
 * Two cases must be handled:
 *  - New-style names contain "simulator" (e.g. iosSimulatorArm64 → "ios_simulator_arm64").
 *  - Legacy iosX64 target name is just "ios_x64" — no "simulator" in the name,
 *    but for iOS the x86_64 architecture is exclusively used by the simulator.
 */
internal val KonanTarget.isSimulator: Boolean
    get() = name.contains("simulator", ignoreCase = true) ||
            (family == Family.IOS && architecture == Architecture.X64)

/**
 * Local directory name for extracted native (K/N) artifacts.
 * e.g. linux-x86_64, macos-arm64, ios-arm64-simulator
 */
internal fun TdlibTarget.nativeLocalDir(isSimulator: Boolean = false): String {
    val tail = when {
        os == TdlibOS.IOS && isSimulator -> if (arch == TdlibArch.Arm64) "arm64-simulator" else "x86_64-simulator"
        os == TdlibOS.IOS                -> "arm64"
        os == TdlibOS.Linux              -> if (arch == TdlibArch.X64) "x86_64" else "arm64"
        os == TdlibOS.MacOS              -> if (arch == TdlibArch.X64) "x86_64" else "arm64"
        os == TdlibOS.Windows            -> if (arch == TdlibArch.X64) "x64"    else "arm64"
        else -> error("Unsupported: $os / $arch")
    }
    return "${os.id}-$tail"
}

/**
 * Local directory name for extracted JNI artifacts.
 * e.g. linux-x86_64-jni, macos-arm64-jni, windows-x64-jni
 */
internal fun TdlibTarget.jniLocalDir(): String = "${nativeLocalDir()}-jni"

/**
 * Resource path used inside the JVM fat JAR.
 * Follows the /natives/{os}_{arch}/ convention.
 * e.g. natives/linux_64, natives/macos_arm64, natives/windows_64
 */
internal fun TdlibTarget.jvmResourceDir(): String {
    val archSuffix = if (arch == TdlibArch.X64) "64" else "arm64"
    return "natives/${os.id}_$archSuffix"
}
