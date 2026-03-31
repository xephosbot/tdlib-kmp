package io.xbot.tdlib

import java.io.File

internal actual object TdLibLoader {
    @Volatile private var loaded = false

    @Synchronized
    actual fun load() {
        if (loaded) return
        val libName = System.mapLibraryName("tdjsonjava")
        val resourceDir = osResourceDir()
        val resourcePath = "/native/$resourceDir/lib/$libName"
        val stream = TdLibLoader::class.java.getResourceAsStream(resourcePath)
        if (stream != null) {
            val tempDir = File(System.getProperty("java.io.tmpdir"), "tdlib-kmp")
            tempDir.mkdirs()
            val tempFile = File(tempDir, libName)
            stream.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            System.load(tempFile.absolutePath)
        } else {
            System.loadLibrary("tdjsonjava")
        }
        loaded = true
    }

    private fun osResourceDir(): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = archId(System.getProperty("os.arch"))
        return when {
            os.contains("mac") -> "macos-$arch"
            os.contains("linux") -> "linux-$arch"
            os.contains("windows") -> "windows-${if (arch == "arm64") "arm64" else "x64"}"
            else -> error("Unsupported OS: $os")
        }
    }

    private fun archId(arch: String): String = when (arch) {
        "aarch64", "arm64" -> "arm64"
        else -> "x86_64"
    }
}