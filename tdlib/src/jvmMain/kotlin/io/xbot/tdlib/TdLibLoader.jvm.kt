package io.xbot.tdlib

import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal actual object TdLibLoader {
    @Volatile private var loaded = false

    actual fun load() {
        if (loaded) return
        loadSynchronous()
    }

    @Synchronized
    private fun loadSynchronous() {
        // Double-checked locking
        if (loaded) return
        val libName = System.mapLibraryName("tdjsonjava")
        val resourcePath = "/natives/${osResourceDir()}/$libName"

        val tempFile = Files.createTempFile(libName, null).apply { toFile().deleteOnExit() }
        TdLibLoader::class
            .java
            .getResourceAsStream(resourcePath)!!
            .use { resourceStream ->
                Files.copy(resourceStream, tempFile, StandardCopyOption.REPLACE_EXISTING)
            }
        System.load(tempFile.toFile().canonicalPath)
        loaded = true
    }

    private fun osResourceDir(): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = archId(System.getProperty("os.arch"))
        return when {
            os.contains("mac")     -> "macos_$arch"
            os.contains("linux")   -> "linux_$arch"
            os.contains("windows") -> "windows_$arch"
            else -> error("Unsupported OS: $os")
        }
    }

    private fun archId(arch: String): String = when (arch) {
        "aarch64", "arm64" -> "arm64"
        else -> "64"
    }
}