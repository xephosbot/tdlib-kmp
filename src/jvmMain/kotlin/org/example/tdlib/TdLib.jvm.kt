package org.example.tdlib

actual object TdLib {
    private val loaded: Boolean by lazy {
        runCatching { System.loadLibrary("tdjni") }.isSuccess
    }

    actual fun platform(): String = "jvm"

    actual fun load(): Boolean = loaded
}

