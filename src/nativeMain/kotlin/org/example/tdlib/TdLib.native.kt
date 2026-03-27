package org.example.tdlib

actual object TdLib {
    actual fun platform(): String = "native"

    actual fun load(): Boolean {
        // Здесь будет реальный вызов через cinterop к tdjson/libtdjson.a.
        return false
    }
}

