import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess

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

/**
 * Returns a local directory name for the given OS/arch/simulator combination.
 * Must match the values used in [TdlibDependencies.localDir].
 */
private fun localDirName(os: TdlibOS, arch: TdlibArch, isSimulator: Boolean): String {
    val tail = when {
        os == TdlibOS.IOS && isSimulator -> if (arch == TdlibArch.Arm64) "arm64-simulator" else "x86_64-simulator"
        os == TdlibOS.IOS                -> "arm64"
        os == TdlibOS.Linux              -> if (arch == TdlibArch.X64) "x86_64" else "arm64"
        os == TdlibOS.MacOS              -> if (arch == TdlibArch.X64) "x86_64" else "arm64"
        os == TdlibOS.Windows            -> if (arch == TdlibArch.X64) "x64"    else "arm64"
        else -> error("Unsupported: $os / $arch")
    }
    return "${os.id}-$tail"
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
fun TdlibProjectContext.configureNativeTarget(
    os: TdlibOS,
    arch: TdlibArch,
    target: KotlinNativeTarget,
    isSimulator: Boolean = false,
) = with(project) {
    val localDir = localDirName(os, arch, isSimulator)
    val libsDir = project.file("libs/$localDir")

    // Absolute paths to every static library that must be linked into the klib.
    val staticLibPaths = TDLIB_STATIC_LIBS.map { libsDir.resolve("lib/$it").absolutePath }

    // Platform-specific linker options.
    val linkerFlags: List<String> = when (os) {
        TdlibOS.MacOS -> listOf(
            "-framework", "Security",
            "-framework", "Foundation",
            "-lz", "-lc++",
        )
        TdlibOS.IOS -> listOf(
            "-framework", "Security",
            "-framework", "Foundation",
            "-lz", "-lc++",
        )
        TdlibOS.Linux -> listOf("-lz", "-lstdc++", "-lm", "-ldl", "-lpthread")
        TdlibOS.Windows -> listOf("-lws2_32", "-lcrypt32", "-lnormaliz")
        else -> emptyList()
    }

    // ---------------------------------------------------------------
    // 1. Dynamic .def generation
    // ---------------------------------------------------------------
    val targetSuffix = localDir.split("-")
        .joinToString("") { it.replaceFirstChar(Char::titlecase) }

    val writeDef = tasks.register<WriteTdlibCInteropDef>("writeTdlibDef$targetSuffix") {
        headerPaths.set(listOf(libsDir.resolve("include").absolutePath))
        this.linkerOpts.set(linkerFlags)
        outputFile.set(layout.buildDirectory.file("cinterop/$localDir/tdjson.def"))
    }

    // Make sure the cinterop task depends on the .def-writing task.
    tasks.withType<CInteropProcess>().configureEach {
        if (konanTarget == target.konanTarget) {
            dependsOn(writeDef)
        }
    }

    // ---------------------------------------------------------------
    // 2. Register the `tdjson` cinterop
    // ---------------------------------------------------------------
    target.compilations.getByName("main") {
        cinterops.create("tdjson") {
            definitionFile.set(writeDef.flatMap { it.outputFile })
        }
    }

    // ---------------------------------------------------------------
    // 3. Include static binaries + linker flags
    // ---------------------------------------------------------------
    val allFlags = staticLibPaths.flatMap { listOf("-include-binary", it) } +
        linkerFlags.flatMap { listOf("-linker-option", it) }

    target.binaries.all {
        freeCompilerArgs += allFlags
    }

    target.compilations.all {
        compilerOptions.configure {
            freeCompilerArgs.addAll(allFlags)
        }
    }
}

/**
 * Wires the extract task for a given native target so that
 * cinterop and compilation tasks depend on extraction.
 */
fun TdlibProjectContext.wireNativeExtractTask(
    extractTaskName: String,
    target: KotlinNativeTarget,
) = with(project) {
    // cinterop tasks
    tasks.withType<CInteropProcess>().configureEach {
        if (konanTarget == target.konanTarget) {
            dependsOn(extractTaskName)
        }
    }
    // compilation tasks
    target.compilations.all {
        compileTaskProvider.configure {
            dependsOn(extractTaskName)
        }
    }
}
