package io.github.cokit.transport.stdio

import kotlin.test.Test
import kotlin.test.assertEquals

class StdioCodexTransportTest {
    @Test
    fun defaultsToCodexAppServerStdioCommand() {
        val transport = StdioCodexTransport()

        assertEquals(listOf("codex", "app-server", "--stdio"), transport.command)
    }
}
