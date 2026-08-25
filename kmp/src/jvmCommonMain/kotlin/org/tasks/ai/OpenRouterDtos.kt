package org.tasks.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null,
    val temperature: Double = 0.0,
    @SerialName("max_tokens") val maxTokens: Int = 2048,
)

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ResponseFormat(
    val type: String = "json_schema",
    @SerialName("json_schema") val jsonSchema: JsonSchemaSpec,
)

@Serializable
data class JsonSchemaSpec(
    val name: String,
    val strict: Boolean = true,
    val schema: JsonObject,
)

@Serializable
data class ChatResponse(val choices: List<Choice> = emptyList()) {
    @Serializable
    data class Choice(
        val message: ChatMessage? = null,
        @SerialName("finish_reason") val finishReason: String? = null,
    )
}

@Serializable
data class OpenRouterError(val error: ErrorBody? = null) {
    @Serializable
    data class ErrorBody(val message: String? = null)
}

@Serializable
data class ModelsResponse(val data: List<ModelInfo> = emptyList())

@Serializable
data class ModelInfo(
    val id: String,
    val name: String? = null,
    @SerialName("supported_parameters") val supportedParameters: List<String> = emptyList(),
) {
    val isFree: Boolean get() = id.endsWith(":free")
    val supportsStructuredOutputs: Boolean get() = supportedParameters.contains("structured_outputs")
}
