package io.xbot.tdlib.client

import kotlinx.coroutines.flow.Flow

/**
 * [WIP] Public coroutine-first contract for TDLib client.
 */
interface TdClient : AutoCloseable {
	val updates: Flow<Any>

	suspend fun create(): TdClient

	suspend fun send(
		function: Any
	): Any

	override fun close()
}
