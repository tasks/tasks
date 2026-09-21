package org.tasks.mcp

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI

class McpHttpServerTest : McpGraphTestCase() {

    private val token = "correct-horse-battery-staple"
    private val port = freePort()
    private lateinit var server: McpHttpServer

    @Before
    fun setUp() {
        server = McpHttpServer(
            port = port,
            token = { token },
            permissions = ToolPermissions.of(AccessMode.ReadOnly),
            api = api,
            log = ActivityLog(),
            onStateChange = { _, _ -> },
        )
        server.start()
    }

    @After
    fun tearDown() {
        if (server.isRunning) server.stop()
    }

    @Test
    fun healthNeedsNoToken() {
        val (status, body) = get(McpHttpServer.baseUrl(port) + McpHttpServer.HEALTH_PATH)

        assertEquals(200, status)
        assertTrue(body, body.contains("\"status\":\"ok\""))
    }

    @Test
    fun theMcpEndpointRefusesARequestWithNoToken() {
        val (status, body) = post(McpHttpServer.endpoint(port), token = null)

        assertEquals(401, status)
        assertTrue(body, body.contains("unauthorized"))
    }

    @Test
    fun theMcpEndpointRefusesAWrongToken() {
        assertEquals(401, post(McpHttpServer.endpoint(port), token = "not-the-token").first)
    }

    @Test
    fun aTokenThatDiffersOnlyInLengthIsStillRefused() {
        assertEquals(401, post(McpHttpServer.endpoint(port), token = token.dropLast(1)).first)
        assertEquals(401, post(McpHttpServer.endpoint(port), token = token + "x").first)
    }

    @Test
    fun theRightTokenGetsPastTheBearerCheck() {
        assertNotEquals(401, post(McpHttpServer.endpoint(port), token = token).first)
    }

    @Test
    fun refusalAdvertisesBearerAuth() {
        val connection = open(McpHttpServer.endpoint(port), method = "POST", token = null)
        assertEquals(401, connection.responseCode)
        assertTrue(
            connection.getHeaderField("WWW-Authenticate").orEmpty().startsWith("Bearer "),
        )
    }

    @Test
    fun itDoesNotListenOnAnythingButLoopback() {
        val lan = InetAddress.getLocalHost()
        if (lan.isLoopbackAddress) return

        val reachable = runCatching {
            java.net.Socket().use { it.connect(java.net.InetSocketAddress(lan, port), 500) }
            true
        }.getOrDefault(false)

        assertEquals(false, reachable)
    }

    private fun get(url: String): Pair<Int, String> {
        val connection = open(url, method = "GET", token = null)
        return connection.responseCode to connection.bodyText()
    }

    private fun post(url: String, token: String?): Pair<Int, String> {
        val connection = open(url, method = "POST", token = token)
        connection.doOutput = true
        connection.outputStream.use {
            it.write("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}""".toByteArray())
        }
        return connection.responseCode to connection.bodyText()
    }

    private fun open(url: String, method: String, token: String?): HttpURLConnection =
        (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json, text/event-stream")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }

    private fun HttpURLConnection.bodyText(): String =
        runCatching { inputStream.bufferedReader().readText() }
            .recoverCatching { errorStream?.bufferedReader()?.readText().orEmpty() }
            .getOrDefault("")

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }
}
