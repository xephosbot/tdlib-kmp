package org.xephosbot.tdlib

/**
 * Android implementation of [NativeBridge].
 *
 * On Android the shared library (`libtdjni.so`) is bundled inside the APK
 * under `lib/{abi}/` and loaded via [System.loadLibrary].
 */
internal actual object NativeBridge {
    init {
        System.loadLibrary("tdjni")
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
