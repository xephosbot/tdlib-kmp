import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.Family

/**
 * Static libraries bundled inside every td-pack native archive.
 */
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

private fun linkerFlagsFor(family: Family): List<String> = when (family) {
    Family.OSX, Family.IOS -> listOf(
        "-framework", "Security",
        "-framework", "Foundation",
        "-lz", "-lc++",
    )
    Family.LINUX -> listOf("-lz", "-lstdc++", "-lm", "-ldl", "-lpthread")
    Family.MINGW -> listOf("-lws2_32", "-lcrypt32", "-lnormaliz")
    else -> emptyList()
}


/**
 * Configures a Kotlin/Native target for TDLib:
 *
 * 1. Dynamically generates a `.def` file via [WriteTdlibCInteropDef].
 * 2. Registers a cinterop named `tdjson`.
 * 3. Passes all static libraries via `-include-binary`.
 * 4. Adds platform-specific linker flags.
 *
 * Analogous to `configureNativeTarget` in JetBrains/skiko
 * (`NativeTasksConfiguration.kt`).
 */
fun TdlibProjectContext.configureNativeTarget(target: KotlinNativeTarget) = with(project) {
    val os = target.konanTarget.family.toTdlibOS()
    val arch = target.konanTarget.architecture.toTdlibArch()
    val isSimulator = target.konanTarget.isSimulator

    val artifactTask = tdlibDeps.nativeFor(target)
    val localDirStr = TdlibTarget(os, arch).nativeLocalDir(isSimulator)
    // Path is known statically at configuration time — no Provider.get() needed.
    // The files themselves are guaranteed to exist by dependsOn(artifactTask) below.
    val extractDir = rootProject.rootDir.resolve("prebuilds/natives/$localDirStr")

    val staticLibPaths = TDLIB_STATIC_LIBS.map { extractDir.resolve("lib/$it").absolutePath }
    val linkerFlags = linkerFlagsFor(target.konanTarget.family)
    val allFlags: List<String> =
        staticLibPaths.flatMap { listOf("-include-binary", it) } +
        linkerFlags.flatMap { listOf("-linker-option", it) }

    val suffix = localDirStr
        .split("-")
        .joinToString("") { it.replaceFirstChar(Char::titlecase) }

    val writeDef = tasks.register<WriteTdlibCInteropDef>("writeTdlibDef$suffix") {
        headerPaths.set(listOf(extractDir.resolve("include").absolutePath))
        this.linkerOpts.set(linkerFlags)
        // Use localDirStr (String) to avoid Provider.toString() garbage path in the cinterop dir name
        outputFile.set(layout.buildDirectory.file("cinterop/$localDirStr/tdjson.def"))
    }

    // Make sure the cinterop task depends on the .def-writing task.
    tasks.withType<CInteropProcess>().configureEach {
        if (konanTarget == target.konanTarget) {
            dependsOn(writeDef, artifactTask)
        }
    }

    target.compilations.getByName("main") {
        cinterops.create("tdjson") {
            definitionFile.set(writeDef.flatMap { it.outputFile })
        }
    }

    target.compilations.all {
        compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.addAll(allFlags)
            dependsOn(artifactTask)
        }
    }

    target.binaries.all {
        freeCompilerArgs += allFlags
    }
}
