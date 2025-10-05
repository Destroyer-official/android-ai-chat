package com.example.androidimageapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawer: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var tvToolbarTitle: TextView
    private lateinit var btnMenu: ImageButton
    private lateinit var btnNewChat: ImageButton
    
    private val prefs: SharedPreferences by lazy { 
        getSharedPreferences("app_settings", Context.MODE_PRIVATE) 
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupToolbar()
        setupNavigationDrawer()
        setupClickListeners()
        loadApiKey()
        
        // Load ChatFragment by default
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, com.example.androidimageapp.fragments.ChatFragment())
                .commit()
        }
        
        // Check if we need to load a specific chat
        val loadChatId = intent.getStringExtra("load_chat_id")
        if (loadChatId != null) {
            loadChatFromHistory(loadChatId)
        }
    }
    
    private fun initializeViews() {
        drawer = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle)
        btnMenu = findViewById(R.id.btnMenu)
        btnNewChat = findViewById(R.id.btnNewChat)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }
    
    private fun setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.setCheckedItem(R.id.nav_chat)
        updateNavigationMenu()
    }
    
    private fun updateNavigationMenu() {
        val menu = navigationView.menu
        
        // Get list of custom model names to identify which items to remove
        val allModels = getCustomModels()
        val userCustomModels = allModels.filter { !it.isDefault }
        val customModelNames = userCustomModels.map { it.name }.toSet()
        
        // Remove existing custom models by name (safer than by ID)
        val itemsToRemove = mutableListOf<MenuItem>()
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (customModelNames.contains(item.title.toString())) {
                itemsToRemove.add(item)
            }
        }
        itemsToRemove.forEach { menu.removeItem(it.itemId) }
        
        // Add user custom models (non-defaults) to the navigation
        userCustomModels.forEachIndexed { index, model ->
            menu.add(R.id.ai_models_group, 10000 + index, 17 + index, model.name)?.apply {
                icon = getDrawable(R.drawable.ic_ai_assistant)
            }
        }
    }
    
    private fun setupClickListeners() {
        btnMenu.setOnClickListener {
            drawer.openDrawer(GravityCompat.START)
        }
        
        btnNewChat.setOnClickListener {
            val chatFragment = getChatFragment()
            if (chatFragment != null) {
                chatFragment.startNewChat()
                // Reset toolbar title to default when starting new chat
                tvToolbarTitle.text = "AI Assistant"
                showToast("Started new chat")
            } else {
                showToast("Chat not ready")
            }
        }
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_chat -> {
                tvToolbarTitle.text = "AI Assistant"
            }
            R.id.nav_history -> {
                tvToolbarTitle.text = "Chat History"
                showChatHistory()
            }
            // Image + Chat Models
            R.id.nav_model_1 -> selectModel("mistralai/mistral-small-3.2-24b-instruct:free", "Mistral Small 3.2 24B")
            R.id.nav_model_2 -> selectModel("moonshotai/kimi-vl-a3b-thinking:free", "Kimi VL A3B Thinking")
            R.id.nav_model_3 -> selectModel("meta-llama/llama-4-maverick:free", "Llama 4 Maverick")
            R.id.nav_model_4 -> selectModel("meta-llama/llama-4-scout:free", "Llama 4 Scout")
            R.id.nav_model_5 -> selectModel("qwen/qwen2.5-vl-32b-instruct:free", "Qwen2.5 VL 32B")
            R.id.nav_model_6 -> selectModel("mistralai/mistral-small-3.1-24b-instruct:free", "Mistral Small 3.1 24B")
            R.id.nav_model_7 -> selectModel("google/gemma-3-4b-it:free", "Gemma 3 4B IT")
            R.id.nav_model_8 -> selectModel("google/gemma-3-12b-it:free", "Gemma 3 12B IT")
            R.id.nav_model_9 -> selectModel("google/gemma-3-27b-it:free", "Gemma 3 27B IT")
            R.id.nav_model_10 -> selectModel("qwen/qwen2.5-vl-72b-instruct:free", "Qwen2.5 VL 72B")
            R.id.nav_model_11 -> selectModel("google/gemini-2.0-flash-exp:free", "Gemini 2.0 Flash")
            // Chat Only Models
            R.id.nav_model_12 -> selectModel("alibaba/tongyi-deepresearch-30b-a3b:free", "Tongyi DeepResearch 30B")
            R.id.nav_model_13 -> selectModel("meituan/longcat-flash-chat:free", "Longcat Flash Chat")
            R.id.nav_model_14 -> selectModel("nvidia/nemotron-nano-9b-v2:free", "Nemotron Nano 9B V2")
            R.id.nav_model_15 -> selectModel("deepseek/deepseek-chat-v3.1:free", "DeepSeek Chat V3.1")
            R.id.nav_model_16 -> selectModel("openai/gpt-oss-20b:free", "GPT OSS 20B")
            R.id.nav_model_17 -> selectModel("z-ai/glm-4.5-air:free", "GLM 4.5 Air")
            R.id.nav_model_18 -> selectModel("qwen/qwen3-coder:free", "Qwen3 Coder")
            R.id.nav_model_19 -> selectModel("moonshotai/kimi-k2:free", "Kimi K2")
            R.id.nav_model_20 -> selectModel("cognitivecomputations/dolphin-mistral-24b-venice-edition:free", "Dolphin Mistral 24B Venice")
            R.id.nav_model_21 -> selectModel("google/gemma-3n-e2b-it:free", "Gemma 3N E2B IT")
            R.id.nav_model_22 -> selectModel("tencent/hunyuan-a13b-instruct:free", "Hunyuan A13B")
            R.id.nav_model_23 -> selectModel("tngtech/deepseek-r1t2-chimera:free", "DeepSeek R1T2 Chimera")
            R.id.nav_model_24 -> selectModel("moonshotai/kimi-dev-72b:free", "Kimi Dev 72B")
            R.id.nav_model_25 -> selectModel("deepseek/deepseek-r1-0528-qwen3-8b:free", "DeepSeek R1 Qwen3 8B")
            R.id.nav_model_26 -> selectModel("deepseek/deepseek-r1-0528:free", "DeepSeek R1 0528")
            R.id.nav_model_27 -> selectModel("mistralai/devstral-small-2505:free", "Devstral Small 2505")
            R.id.nav_model_28 -> selectModel("google/gemma-3n-e4b-it:free", "Gemma 3N E4B IT")
            R.id.nav_model_29 -> selectModel("meta-llama/llama-3.3-8b-instruct:free", "Llama 3.3 8B")
            R.id.nav_model_30 -> selectModel("qwen/qwen3-4b:free", "Qwen3 4B")
            R.id.nav_model_31 -> selectModel("qwen/qwen3-30b-a3b:free", "Qwen3 30B A3B")
            R.id.nav_model_32 -> selectModel("qwen/qwen3-8b:free", "Qwen3 8B")
            R.id.nav_model_33 -> selectModel("qwen/qwen3-14b:free", "Qwen3 14B")
            R.id.nav_model_34 -> selectModel("qwen/qwen3-235b-a22b:free", "Qwen3 235B A22B")
            R.id.nav_model_35 -> selectModel("tngtech/deepseek-r1t-chimera:free", "DeepSeek R1T Chimera")
            R.id.nav_model_36 -> selectModel("microsoft/mai-ds-r1:free", "MAI DS R1")
            R.id.nav_model_37 -> selectModel("shisa-ai/shisa-v2-llama3.3-70b:free", "Shisa V2 Llama3.3 70B")
            R.id.nav_model_38 -> selectModel("arliai/qwq-32b-arliai-rpr-v1:free", "QwQ 32B ArliAI RPR V1")
            R.id.nav_model_39 -> selectModel("agentica-org/deepcoder-14b-preview:free", "DeepCoder 14B Preview")
            R.id.nav_api_key -> {
                showApiKeyDialog()
            }
            R.id.nav_models -> {
                showModelsDialog()
            }
            R.id.nav_settings -> {
                showSettingsDialog()
            }
            R.id.nav_about -> {
                showAboutDialog()
            }
            else -> {
                // Check if it's a custom model (ID >= 10000)
                if (item.itemId >= 10000) {
                    val customModels = getCustomModels()
                    val userCustomModels = customModels.filter { !it.isDefault }
                    val modelIndex = item.itemId - 10000
                    if (modelIndex < userCustomModels.size) {
                        val selectedModel = userCustomModels[modelIndex]
                        selectModel(selectedModel.id, selectedModel.name)
                    }
                }
            }
        }
        
        drawer.closeDrawer(GravityCompat.START)
        return true
    }
    
    private fun selectModel(modelId: String, modelName: String) {
        prefs.edit().putString("selected_model", modelId).apply()
        tvToolbarTitle.text = modelName
        getChatFragment()?.changeModel(modelId)
    }
    
    private fun showApiKeyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_api_key, null)
        val etApiKey = dialogView.findViewById<EditText>(R.id.etApiKey)
        
        etApiKey.setText(apiKey ?: "")
        
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("API Key Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newApiKey = etApiKey.text.toString().trim()
                if (newApiKey.isNotEmpty()) {
                    apiKey = newApiKey
                    saveApiKey(newApiKey)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showChatHistory() {
        val intent = Intent(this, ChatHistoryActivity::class.java)
        startActivity(intent)
    }
    
    private fun showModelsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manage_models, null)
        val rvModels = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvModels)
        val etModelName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etModelName)
        val etModelId = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etModelId)
        val btnAddModel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddModel)
        
        // Only show user custom models (not default models)
        val userCustomModels = getCustomModels().filter { !it.isDefault }.toMutableList()
        android.util.Log.d("MainActivity", "Dialog: Custom models only = ${userCustomModels.size}")
        lateinit var adapter: ModelManagerAdapter
        
        adapter = ModelManagerAdapter(userCustomModels) { model ->
            // Remove user custom model
            userCustomModels.remove(model)
            saveCustomModels(getDefaultModels() + userCustomModels) // Save all models
            
            // Force refresh the adapter with fresh data
            userCustomModels.clear()
            userCustomModels.addAll(getCustomModels().filter { !it.isDefault })
            adapter.notifyDataSetChanged()
            updateNavigationMenu() // Refresh navigation menu
        }
        
        rvModels.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvModels.adapter = adapter
        
        // Apply layout animation programmatically as well
        val layoutAnimationController = android.view.animation.AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
        rvModels.layoutAnimation = layoutAnimationController
        
        // Trigger the animation when data changes
        rvModels.scheduleLayoutAnimation()
        
        btnAddModel.setOnClickListener {
            val name = etModelName.text.toString().trim()
            val id = etModelId.text.toString().trim()
            
            if (name.isNotEmpty() && id.isNotEmpty()) {
                // Check if model already exists (including default models)
                val allExistingModels = getCustomModels()
                val existingModel = allExistingModels.find { it.id == id }
                if (existingModel != null) {
                    showToast("Model with this ID already exists")
                    return@setOnClickListener
                }
                
                val newModel = CustomModel(name, id, false) // User-added models are not default
                userCustomModels.add(newModel)
                saveCustomModels(getDefaultModels() + userCustomModels) // Save all models
                
                // Force refresh the adapter with fresh data
                userCustomModels.clear()
                userCustomModels.addAll(getCustomModels().filter { !it.isDefault })
                adapter.notifyDataSetChanged()
                rvModels.scheduleLayoutAnimation() // Trigger animation for new items
                
                etModelName.text?.clear()
                etModelId.text?.clear()
                updateNavigationMenu() // Refresh navigation menu
                showToast("Model configured successfully")
            } else {
                showToast("Please enter both model name and ID")
            }
        }
        
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Model Management")
            .setMessage("Configure and manage AI language models for your conversations.")
            .setView(dialogView)
            .setPositiveButton("Done", null)
            .show()
    }
    
    private fun showSettingsDialog() {
        // Replace current fragment with settings fragment
        val settingsFragment = com.example.androidimageapp.fragments.SettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, settingsFragment)
            .addToBackStack(null)
            .commit()
        tvToolbarTitle.text = "Settings"
    }
    
    private fun showClearHistoryDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Clear All History")
            .setMessage("This will permanently delete all chat history. Continue?")
            .setPositiveButton("Clear All") { _, _ ->
                val historyManager = ChatHistoryManager(this)
                historyManager.clearAllHistory()
                showToast("Chat history cleared")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showResetDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Reset App Settings")
            .setMessage("This will reset all app settings to default. Continue?")
            .setPositiveButton("Reset") { _, _ ->
                prefs.edit().clear().apply()
                showToast("Settings reset")
                recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun showBackgroundDialog() {
        val backgrounds = arrayOf(
            "Default Premium",
            "Dark Gradient",
            "Space Theme",
            "Ocean Theme",
            "Custom Image"
        )
        
        val currentBg = prefs.getString("chat_background", "default")
        val selectedIndex = when (currentBg) {
            "default" -> 0
            "dark" -> 1
            "space" -> 2
            "ocean" -> 3
            "custom" -> 4
            else -> 0
        }
        
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Chat Background")
            .setSingleChoiceItems(backgrounds, selectedIndex) { dialog, which ->
                val bgType = when (which) {
                    0 -> "default"
                    1 -> "dark"
                    2 -> "space"
                    3 -> "ocean"
                    4 -> "custom"
                    else -> "default"
                }
                
                if (which == 4) {
                    // Custom image - would need image picker implementation
                    showToast("Custom background feature coming soon")
                } else {
                    prefs.edit().putString("chat_background", bgType).apply()
                    showToast("Background updated")
                    // Refresh chat fragment if needed
                    getChatFragment()?.refreshBackground()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAboutDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("About AI Assistant")
            .setMessage("Advanced AI conversation platform with multi-modal capabilities.\n\nVersion 1.0.0\n\nOpen Source Project\nSupports text and image analysis across multiple language models.")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun getChatFragment(): com.example.androidimageapp.fragments.ChatFragment? {
        return supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? com.example.androidimageapp.fragments.ChatFragment
    }
    
    data class CustomModel(
        val name: String, 
        val id: String, 
        val isDefault: Boolean = false,
        val supportsImages: Boolean = false
    )
    
    private fun getDefaultModels(): List<CustomModel> {
        return listOf(
            // === IMAGE + CHAT MODELS (11 models) ===
            CustomModel("Mistral Small 3.2 24B", "mistralai/mistral-small-3.2-24b-instruct:free", true, true),
            CustomModel("Kimi VL A3B Thinking", "moonshotai/kimi-vl-a3b-thinking:free", true, true),
            CustomModel("Llama 4 Maverick", "meta-llama/llama-4-maverick:free", true, true),
            CustomModel("Llama 4 Scout", "meta-llama/llama-4-scout:free", true, true),
            CustomModel("Qwen2.5 VL 32B", "qwen/qwen2.5-vl-32b-instruct:free", true, true),
            CustomModel("Mistral Small 3.1 24B", "mistralai/mistral-small-3.1-24b-instruct:free", true, true),
            CustomModel("Gemma 3 4B IT", "google/gemma-3-4b-it:free", true, true),
            CustomModel("Gemma 3 12B IT", "google/gemma-3-12b-it:free", true, true),
            CustomModel("Gemma 3 27B IT", "google/gemma-3-27b-it:free", true, true),
            CustomModel("Qwen2.5 VL 72B", "qwen/qwen2.5-vl-72b-instruct:free", true, true),
            CustomModel("Gemini 2.0 Flash", "google/gemini-2.0-flash-exp:free", true, true),
            
            // === CHAT ONLY MODELS (28 models) ===
            CustomModel("Tongyi DeepResearch 30B", "alibaba/tongyi-deepresearch-30b-a3b:free", true, false),
            CustomModel("Longcat Flash Chat", "meituan/longcat-flash-chat:free", true, false),
            CustomModel("Nemotron Nano 9B V2", "nvidia/nemotron-nano-9b-v2:free", true, false),
            CustomModel("DeepSeek Chat V3.1", "deepseek/deepseek-chat-v3.1:free", true, false),
            CustomModel("GPT OSS 20B", "openai/gpt-oss-20b:free", true, false),
            CustomModel("GLM 4.5 Air", "z-ai/glm-4.5-air:free", true, false),
            CustomModel("Qwen3 Coder", "qwen/qwen3-coder:free", true, false),
            CustomModel("Kimi K2", "moonshotai/kimi-k2:free", true, false),
            CustomModel("Dolphin Mistral 24B Venice", "cognitivecomputations/dolphin-mistral-24b-venice-edition:free", true, false),
            CustomModel("Gemma 3N E2B IT", "google/gemma-3n-e2b-it:free", true, false),
            CustomModel("Hunyuan A13B", "tencent/hunyuan-a13b-instruct:free", true, false),
            CustomModel("DeepSeek R1T2 Chimera", "tngtech/deepseek-r1t2-chimera:free", true, false),
            CustomModel("Kimi Dev 72B", "moonshotai/kimi-dev-72b:free", true, false),
            CustomModel("DeepSeek R1 Qwen3 8B", "deepseek/deepseek-r1-0528-qwen3-8b:free", true, false),
            CustomModel("DeepSeek R1 0528", "deepseek/deepseek-r1-0528:free", true, false),
            CustomModel("Devstral Small 2505", "mistralai/devstral-small-2505:free", true, false),
            CustomModel("Gemma 3N E4B IT", "google/gemma-3n-e4b-it:free", true, false),
            CustomModel("Llama 3.3 8B", "meta-llama/llama-3.3-8b-instruct:free", true, false),
            CustomModel("Qwen3 4B", "qwen/qwen3-4b:free", true, false),
            CustomModel("Qwen3 30B A3B", "qwen/qwen3-30b-a3b:free", true, false),
            CustomModel("Qwen3 8B", "qwen/qwen3-8b:free", true, false),
            CustomModel("Qwen3 14B", "qwen/qwen3-14b:free", true, false),
            CustomModel("Qwen3 235B A22B", "qwen/qwen3-235b-a22b:free", true, false),
            CustomModel("DeepSeek R1T Chimera", "tngtech/deepseek-r1t-chimera:free", true, false),
            CustomModel("MAI DS R1", "microsoft/mai-ds-r1:free", true, false),
            CustomModel("Shisa V2 Llama3.3 70B", "shisa-ai/shisa-v2-llama3.3-70b:free", true, false),
            CustomModel("QwQ 32B ArliAI RPR V1", "arliai/qwq-32b-arliai-rpr-v1:free", true, false),
            CustomModel("DeepCoder 14B Preview", "agentica-org/deepcoder-14b-preview:free", true, false)
        )
    }
    
    private fun getCustomModels(): List<CustomModel> {
        val json = prefs.getString("custom_models", null)
        val userCustomModels = if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<CustomModel>>() {}.type
            try {
                com.google.gson.Gson().fromJson<List<CustomModel>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        
        // Always include default models + user custom models
        val allModels = mutableListOf<CustomModel>()
        allModels.addAll(getDefaultModels())
        
        // Add user custom models (non-defaults only)
        val defaultModelIds = getDefaultModels().map { it.id }.toSet()
        userCustomModels.forEach { model ->
            if (!defaultModelIds.contains(model.id)) {
                allModels.add(model.copy(isDefault = false))
            }
        }
        
        return allModels
    }
    
    private fun saveCustomModels(models: List<CustomModel>) {
        // Only save user-added custom models (not defaults)
        val userCustomModels = models.filter { !it.isDefault }
        val json = com.google.gson.Gson().toJson(userCustomModels)
        prefs.edit().putString("custom_models", json).apply()
    }
    
    private fun saveApiKey(key: String) {
        prefs.edit().putString("api_key", key).apply()
    }
    
    private fun loadApiKey() {
        val savedKey = prefs.getString("api_key", null)
        if (!savedKey.isNullOrEmpty()) {
            apiKey = savedKey
        }
    }
    
    private fun loadChatFromHistory(chatId: String) {
        try {
            val historyManager = ChatHistoryManager(this)
            val chatSession = historyManager.getChatSession(chatId)
            chatSession?.let { session ->
                // Wait for fragment to be ready and ensure it's properly initialized
                supportFragmentManager.executePendingTransactions()
                
                // Give the fragment time to initialize
                findViewById<View>(android.R.id.content).post {
                    val chatFragment = getChatFragment()
                    if (chatFragment != null && chatFragment.isAdded) {
                        chatFragment.loadChatSession(session.messages, session.modelUsed)
                        tvToolbarTitle.text = session.title
                    } else {
                        android.util.Log.w("MainActivity", "ChatFragment not ready for loading")
                        showToast("Please wait and try again")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error loading chat from history", e)
            showToast("Error loading chat history")
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private var _apiKey: String? = "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"
        
        var apiKey: String?
            get() = _apiKey
            set(value) {
                if (value.isNullOrBlank() || value == "YOUR_REAL_OPENROUTER_API_KEY_HERE") {
                    _apiKey = null
                } else {
                    _apiKey = value
                }
            }
            
        fun isValidApiKey(): Boolean = !apiKey.isNullOrBlank() && apiKey != "YOUR_REAL_OPENROUTER_API_KEY_HERE"
        
        val apiService: ApiService by lazy {
            if (!isValidApiKey()) {
                throw IllegalStateException("API key not set or invalid")
            }
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $apiKey")
                            .addHeader("Content-Type", "application/json")
                            .addHeader("HTTP-Referer", "https://androidimageapp.example.com")
                            .addHeader("X-Title", "Android Image Chat")
                            .build()
                        chain.proceed(request)
                    }
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .build())
                .build()

            retrofit.create(ApiService::class.java)
        }

        fun getConnectivityManager(context: Context): ConnectivityManager = 
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        private const val BASE_URL = "https://openrouter.ai/api/v1/"
    }
}

class ModelManagerAdapter(
    private val models: MutableList<MainActivity.CustomModel>,
    private val onDeleteClick: (MainActivity.CustomModel) -> Unit
) : RecyclerView.Adapter<ModelManagerAdapter.ViewHolder>() {
    
    init {
        android.util.Log.d("ModelManagerAdapter", "Adapter created with ${models.size} models")
        models.forEachIndexed { index, model ->
            android.util.Log.d("ModelManagerAdapter", "Model $index: ${model.name} (default: ${model.isDefault})")
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model_manager, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        android.util.Log.d("ModelManagerAdapter", "Binding position $position: ${models[position].name}")
        holder.bind(models[position])
    }
    
    override fun getItemCount(): Int {
        android.util.Log.d("ModelManagerAdapter", "getItemCount: ${models.size}")
        return models.size
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvModelName = itemView.findViewById<TextView>(R.id.tvModelName)
        private val tvModelId = itemView.findViewById<TextView>(R.id.tvModelId)
        private val btnDeleteModel = itemView.findViewById<ImageButton>(R.id.btnDeleteModel)
        
        fun bind(model: MainActivity.CustomModel) {
            tvModelName.text = if (model.isDefault) "${model.name} (Default)" else model.name
            tvModelId.text = model.id
            
            // Show different visual cues for default vs custom models
            if (model.isDefault) {
                btnDeleteModel.alpha = 0.3f
                btnDeleteModel.isEnabled = false
            } else {
                btnDeleteModel.alpha = 1.0f
                btnDeleteModel.isEnabled = true
            }
            
            btnDeleteModel.setOnClickListener { onDeleteClick(model) }
        }
    }
}