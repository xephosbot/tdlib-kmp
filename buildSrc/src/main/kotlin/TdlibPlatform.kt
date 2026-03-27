/**
 * Host OS and architecture detection.
 */

enum class TdlibOS(val id: String) {
    Windows("windows"),
    Linux("linux"),
    MacOS("macos"),
    Android("android"),
    IOS("ios");

    val isWindows get() = this == Windows
    val isMacOs get() = this == MacOS
    val isLinux get() = this == Linux
}

enum class TdlibArch(val id: String) {
    X64("x64"),
    Arm64("arm64");
}

val hostOs: TdlibOS by lazy {
    val osName = System.getProperty("os.name")
    when {
        osName.startsWith("Windows") -> TdlibOS.Windows
        osName.startsWith("Mac") || osName.startsWith("Darwin") -> TdlibOS.MacOS
        osName.startsWith("Linux") -> TdlibOS.Linux
        else -> error("Unsupported host OS: $osName")
    }
}

val hostArch: TdlibArch by lazy {
    val arch = System.getProperty("os.arch")
    when (arch) {
        "amd64", "x86_64" -> TdlibArch.X64
        "aarch64", "arm64" -> TdlibArch.Arm64
        else -> error("Unsupported host architecture: $arch")
    }
}

