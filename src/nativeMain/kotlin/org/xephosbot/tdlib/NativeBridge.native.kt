package org.xephosbot.tdlib

import kotlinx.cinterop.toKString
import tdjson.td_create_client_id
import tdjson.td_execute
import tdjson.td_receive
import tdjson.td_send

internal actual object NativeBridge {
    actual fun createClientId(): Int = td_create_client_id()

    actual fun send(clientId: Int, request: String) {
        td_send(clientId, request)
    }

    actual fun receive(timeout: Double): String? {
        val result = td_receive(timeout)
        return result?.toKString()
    }

    actual fun execute(request: String): String? {
        val result = td_execute(request)
        return result?.toKString()
    }
}
