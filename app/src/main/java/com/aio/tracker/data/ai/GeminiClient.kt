package com.aio.tracker.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

@Serializable data class GPart(val text: String? = null, val thought: Boolean? = null)
@Serializable data class GContent(val role: String? = null, val parts: List<GPart> = emptyList())
@Serializable data class GGenerationConfig(val temperature: Double? = null)
@Serializable
data class GRequest(
    val contents: List<GContent>,
    val systemInstruction: GContent? = null,
    val generationConfig: GGenerationConfig? = null,
)
@Serializable data class GCandidate(val content: GContent? = null, val finishReason: String? = null)
@Serializable data class GPromptFeedback(val blockReason: String? = null)
@Serializable data class GResponse(val candidates: List<GCandidate> = emptyList(), val promptFeedback: GPromptFeedback? = null)
@Serializable data class GError(val code: Int = 0, val message: String = "", val status: String = "")
@Serializable data class GErrorWrap(val error: GError? = null)
@Serializable
data class GModel(
    val name: String,
    val displayName: String? = null,
    val supportedGenerationMethods: List<String> = emptyList(),
)
@Serializable data class GModelList(val models: List<GModel> = emptyList(), val nextPageToken: String? = null)

class GeminiException(message: String) : Exception(message)

/**
 * Minimal client for the Google AI Studio (Gemini) REST API.
 * The API key is sent in the `x-goog-api-key` header (never in the URL, so it won't end up in logs).
 */
class GeminiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    suspend fun generate(apiKey: String, model: String, system: String, prompt: String): String {
        val body = GRequest(
            contents = listOf(GContent(role = "user", parts = listOf(GPart(prompt)))),
            systemInstruction = GContent(parts = listOf(GPart(system))),
            generationConfig = GGenerationConfig(temperature = 0.6),
        )
        val modelId = model.trim().removePrefix("models/").ifBlank { "gemini-2.5-flash" }
        val raw = request("POST", "$BASE/models/$modelId:generateContent", apiKey, json.encodeToString(GRequest.serializer(), body))
        val response = json.decodeFromString(GResponse.serializer(), raw)
        response.promptFeedback?.blockReason?.let { throw GeminiException("The request was blocked ($it).") }
        val text = response.candidates.firstOrNull()?.content?.parts
            ?.filter { it.thought != true }
            ?.mapNotNull { it.text }
            ?.joinToString("")
            ?.trim()
        if (text.isNullOrEmpty()) {
            val reason = response.candidates.firstOrNull()?.finishReason
            throw GeminiException("The model returned an empty answer" + (reason?.let { " ($it)" } ?: "") + ".")
        }
        return text
    }

    /** Lists models usable with generateContent. Doubles as an API key check. */
    suspend fun listModels(apiKey: String): List<String> {
        val result = mutableListOf<String>()
        var token: String? = null
        var pages = 0
        do {
            val url = "$BASE/models?pageSize=200" + (token?.let { "&pageToken=$it" } ?: "")
            val list = json.decodeFromString(GModelList.serializer(), request("GET", url, apiKey, null))
            result += list.models
                .filter { "generateContent" in it.supportedGenerationMethods }
                .map { it.name.removePrefix("models/") }
                .filter { it.startsWith("gemini") || it.startsWith("gemma") }
            token = list.nextPageToken?.takeIf { it.isNotBlank() }
            pages++
        } while (token != null && pages < 5)
        return result.distinct().sortedWith(compareBy({ !it.contains("flash") }, { it }))
    }

    private suspend fun request(method: String, url: String, apiKey: String, body: String?): String =
        withContext(Dispatchers.IO) {
            val conn = (URL(url).openConnection() as HttpURLConnection)
            try {
                conn.requestMethod = method
                conn.connectTimeout = 20_000
                conn.readTimeout = 180_000
                conn.setRequestProperty("x-goog-api-key", apiKey)
                conn.setRequestProperty("Accept", "application/json")
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code !in 200..299) throw GeminiException(friendlyError(code, text))
                text
            } catch (e: GeminiException) {
                throw e
            } catch (e: UnknownHostException) {
                throw GeminiException("No internet connection.")
            } catch (e: SocketTimeoutException) {
                throw GeminiException("The request timed out. Please try again.")
            } catch (e: IOException) {
                throw GeminiException("Network error: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                conn.disconnect()
            }
        }

    private fun friendlyError(code: Int, body: String): String {
        val apiMessage = runCatching { json.decodeFromString(GErrorWrap.serializer(), body).error?.message }
            .getOrNull()?.takeIf { it.isNotBlank() }
        return when (code) {
            400 -> apiMessage ?: "Bad request."
            401, 403 -> "The API key was rejected. Check it in Settings. " + (apiMessage ?: "")
            404 -> "Model not found. Pick another model in Settings. " + (apiMessage ?: "")
            429 -> "Rate limit or quota reached. Wait a moment and try again."
            in 500..599 -> "Gemini is having trouble right now (HTTP $code). Try again shortly."
            else -> apiMessage ?: "Request failed (HTTP $code)."
        }.trim()
    }

    private companion object {
        const val BASE = "https://generativelanguage.googleapis.com/v1beta"
    }
}
