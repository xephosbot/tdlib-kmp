package io.github.xephosbot.tdlib

import java.io.File

/**
 * Extracts the TDLib JNI shared library from the classpath JAR and loads it.
 *
 * The library is expected at `native/{os-arch}/libtdjson.{ext}` inside the JAR.
 * Analogous to how JetBrains/skiko loads `libskiko` on JVM.
 */
internal object TdLibLoader {
    @Volatile
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return

        val libName = System.mapLibraryName("tdjson") // libtdjson.so / libtdjson.dylib / tdjson.dll
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
            System.loadLibrary("tdjson")
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
