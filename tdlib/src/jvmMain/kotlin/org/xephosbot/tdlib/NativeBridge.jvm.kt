package org.xephosbot.tdlib

/**
 * JVM implementation of [NativeBridge].
 *
 * Loads the JNI shared library (`libtdjni.so` / `libtdjni.dylib` / `tdjni.dll`)
 * via [TdLibLoader] which extracts it from the classpath JAR at runtime,
 * similar to how JetBrains/skiko loads `libskiko`.
 */
internal actual object NativeBridge {
    init {
        TdLibLoader.load()
    }

    actual fun createClientId(): Int = nativeCreateClientId()
    actual fun send(clientId: Int, request: String): Unit = nativeSend(clientId, request)
    actual fun receive(timeout: Double): String? = nativeReceive(timeout)
    actual fun execute(request: String): String? = nativeExecute(request)

    @JvmStatic
    private external fun nativeCreateClientId(): Int

    @JvmStatic
    private external fun nativeSend(clientId: Int, request: String)

    @JvmStatic
    private external fun nativeReceive(timeout: Double): String?

    @JvmStatic
    private external fun nativeExecute(request: String): String?
}
