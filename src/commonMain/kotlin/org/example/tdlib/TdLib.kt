package org.example.tdlib

expect object TdLib {
    fun platform(): String
    fun load(): Boolean
}

