package io.xbot.tdlib

import io.xbot.tdlib.cinterop.td_create_client_id
import io.xbot.tdlib.cinterop.td_execute
import io.xbot.tdlib.cinterop.td_receive
import io.xbot.tdlib.cinterop.td_send
import kotlinx.cinterop.toKString

internal actual object NativeBridge {
    actual fun createClientId(): Int = td_create_client_id()
    actual fun send(clientId: Int, request: String) { td_send(clientId, request) }
    actual fun receive(timeout: Double): String? {
        val result = td_receive(timeout)
        return result?.toKString()
    }
    actual fun execute(request: String): String? {
        val result = td_execute(request)
        return result?.toKString()
    }
}
