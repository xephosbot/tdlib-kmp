package org.xephosbot.tdlib

import java.io.File

/**
 * Extracts the TDLib JNI shared library from the classpath JAR and loads it.
 *
 * The library is expected at `native/{os-arch}/libtdjni.{ext}` inside the JAR.
 * Analogous to how JetBrains/skiko loads `libskiko` on JVM.
 */
internal object TdLibLoader {
    @Volatile
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return

        val libName = System.mapLibraryName("tdjni") // libtdjni.so / libtdjni.dylib / tdjni.dll
        val resourceDir = osResourceDir()
        val resourcePath = "/native/$resourceDir/lib/$libName"
        val stream = TdLibLoader::class.java.getResourceAsStream(resourcePath)

        if (stream != null) {
            // Extract from classpath to temp directory.
            val tempDir = File(System.getProperty("java.io.tmpdir"), "tdlib-kmp")
            tempDir.mkdirs()
            val tempFile = File(tempDir, libName)

            stream.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            System.load(tempFile.absolutePath)
        } else {
            // Fall back to system library path (for development / manual setups).
            System.loadLibrary("tdjni")
        }

        loaded = true
    }

    private fun osResourceDir(): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch")
        return when {
            os.contains("mac") -> "macos-${if (arch == "aarch64") "arm64" else "x86_64"}"
            os.contains("linux") -> "linux-${if (arch == "aarch64") "arm64" else "x86_64"}"
            os.contains("windows") -> "windows-${if (arch == "aarch64") "arm64" else "x64"}"
            else -> error("Unsupported OS: $os")
        }
    }
}
