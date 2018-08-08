package com.lieluobo.op.requests

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import junit.framework.TestCase
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.util.*

class RequestsTest : TestCase() {
    private val address = InetSocketAddress(0)
    private val server = HttpServer.create(address, 0)


    private fun response(exchange: HttpExchange, body: ByteArray) {
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, body.size.toLong())
        exchange.responseBody.write(body)
        exchange.close()
    }

    @BeforeClass
    override fun setUp() {
        server.createContext("/params") { exchange ->
            val resp = exchange.requestURI.query.split("&")
                    .map { it.split("=") }
                    .firstOrNull { it.component1() == "test" }
                    ?.component2()
                    ?: ""
            response(exchange, resp.toByteArray())
        }

        server.createContext("/headers") { exchange ->
            val resp = exchange.requestHeaders["test"]?.get(0) ?: ""
            response(exchange, resp.toByteArray())
        }

        server.createContext("/json") {
            val resp = it.requestBody.readBytes()
            response(it, resp)
        }

        server.createContext("/form") {
            val body = it.requestBody.readBytes()
        }
        server.start()
    }

    @AfterClass
    override fun tearDown() {
        server.stop(0)
    }

    @Test
    fun testParams() {
        val parameter = Random().nextInt(100).toString()
        Requests.url("http://127.0.0.1:${server.address.port}/params").params(mapOf("test" to parameter)).get {
            assertNotNull(it.body())
            assertEquals(parameter, String(it.body()!!.byteStream().readBytes()))
        }
    }

    @Test
    fun testHeaders() {
        val value = Random().nextInt().toString()
        Requests.url("http://127.0.0.1:${server.address.port}/headers").headers(mapOf("test" to value)).get {
            assertNotNull(it.body())
            assertEquals(value, String(it.body()!!.byteStream().readBytes()))
        }
    }

    @Test
    fun testJson() {
        val json = """{"value": "${Random().nextInt()}"}"""
        Requests.url("http://127.0.0.1:${server.address.port}/json").json(json).post {
            assertNotNull(it.body())
            assertEquals(json, String(it.body()!!.byteStream().readBytes()))
        }
    }

}