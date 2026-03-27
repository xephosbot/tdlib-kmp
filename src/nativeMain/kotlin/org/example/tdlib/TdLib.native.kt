package org.example.tdlib

import tdjson.td_create_client_id

actual object TdLib {
    actual fun platform(): String = "native"

    actual fun load(): Boolean {
        return runCatching { td_create_client_id() }.isSuccess
    }
}

