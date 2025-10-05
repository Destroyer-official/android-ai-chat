package com.example.androidimageapp

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response

interface ApiService {
    @POST("chat/completions")
    suspend fun getChatCompletion(@Body request: ChatRequest): Response<ChatResponse>
}

data class ChatRequest(
    val model: String = "x-ai/grok-4-fast:free",
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: List<Content>
)

data class Content(
    val type: String,
    val text: String? = null,
    val image_url: ImageUrl? = null
)

data class ImageUrl(
    val url: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: MessageResponse
)

data class MessageResponse(
    val role: String,
    val content: String
)
