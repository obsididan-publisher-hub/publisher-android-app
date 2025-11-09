package com.example.app

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*

class NetworkTest {

    @Test /*Тестируем доступность тестовых API*/
    fun `test that test APIs are reachable`() {
        val testUrls = listOf(
            "https://jsonplaceholder.typicode.com/posts/1",
            "https://httpbin.org/json",
            "https://example.com"
        )

        testUrls.forEach { urlString ->
            try {
                val url = java.net.URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                assertTrue(
                    "URL $urlString should be accessible (response: $responseCode)",
                    responseCode in 200..399
                )

                connection.disconnect()
            } catch (e: Exception) {
                fail("Failed to connect to $urlString: ${e.message}")
            }
        }
    }

    @Test  /*Реальный запрос к тестовому API*/
    fun `test performGetRequest with real API`() = runBlocking {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

        try {
            val responseText: String = client.get("https://jsonplaceholder.typicode.com/posts/1").bodyAsText()

            assertTrue(responseText.isNotEmpty())
            assertTrue(responseText.contains("userId"))
            assertTrue(responseText.contains("id"))
            assertTrue(responseText.contains("title"))

        } finally {
            client.close()
        }
    }
}