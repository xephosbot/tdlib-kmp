import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Generates a .def file for Kotlin/Native cinterop with TDLib.
 *
 * Analogous to `WriteCInteropDefFile` in JetBrains/skiko.
 * The .def is created dynamically so that header paths and linker
 * options can be resolved at configuration time per target.
 */
@CacheableTask
internal abstract class WriteTdlibCInteropDef : DefaultTask() {
    @get:Input
    abstract val headerPaths: ListProperty<String>

    @get:Input
    abstract val linkerOpts: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val newContent = buildString {
            appendLine("package = io.xbot.tdlib.cinterop")
            appendLine("headers = td_json_client.h td_log.h")
            appendLine("headerFilter = td_json_client.h td_log.h td/telegram/**")

            val hpaths = headerPaths.get()
            if (hpaths.isNotEmpty()) {
                appendLine("compilerOpts = ${hpaths.joinToString(" ") { "-I$it" }}")
            }

            val lopts = linkerOpts.get()
            if (lopts.isNotEmpty()) {
                appendLine("linkerOpts = ${lopts.joinToString(" ")}")
            }
        }

        val file = outputFile.get().asFile
        // Only write if content actually changed — keeps the file timestamp stable
        // so downstream cinteropTdjson* tasks remain UP-TO-DATE when nothing changed.
        if (!file.exists() || file.readText() != newContent) {
            file.parentFile.mkdirs()
            file.writeText(newContent)
        }
    }
}
