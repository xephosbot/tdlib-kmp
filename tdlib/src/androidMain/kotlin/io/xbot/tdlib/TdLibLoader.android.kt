package io.xbot.tdlib

internal actual object TdLibLoader {
    actual fun load() {
        System.loadLibrary("tdjsonjava")
    }
}