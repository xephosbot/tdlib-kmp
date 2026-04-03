enum class TdlibTarget(
    val kotlinTargetName: String,
    val assetName: String,
    val resourcePath: String,
    val os: TdlibOS,
    val arch: TdlibArch,
    val isSimulator: Boolean = false,
) {
    // Kotlin/Native (static libs + cinterop)
    IOS_ARM64("iosArm64", "tdlib-ios-arm64", "ios_arm64", TdlibOS.IOS, TdlibArch.Arm64),
    IOS_SIM_ARM64("iosSimulatorArm64", "tdlib-ios-arm64-simulator", "ios_arm64_simulator", TdlibOS.IOS, TdlibArch.Arm64, true),
    MACOS_ARM64("macosArm64", "tdlib-macos-arm64", "macos_arm64", TdlibOS.MacOS, TdlibArch.Arm64),
    MACOS_X64("macosX64", "tdlib-macos-x86_64", "macos_x64", TdlibOS.MacOS, TdlibArch.X64),
    LINUX_X64("linuxX64", "tdlib-linux-x86_64", "linux_x64", TdlibOS.Linux, TdlibArch.X64),
    LINUX_ARM64("linuxArm64", "tdlib-linux-arm64", "linux_arm64", TdlibOS.Linux, TdlibArch.Arm64),

    // JNI shared libraries (all platforms bundled into JVM JAR)
    JNI_MACOS_ARM64("jvm", "tdlib-macos-jni-arm64", "macos_arm64", TdlibOS.MacOS, TdlibArch.Arm64),
    JNI_MACOS_X64("jvm", "tdlib-macos-jni-x86_64", "macos_x64", TdlibOS.MacOS, TdlibArch.X64),
    JNI_LINUX_X64("jvm", "tdlib-linux-jni-x86_64", "linux_x64", TdlibOS.Linux, TdlibArch.X64),
    JNI_LINUX_ARM64("jvm", "tdlib-linux-jni-arm64", "linux_arm64", TdlibOS.Linux, TdlibArch.Arm64),
    JNI_WIN_X64("jvm", "tdlib-windows-jni-x64", "windows_x64", TdlibOS.Windows, TdlibArch.X64),
    JNI_WIN_ARM64("jvm", "tdlib-windows-jni-arm64", "windows_arm64", TdlibOS.Windows, TdlibArch.Arm64),

    // Android .so (resourcePath = ABI name for jniLibs/{abi}/)
    ANDROID_ARM64("android", "tdlib-android-arm64-v8a", "arm64-v8a", TdlibOS.Android, TdlibArch.Arm64),
    ANDROID_ARM32("android", "tdlib-android-armeabi-v7a", "armeabi-v7a", TdlibOS.Android, TdlibArch.Arm64),
    ANDROID_X86("android", "tdlib-android-x86", "x86", TdlibOS.Android, TdlibArch.X64),
    ANDROID_X86_64("android", "tdlib-android-x86_64", "x86_64", TdlibOS.Android, TdlibArch.X64),
    ;

    /** Local directory name under build/tdlib/ where the archive is extracted. */
    val extractDir: String get() = assetName.removePrefix("tdlib-")

    companion object {
        val nativeTargets get() = entries.filter { it.os != TdlibOS.Android && !it.name.startsWith("JNI_") }
        val jniTargets get() = entries.filter { it.name.startsWith("JNI_") }
        val androidTargets get() = entries.filter { it.os == TdlibOS.Android }
    }
}
