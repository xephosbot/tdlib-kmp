import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.tasks.Copy
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
    val resourceLayout = "native/${hostOs.id}-${nativeArchId()}"
    val jniResourcesDir = layout.buildDirectory.dir("jvmJniResources")

    // Copy JNI library into the expected classpath resource layout:
    //   build/jvmJniResources/native/{os-arch}/lib/libtdjson.{ext}
    // This matches the path TdLibLoader looks up at runtime.
    val stageJniTask = tasks.register<Copy>("stageJvmJniResources") {
        group = "tdlib"
        description = "Stage JNI library in classpath resource layout for run/test"
        dependsOn(jniExtractTask)
        from(jniLibsDir) {
            include("lib/**")
            into(resourceLayout)
        }
        into(jniResourcesDir)
    }

    // Add staged resources so both run/test and published JAR include the library.
    kotlin.sourceSets.getByName("jvmMain") {
        resources.srcDir(jniResourcesDir)
    }

    // --- Pack JNI library into standalone classifier JAR --------------------------------
    tasks.register<Jar>("jvmJniJar") {
        group = "tdlib"
        description = "Packages host JNI library into JVM resources"
        archiveClassifier.set("jni-${hostOs.id}-${hostArch.id}")
        dependsOn(stageJniTask)
        from(jniResourcesDir)
    }

    // Wire JVM compile and resource processing to depend on staging.
    tasks.matching { it.name == "compileKotlinJvm" }.configureEach {
        dependsOn(stageJniTask)
    }
    tasks.matching { it.name == "jvmProcessResources" }.configureEach {
        dependsOn(stageJniTask)
    }
}

/**
 * Configures Android target: copies `.so` files into the proper `{abi}/` directory
 * layout expected by AGP and registers the folder as a jniLibs source directory so
 * that native libraries are packaged into the AAR.
 */
fun TdlibProjectContext.configureAndroidTarget(abis: Map<String, String>) = with(project) {
    val jniLibsBaseDir = layout.buildDirectory.dir("androidJniLibs")

    // Per-ABI copy tasks: libs/android-{abi}/*.so → build/androidJniLibs/{abi}/*.so
    val copyTasks = abis.map { (abi, extractTask) ->
        val suffix = abi.split("-").joinToString("") { it.replaceFirstChar(Char::titlecase) }
        val taskName = "copyAndroidJniLibs$suffix"
        tasks.register<Copy>(taskName) {
            group = "tdlib"
            description = "Copy $abi .so libs for Android AAR"
            dependsOn(extractTask)
            from(file("libs/android-$abi")) {
                include("*.so")
            }
            into(jniLibsBaseDir.map { it.dir(abi) })
        }
        taskName
    }

    // Register assembled jniLibs directory with AGP via the Variant API.
    extensions.getByType(KotlinMultiplatformAndroidComponentsExtension::class.java)
        .onVariants { variant ->
            variant.sources.jniLibs?.addStaticSourceDirectory("build/androidJniLibs")
        }

    // Wire copy tasks before AGP merges JNI libraries.
    tasks.matching {
        (it.name.contains("JniLib", ignoreCase = true) && !it.name.startsWith("copyAndroidJniLibs")) ||
            (it.name.startsWith("compile") && it.name.contains("Android"))
    }.configureEach {
        copyTasks.forEach { task -> dependsOn(task) }
    }
}

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
