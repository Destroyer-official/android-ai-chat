package com.example.androidimageapp

import android.content.Context
import android.content.SharedPreferences
import com.example.androidimageapp.fragments.MessageItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class ChatSession(
    val id: String,
    val title: String,
    val timestamp: Long,
    val messages: List<MessageItem>,
    val modelUsed: String
)

class ChatHistoryManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("chat_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveCurrentChat(messages: List<MessageItem>, modelId: String): String {
        if (messages.isEmpty()) return ""
        
        val chatId = "chat_${System.currentTimeMillis()}"
        val title = generateChatTitle(messages)
        
        val chatSession = ChatSession(
            id = chatId,
            title = title,
            timestamp = System.currentTimeMillis(),
            messages = messages,
            modelUsed = modelId
        )
        
        // Save individual chat
        prefs.edit().putString(chatId, gson.toJson(chatSession)).apply()
        
        // Update chat list
        val chatList = getChatList().toMutableList()
        chatList.add(0, chatId) // Add to beginning
        
        // Keep only last 50 chats
        if (chatList.size > 50) {
            val oldChatId = chatList.removeAt(chatList.size - 1)
            prefs.edit().remove(oldChatId).apply()
        }
        
        prefs.edit().putString("chat_list", gson.toJson(chatList)).apply()
        return chatId
    }
    
    fun getChatList(): List<String> {
        val json = prefs.getString("chat_list", "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
    }
    
    fun getChatSession(chatId: String): ChatSession? {
        val json = prefs.getString(chatId, null) ?: return null
        return gson.fromJson(json, ChatSession::class.java)
    }
    
    fun getAllChatSessions(): List<ChatSession> {
        return getChatList().mapNotNull { getChatSession(it) }
    }
    
    fun deleteChat(chatId: String) {
        prefs.edit().remove(chatId).apply()
        val chatList = getChatList().toMutableList()
        chatList.remove(chatId)
        prefs.edit().putString("chat_list", gson.toJson(chatList)).apply()
    }
    
    fun clearAllHistory() {
        val chatList = getChatList()
        val editor = prefs.edit()
        chatList.forEach { editor.remove(it) }
        editor.remove("chat_list")
        editor.apply()
    }
    
    private fun generateChatTitle(messages: List<MessageItem>): String {
        val firstUserMessage = messages.find { it.isUser }?.text?.replace("🖼️ ", "")?.trim()
        return when {
            firstUserMessage.isNullOrEmpty() -> "New Chat"
            firstUserMessage.length > 30 -> firstUserMessage.take(30) + "..."
            else -> firstUserMessage
        }
    }
}