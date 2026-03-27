package org.xephosbot.tdlib

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TdLibTest {
    @Test
    fun testCreateClientId() {
        val id = TdLib.createClientId()
        assertTrue(id > 0, "Client ID must be positive, got $id")
    }

    @Test
    fun testExecute() {
        val result = TdLib.execute("""{"@type":"getOption","name":"version"}""")
        assertNotNull(result, "execute() must return a non-null result")
        assertTrue(result.contains("version"), "Result must contain 'version': $result")
    }
}
