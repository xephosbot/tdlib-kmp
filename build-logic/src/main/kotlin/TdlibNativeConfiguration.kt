import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess

private val TDLIB_STATIC_LIBS = listOf(
    "libtdjson_static.a",
    "libtdjson_private.a",
    "libtdclient.a",
    "libtdcore.a",
    "libtdapi.a",
    "libtdactor.a",
    "libtdutils.a",
    "libtddb.a",
    "libtdsqlite.a",
    "libtdnet.a",
    "libtdmtproto.a",
    "libtde2e.a",
    "libcrypto.a",
    "libssl.a",
)

private fun linkerFlagsFor(os: TdlibOS): List<String> = when (os) {
    TdlibOS.MacOS, TdlibOS.IOS -> listOf(
        "-framework", "Security",
        "-framework", "Foundation",
        "-lz", "-lc++",
    )
    TdlibOS.Linux   -> listOf("-lz", "-lstdc++", "-lm", "-ldl", "-lpthread")
    TdlibOS.Windows -> listOf("-lws2_32", "-lcrypt32", "-lnormaliz")
    else -> emptyList()
}

/**
 * Configures a Kotlin/Native target for TDLib:
 *
 * 1. Registers download + extract tasks for the target's static library archive.
 * 2. Dynamically generates a .def file via [WriteTdlibCInteropDef].
 * 3. Registers the `tdjson` cinterop.
 * 4. Passes all static libraries and linker flags via freeCompilerArgs on binaries.
 *
 * The -include-binary flag is only passed to binaries (link stage), not to compilations,
 * because it instructs the linker to bundle the static library into the klib — not the compiler.
 */
fun configureCinteropTarget(
    project: Project,
    deps: TdlibDependencies,
    target: KotlinNativeTarget,
    spec: TdlibTarget,
) = with(project) {
    val extractDir = layout.buildDirectory.dir("tdlib/${spec.extractDir}")
    val extractTask = deps.register(spec)

    val staticLibPaths = TDLIB_STATIC_LIBS.map {
        extractDir.get().asFile.resolve("lib/$it").absolutePath
    }
    val linkerFlags = linkerFlagsFor(spec.os)

    val allFlags = staticLibPaths.flatMap { listOf("-include-binary", it) } +
        linkerFlags.flatMap { listOf("-linker-option", it) }

    val suffix = spec.extractDir
        .split("-")
        .joinToString("") { it.replaceFirstChar(Char::titlecase) }

    val writeDef = tasks.register<WriteTdlibCInteropDef>("writeTdlibDef$suffix") {
        headerPaths.set(listOf(extractDir.get().asFile.resolve("include").absolutePath))
        this.linkerOpts.set(linkerFlags)
        outputFile.set(layout.buildDirectory.file("cinterop/${spec.extractDir}/tdjson.def"))
    }

    tasks.withType<CInteropProcess>().configureEach {
        if (konanTarget == target.konanTarget) {
            dependsOn(writeDef, extractTask)
        }
    }

    target.compilations.getByName("main") {
        cinterops.create("tdjson") {
            definitionFile.set(writeDef.flatMap { it.outputFile })
        }
    }

    target.compilations.all {
        compileTaskProvider.configure {
            dependsOn(extractTask)
        }
    }

    target.binaries.all {
        freeCompilerArgs += allFlags
    }
}
