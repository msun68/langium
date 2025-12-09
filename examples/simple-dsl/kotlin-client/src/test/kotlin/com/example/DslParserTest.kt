package com.example

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class DslParserTest {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val baseUrl = "http://localhost:3000"

    @Test
    fun `test health endpoint`() {
        val request = Request.Builder()
            .url("$baseUrl/health")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful, "Health check should succeed")
            val body = response.body?.string()
            assertTrue(body?.contains("ok") == true, "Health check should return ok status")
        }
    }

    @Test
    fun `test parse simple greeting`() {
        val dslContent = mapOf("content" to "Hello World!")
        val json = gson.toJson(dslContent)

        val request = Request.Builder()
            .url("$baseUrl/parse")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful, "Parse request should succeed")
            val body = response.body?.string()
            
            @Suppress("UNCHECKED_CAST")
            val result = gson.fromJson(body, Map::class.java) as Map<String, Any>
            
            assertTrue(result["success"] == true, "Parse should be successful")
            
            @Suppress("UNCHECKED_CAST")
            val ast = result["ast"] as Map<String, Any>
            assertEquals("World", ast["name"], "AST should contain name 'World'")
        }
    }

    @Test
    fun `test parse greeting with location`() {
        val dslContent = mapOf("content" to "Hello World from Earth!")
        val json = gson.toJson(dslContent)

        val request = Request.Builder()
            .url("$baseUrl/parse")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful, "Parse request should succeed")
            val body = response.body?.string()
            
            @Suppress("UNCHECKED_CAST")
            val result = gson.fromJson(body, Map::class.java) as Map<String, Any>
            
            assertTrue(result["success"] == true, "Parse should be successful")
            
            @Suppress("UNCHECKED_CAST")
            val ast = result["ast"] as Map<String, Any>
            assertEquals("World", ast["name"], "AST should contain name 'World'")
            assertEquals("Earth", ast["location"], "AST should contain location 'Earth'")
        }
    }

    @Test
    fun `test parse invalid DSL content`() {
        val dslContent = mapOf("content" to "Invalid Content Here")
        val json = gson.toJson(dslContent)

        val request = Request.Builder()
            .url("$baseUrl/parse")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            // This should fail with validation error or parse error
            val body = response.body?.string()
            
            @Suppress("UNCHECKED_CAST")
            val result = gson.fromJson(body, Map::class.java) as Map<String, Any>
            
            assertTrue(result.containsKey("error"), "Invalid DSL should return an error")
        }
    }
}
