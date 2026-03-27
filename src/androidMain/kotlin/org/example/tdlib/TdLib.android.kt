package org.example.tdlib

actual object TdLib {
    private val loaded: Boolean by lazy {
        runCatching { System.loadLibrary("tdjsonjava") }.isSuccess
    }

    actual fun platform(): String = "android"

    actual fun load(): Boolean = loaded
}

