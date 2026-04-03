import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

/**
 * Manages TDLib native artifact downloads from GitHub releases.
 *
 * Release URL pattern:
 *   https://github.com/xephosbot/td-pack/releases/download/v{version}/{asset}.tar.gz
 *
 * Archives are extracted to build/tdlib/{target.extractDir}/.
 */
class TdlibDependencies(
    private val project: Project,
    private val tdlibVersion: String,
) {
    private val releaseUrl = "https://github.com/xephosbot/td-pack/releases/download/v$tdlibVersion"

    /** Already-registered extract task names (avoids duplicates). */
    private val registered = mutableMapOf<String, String>()

    /** Registers download + extract tasks for the given target and returns the extract task name. */
    fun register(target: TdlibTarget): String =
        registered.getOrPut(target.assetName) {
            val suffix = target.extractDir
                .split("-")
                .joinToString("") { it.replaceFirstChar(Char::titlecase) }

            val dlTask = "downloadTdlib$suffix"
            val exTask = "extractTdlib$suffix"

            val targetDir = project.layout.buildDirectory.dir("tdlib/${target.extractDir}")

            project.tasks.register(dlTask, Download::class.java) {
                group = "tdlib"
                description = "Download ${target.assetName}.tar.gz"
                src("$releaseUrl/${target.assetName}.tar.gz")
                dest(project.layout.buildDirectory.file("downloads/${target.assetName}.tar.gz"))
                overwrite(false)
                acceptAnyCertificate(true)
                onlyIf { targetDir.get().asFile.let { !it.exists() || it.listFiles().isNullOrEmpty() } }
            }

            project.tasks.register(exTask, Copy::class.java) {
                group = "tdlib"
                description = "Extract ${target.assetName} → build/tdlib/${target.extractDir}"
                dependsOn(dlTask)
                from(project.tarTree(project.layout.buildDirectory.file("downloads/${target.assetName}.tar.gz")))
                into(targetDir)
                onlyIf { targetDir.get().asFile.let { !it.exists() || it.listFiles().isNullOrEmpty() } }
            }

            exTask
        }
}
