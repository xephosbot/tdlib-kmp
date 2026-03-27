import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Project
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Copy
import java.io.File

/**
 * Manages TDLib native artifact downloads from GitHub releases.
 *
 * Inspired by Skiko: each target lazily downloads only the artifacts
 * it needs — not the entire release.
 *
 * Release URL pattern:
 *   https://github.com/xephosbot/td-pack/releases/download/v{version}/{asset}.tar.gz
 *
 * Archive layout (root directory is stripped on extraction):
 *   tdlib-ios-arm64/
 *     include/ …
 *     lib/ …
 */
class TdlibDependencies(
    private val project: Project,
    private val tdlibVersion: String,
) {
    private val releaseUrl = "https://github.com/xephosbot/td-pack/releases/download/v$tdlibVersion"
    private val libsDir: File = project.file("libs")

    /** Already-registered extract task names (avoids duplicates). */
    private val registered = mutableMapOf<String, String>()

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** JNI shared library for the current host OS (used by JVM target). */
    fun jniForHost(): String {
        val archId = when (hostOs) {
            TdlibOS.Windows -> if (hostArch == TdlibArch.X64) "x64" else "arm64"
            else             -> if (hostArch == TdlibArch.X64) "x86_64" else "arm64"
        }
        val localArch = if (hostOs == TdlibOS.Windows && hostArch == TdlibArch.X64) "x64" else archId
        return register("tdlib-${hostOs.id}-jni-$archId", "${hostOs.id}-$localArch-jni")
    }

    /** Static / native library for a K/N target. */
    fun nativeFor(os: TdlibOS, arch: TdlibArch, isSimulator: Boolean = false): String {
        val asset = assetName(os, arch, isSimulator)
        val local = localDir(os, arch, isSimulator)
        return register(asset, local)
    }

    /** Android SO for a given ABI string (e.g. "arm64-v8a"). */
    fun androidFor(abi: String): String =
        register("tdlib-android-$abi", "android-$abi")

    // ------------------------------------------------------------------
    // Task registration (idempotent)
    // ------------------------------------------------------------------

    private fun register(assetName: String, localDir: String): String =
        registered.getOrPut(assetName) {
            val suffix = assetName.removePrefix("tdlib-")
                .split("-")
                .joinToString("") { it.replaceFirstChar(Char::titlecase) }

            val dlTask = "downloadTdlib$suffix"
            val exTask = "extractTdlib$suffix"

            val targetDir = libsDir.resolve(localDir)

            project.tasks.register(dlTask, Download::class.java) {
                group = "tdlib"
                description = "Download $assetName.tar.gz"
                src("$releaseUrl/$assetName.tar.gz")
                dest(project.layout.buildDirectory.file("downloads/$assetName.tar.gz"))
                overwrite(false)
                acceptAnyCertificate(true)
                onlyIf { !targetDir.exists() || targetDir.listFiles().isNullOrEmpty() }
            }

            project.tasks.register(exTask, Copy::class.java) {
                group = "tdlib"
                description = "Extract $assetName → libs/$localDir"
                dependsOn(dlTask)
                from(project.tarTree(project.layout.buildDirectory.file("downloads/$assetName.tar.gz"))) {
                    eachFile {
                        // strip root dir inside archive (e.g. "tdlib-ios-arm64/")
                        relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
                    }
                    includeEmptyDirs = false
                }
                into(targetDir)
                onlyIf { !targetDir.exists() || targetDir.listFiles().isNullOrEmpty() }
            }

            exTask
        }

    // ------------------------------------------------------------------
    // Asset / local-dir name helpers
    // ------------------------------------------------------------------

    private fun assetName(os: TdlibOS, arch: TdlibArch, sim: Boolean): String {
        val tail = when {
            os == TdlibOS.IOS && sim  -> if (arch == TdlibArch.Arm64) "arm64-simulator" else "x86_64-simulator"
            os == TdlibOS.IOS         -> "arm64"
            os == TdlibOS.Linux       -> if (arch == TdlibArch.X64) "x86_64" else "arm64"
            os == TdlibOS.MacOS       -> if (arch == TdlibArch.X64) "x86_64" else "arm64"
            os == TdlibOS.Windows     -> if (arch == TdlibArch.X64) "x64"    else "arm64"
            else -> error("Unsupported: $os / $arch")
        }
        return "tdlib-${os.id}-$tail"
    }

    private fun localDir(os: TdlibOS, arch: TdlibArch, sim: Boolean): String {
        val tail = when {
            os == TdlibOS.IOS && sim  -> if (arch == TdlibArch.Arm64) "arm64-simulator" else "x86_64-simulator"
            os == TdlibOS.IOS         -> "arm64"
            os == TdlibOS.Linux       -> if (arch == TdlibArch.X64) "x86_64" else "arm64"
            os == TdlibOS.MacOS       -> if (arch == TdlibArch.X64) "x86_64" else "arm64"
            os == TdlibOS.Windows     -> if (arch == TdlibArch.X64) "x64"    else "arm64"
            else -> error("Unsupported: $os / $arch")
        }
        return "${os.id}-$tail"
    }
}
