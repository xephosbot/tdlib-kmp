import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
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

        // Clean up before extraction to ensure a clean state
        outDir.deleteRecursively()
        outDir.mkdirs()

        val url = "https://github.com/xephosbot/td-pack/releases/download/v$version/$asset.tar.gz"
        val archiveFile = File(temporaryDir, "$asset.tar.gz")
        temporaryDir.mkdirs()

        val action = DownloadAction(project, this)
        action.src(url)
        action.dest(archiveFile)
        action.overwrite(true)
        action.acceptAnyCertificate(true)
        action.execute().get()

        check(archiveFile.exists()) {
            "Download of $url seems to have failed — archive not found at expected path:\n  $archiveFile"
        }

        fs.copy {
            from(archiveOps.tarTree(archiveFile))
            into(outDir)
        }
    }
}
