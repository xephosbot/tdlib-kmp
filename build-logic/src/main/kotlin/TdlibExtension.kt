import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class TdlibExtension @Inject constructor(objects: ObjectFactory) {
    val version: Property<String> = objects.property(String::class.java)

    internal var jvmEnabled = false
    internal var androidEnabled = false
    internal val nativeTargets = mutableListOf<TdlibTarget>()

    fun jvm() { jvmEnabled = true }
    fun android() { androidEnabled = true }

    fun native(action: Action<NativeTargetDsl>) {
        val dsl = NativeTargetDsl()
        action.execute(dsl)
        nativeTargets.addAll(dsl.targets)
    }
}

class NativeTargetDsl {
    internal val targets = mutableListOf<TdlibTarget>()
    fun iosArm64() { targets.add(TdlibTarget.IOS_ARM64) }
    fun iosSimulatorArm64() { targets.add(TdlibTarget.IOS_SIM_ARM64) }
    fun macosArm64() { targets.add(TdlibTarget.MACOS_ARM64) }
    fun macosX64() { targets.add(TdlibTarget.MACOS_X64) }
    fun linuxX64() { targets.add(TdlibTarget.LINUX_X64) }
    fun linuxArm64() { targets.add(TdlibTarget.LINUX_ARM64) }
}
