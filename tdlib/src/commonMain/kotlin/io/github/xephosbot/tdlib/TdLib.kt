package io.github.xephosbot.tdlib

/**
 * Main entry point for the TDLib JSON Client API.
 *
 * All calls are delegated to [NativeBridge], which is implemented per-platform:
 * - Kotlin/Native: cinterop bindings to `td_json_client.h`
 * - JVM: JNI via `libtdjson`
 * - Android: JNI via `libtdjsonjava`
 */
object TdLib {
    /** Creates a new TDLib client and returns its unique identifier. */
    fun createClientId(): Int = NativeBridge.createClientId()

    /** Sends a JSON-encoded request to the given client. */
    fun send(clientId: Int, request: String): Unit = NativeBridge.send(clientId, request)

    /** Receives a JSON-encoded response/update from TDLib (blocks up to [timeout] seconds). */
    fun receive(timeout: Double): String? = NativeBridge.receive(timeout)

    /** Synchronously executes a TDLib request and returns the JSON result. */
    fun execute(request: String): String? = NativeBridge.execute(request)
}
