package io.xbot.tdlib

internal actual object TdLibLoader {
    @Volatile private var loaded = false

    actual fun load() {
        if (loaded) return
        loadSynchronous()
    }

    private fun loadSynchronous() {
        // Double-checked locking
        if (loaded) return
        System.loadLibrary("tdjsonjava")
    }
}