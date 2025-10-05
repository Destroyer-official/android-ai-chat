package com.example.androidimageapp.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "ai_image_app_prefs"
        private const val KEY_CHAT_BACKGROUND = "chat_background"
        private const val KEY_CUSTOM_CHAT_BACKGROUND = "custom_chat_background"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SELECTED_MODEL = "selected_model"
        private const val KEY_CUSTOM_MODELS = "custom_models"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_THEME_MODE = "theme_mode"
    }
    
    fun setChatBackground(background: String) {
        sharedPreferences.edit()
            .putString(KEY_CHAT_BACKGROUND, background)
            .remove(KEY_CUSTOM_CHAT_BACKGROUND) // Clear custom when setting preset
            .apply()
    }
    
    fun getChatBackground(): String {
        return sharedPreferences.getString(KEY_CHAT_BACKGROUND, "default") ?: "default"
    }
    
    fun setCustomChatBackground(uri: String) {
        sharedPreferences.edit()
            .putString(KEY_CUSTOM_CHAT_BACKGROUND, uri)
            .putString(KEY_CHAT_BACKGROUND, "custom")
            .apply()
    }
    
    fun getCustomChatBackground(): String? {
        return sharedPreferences.getString(KEY_CUSTOM_CHAT_BACKGROUND, null)
    }
    
    fun setApiKey(apiKey: String) {
        sharedPreferences.edit()
            .putString(KEY_API_KEY, apiKey)
            .apply()
    }
    
    fun getApiKey(): String? {
        return sharedPreferences.getString(KEY_API_KEY, null)
    }
    
    fun setSelectedModel(model: String) {
        sharedPreferences.edit()
            .putString(KEY_SELECTED_MODEL, model)
            .apply()
    }
    
    fun getSelectedModel(): String {
        return sharedPreferences.getString(KEY_SELECTED_MODEL, "gpt-4o-mini") ?: "gpt-4o-mini"
    }
    
    fun setCustomModels(models: Set<String>) {
        sharedPreferences.edit()
            .putStringSet(KEY_CUSTOM_MODELS, models)
            .apply()
    }
    
    fun getCustomModels(): Set<String> {
        return sharedPreferences.getStringSet(KEY_CUSTOM_MODELS, emptySet()) ?: emptySet()
    }
    
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    fun setFirstLaunchCompleted() {
        sharedPreferences.edit()
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
    }
    
    fun setThemeMode(mode: String) {
        sharedPreferences.edit()
            .putString(KEY_THEME_MODE, mode)
            .apply()
    }
    
    fun getThemeMode(): String {
        return sharedPreferences.getString(KEY_THEME_MODE, "system") ?: "system"
    }
    
    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }
}