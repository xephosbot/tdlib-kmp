package io.github.xephosbot.tdlib

/**
 * Platform-specific bridge to TDLib C functions.
 *
 * Each platform provides an `actual` implementation:
 * - **nativeMain** → cinterop (`td_create_client_id`, `td_send`, `td_receive`, `td_execute`)
 * - **jvmMain** → JNI external functions loaded from `libtdjson`
 * - **androidMain** → JNI external functions loaded from `libtdjsonjava`
 */
internal expect object NativeBridge {
    fun createClientId(): Int
    fun send(clientId: Int, request: String)
    fun receive(timeout: Double): String?
    fun execute(request: String): String?
}
