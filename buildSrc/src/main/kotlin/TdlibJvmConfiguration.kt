import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.register

/**
 * JVM & Android target configuration for TDLib.
 *
 * Analogous to `JvmTasksConfiguration.kt` in JetBrains/skiko.
 *
 * JVM target:
 *   Packages the host-OS JNI shared library (`libtdjson.so` / `libtdjson.dylib`
 *   / `tdjson.dll`) into the JVM JAR under `native/{os-arch}/`.
 *   At runtime [TdLibLoader] extracts it from the classpath.
 *
 * Android target:
 *   The shared `.so` files go into `jniLibs/{abi}/` automatically via
 *   the Android Gradle Plugin, so we only need to copy the extracted
 *   artifacts to the right place.
 */

/**
 * Registers a Jar task that bundles the host-OS JNI shared library as a
 * classpath resource.
 *
 * The file is placed at `native/{os-arch}/libtdjson.{ext}` inside the JAR.
 */
fun TdlibProjectContext.configureJvmTarget(jniExtractTask: String) = with(project) {
    val jniLibsDir = file("libs/${jniLocalDir()}")

    // --- Pack JNI library into JVM resources --------------------------------
    tasks.register<Jar>("jvmJniJar") {
        group = "tdlib"
        description = "Packages host JNI library into JVM resources"
        archiveClassifier.set("jni-${hostOs.id}-${hostArch.id}")
        dependsOn(jniExtractTask)
        from(jniLibsDir) {
            include("lib/**")
            into("native/${hostOs.id}-${nativeArchId()}")
        }
    }

    // Wire JVM compile to depend on JNI extraction.
    tasks.matching { it.name == "compileKotlinJvm" }.configureEach {
        dependsOn(jniExtractTask)
    }
    tasks.matching { it.name == "jvmProcessResources" }.configureEach {
        dependsOn(jniExtractTask)
    }
}

/**
 * Wires Android extract tasks so that `.so` files are available at build time.
 */
fun TdlibProjectContext.configureAndroidTarget(abis: Map<String, String>) = with(project) {
    abis.forEach { (_, extractTask) ->
        tasks.matching { it.name.startsWith("compile") && it.name.contains("Android") }.configureEach {
            dependsOn(extractTask)
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun jniLocalDir(): String {
    val archId = when {
        hostOs == TdlibOS.Windows && hostArch == TdlibArch.X64 -> "x64"
        hostArch == TdlibArch.X64 -> "x86_64"
        else -> "arm64"
    }
    return "${hostOs.id}-$archId-jni"
}

private fun nativeArchId(): String = when {
    hostOs == TdlibOS.Windows && hostArch == TdlibArch.X64 -> "x64"
    hostArch == TdlibArch.X64 -> "x86_64"
    else -> "arm64"
}
