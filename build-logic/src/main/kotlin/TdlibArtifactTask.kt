import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Task that downloads and extracts a TDLib native artifact.
 */
@CacheableTask
abstract class TdlibArtifactTask @Inject constructor(
    private val fs: FileSystemOperations,
    private val archiveOps: ArchiveOperations,
) : DefaultTask() {

    @get:Input
    abstract val tdlibVersion: Property<String>

    @get:Input
    abstract val assetName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun run() {
        val version = tdlibVersion.get()
        val asset = assetName.get()
        val outDir = outputDirectory.get().asFile
        val archiveName = "$asset.tar.gz"

        val archiveUrl = "https://github.com/xephosbot/td-pack/releases/download/v$version/$archiveName"
        val expectedSha = readReleaseSha256(version, archiveName)
        val shaFile = File(outDir, ".tdlib.sha256")

        if (shaFile.exists() && shaFile.readText().trim() == expectedSha && outDir.listFiles().orEmpty().isNotEmpty()) {
            throw StopExecutionException("TDLib artifact already prepared for $asset (sha256=$expectedSha)")
        }

        outDir.deleteRecursively()
        outDir.mkdirs()

        val archiveFile = File(temporaryDir, archiveName)
        val action = DownloadAction(project, this).apply {
            src(archiveUrl)
            dest(archiveFile)
            overwrite(true)
            acceptAnyCertificate(true)
        }
        action.execute().get()

        val actualSha = sha256Hex(archiveFile)
        check(actualSha == expectedSha) {
            "SHA-256 mismatch for $archiveUrl:\n  expected: $expectedSha\n  actual:   $actualSha"
        }

        fs.copy {
            from(archiveOps.tarTree(archiveFile))
            into(outDir)
        }
        shaFile.writeText("$expectedSha\n")
    }

    private fun readReleaseSha256(version: String, archiveName: String): String {
        val releaseApiUrl = "https://api.github.com/repos/xephosbot/td-pack/releases/tags/v$version"
        val body = URI(releaseApiUrl).toURL().openStream().bufferedReader().use { it.readText() }
        val escaped = Regex.escape(archiveName)
        val regex = Regex("\"name\"\\s*:\\s*\"$escaped\"[\\s\\S]*?\"digest\"\\s*:\\s*\"sha256:([A-Fa-f0-9]{64})\"")
        return regex.find(body)?.groupValues?.get(1)?.lowercase()
            ?: error("Unable to find sha256 digest for $archiveName in $releaseApiUrl")
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
