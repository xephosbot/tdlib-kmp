import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Central project context for tdlib-kmp build configuration.
 *
 * Analogous to `SkikoProjectContext` in JetBrains/skiko.
 * Holds references to the project, Kotlin extension, and TDLib dependencies
 * so that configuration helpers can be called as extension functions.
 */
class TdlibProjectContext(
    val project: Project,
    val kotlin: KotlinMultiplatformExtension,
    val tdlibDeps: TdlibDependencies,
)
