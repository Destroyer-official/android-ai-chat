package com.example.androidimageapp.fragments

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.androidimageapp.*
import com.example.androidimageapp.utils.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.noties.markwon.Markwon

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayOutputStream
import retrofit2.HttpException

class ChatFragment : Fragment() {
    
    // UI Components
    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnImageUpload: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var ivImagePreview: ImageView
    private lateinit var btnClearImage: ImageButton
    
    // Core components
    private lateinit var markwon: Markwon
    private lateinit var adapter: ChatAdapter
    private lateinit var historyManager: ChatHistoryManager
    private lateinit var preferenceManager: PreferenceManager
    private val gson = Gson()
    private val prefs by lazy { requireContext().getSharedPreferences("ai_chat_prefs", Context.MODE_PRIVATE) }
    
    // Data
    private val messages = mutableListOf<MessageItem>()
    private var currentImageUri: Uri? = null
    private var selectedModelId = "mistralai/mistral-small-3.2-24b-instruct:free" // Default to first image+chat model
    
    // Models are now managed by MainActivity - no hardcoded list needed
    
    // Permission launcher#
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            pickImageLauncher.launch("image/*")
        } else {
            showToast("Permission required for image upload")
        }
    }
    
    // Image picker launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        handleImageSelection(uri)
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupMarkdown()
        setupRecyclerView()
        setupClickListeners()
        historyManager = ChatHistoryManager(requireContext())
        preferenceManager = PreferenceManager(requireContext())
        loadChatHistory()
        applyBackgroundSettings()
    }
    
    override fun onPause() {
        super.onPause()
        // Auto-save current chat when app goes to background
        if (messages.isNotEmpty()) {
            try {
                historyManager.saveCurrentChat(messages, selectedModelId)
            } catch (e: Exception) {
                // Ignore save errors
            }
        }
    }
    
    private fun initializeViews(view: View) {
        rvChat = view.findViewById(R.id.rvChat)
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
        btnImageUpload = view.findViewById(R.id.btnImageUpload)
        progressBar = view.findViewById(R.id.progressBar)
        ivImagePreview = view.findViewById(R.id.ivImagePreview)
        btnClearImage = view.findViewById(R.id.btnClearImage)
    }
    
    private fun setupMarkdown() {
        markwon = Markwon.builder(requireContext())
            .usePlugin(io.noties.markwon.html.HtmlPlugin.create())
            .usePlugin(io.noties.markwon.linkify.LinkifyPlugin.create())
            .usePlugin(object : io.noties.markwon.AbstractMarkwonPlugin() {
                override fun configureTheme(builder: io.noties.markwon.core.MarkwonTheme.Builder) {
                    builder
                        // Enhanced code styling
                        .codeTextColor(ContextCompat.getColor(requireContext(), R.color.code_text_premium))
                        .codeBackgroundColor(ContextCompat.getColor(requireContext(), R.color.code_background_premium))
                        .codeBlockTextColor(ContextCompat.getColor(requireContext(), R.color.code_text_premium))
                        .codeBlockBackgroundColor(ContextCompat.getColor(requireContext(), R.color.code_background_premium))
                        .codeBlockMargin(32)
                        .codeTypeface(android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL))
                        .codeTextSize(android.util.TypedValue.applyDimension(
                            android.util.TypedValue.COMPLEX_UNIT_SP, 14f, 
                            requireContext().resources.displayMetrics).toInt())
                        
                        // Enhanced heading styling
                        .headingBreakHeight(12)
                        .headingTextSizeMultipliers(floatArrayOf(1.8f, 1.5f, 1.3f, 1.2f, 1.1f, 1.0f))
                        
                        // Enhanced list and link styling
                        .listItemColor(ContextCompat.getColor(requireContext(), R.color.accent_primary))
                        .linkColor(ContextCompat.getColor(requireContext(), R.color.accent_secondary))
                        .blockMargin(20)
                        .blockQuoteColor(ContextCompat.getColor(requireContext(), R.color.accent_tertiary))
                        .blockQuoteWidth(6)
                        
                        // Enhanced text styling
                        .bulletListItemStrokeWidth(3)
                        .bulletWidth(8)
                }
            })
            .build()
    }
    
    private fun setupRecyclerView() {
        adapter = ChatAdapter(markwon, ::copyMessage, ::copyCode)
        rvChat.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = this@ChatFragment.adapter
            
            // Enhanced premium animations
            itemAnimator?.apply {
                addDuration = 300
                changeDuration = 250
                moveDuration = 250
                removeDuration = 200
            }
            
            // Add smooth scrolling behavior
            isNestedScrollingEnabled = true
            
            // Add layout animation for premium feel
            try {
                val layoutAnimationController = android.view.animation.AnimationUtils.loadLayoutAnimation(
                    requireContext(), R.anim.layout_animation_fall_down
                )
                layoutAnimation = layoutAnimationController
            } catch (e: Exception) {
                // Ignore if animation resource doesn't exist
            }
        }
    }
    
    private fun setupClickListeners() {
        btnSend.setOnClickListener { sendMessage() }
        btnImageUpload.setOnClickListener { requestImagePermission() }
        btnClearImage.setOnClickListener { clearImage() }
    }
    
    private fun sendMessage() {
        val messageText = etMessage.text.toString().trim()
        if (messageText.isEmpty() && currentImageUri == null) {
            showToast("Enter a message or select an image")
            return
        }
        
        if (messageText.length > 4000) {
            showToast("Message too long (max 4000 characters)")
            return
        }
        
        // Clear input and show user message
        etMessage.text.clear()
        val hasImage = currentImageUri != null
        val displayText = if (hasImage) "🖼️ $messageText" else messageText
        
        addMessage(displayText, isUser = true, hasImage = hasImage)
        
        // Send to AI (don't clear image yet - we need it for the API call)
        sendToAI(messageText, hasImage)
    }
    
    private fun sendToAI(messageText: String, hasImage: Boolean) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val apiMessages = buildApiMessages(messageText, hasImage)
                val request = ChatRequest(model = selectedModelId, messages = apiMessages)
                
                // Debug logging
                Log.d("ChatFragment", "Sending request to model: $selectedModelId")
                Log.d("ChatFragment", "Has image: $hasImage")
                Log.d("ChatFragment", "Messages count: ${apiMessages.size}")
                apiMessages.forEach { msg ->
                    Log.d("ChatFragment", "Message role: ${msg.role}, content types: ${msg.content.map { it.type }}")
                }
                
                val response = withContext(Dispatchers.IO) {
                    MainActivity.apiService.getChatCompletion(request)
                }
                
                if (response.isSuccessful) {
                    val aiResponse = response.body()?.choices?.firstOrNull()?.message?.content
                    if (!aiResponse.isNullOrBlank()) {
                        addMessage(aiResponse, isUser = false)
                    } else {
                        addMessage("❌ No response received", isUser = false)
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "❌ Invalid API key"
                        429 -> "❌ Rate limit exceeded. Please wait."
                        500 -> "❌ Server error. Please try again."
                        else -> "❌ Error: ${response.code()}"
                    }
                    addMessage(errorMsg, isUser = false)
                }
            } catch (e: Exception) {
                Log.e("ChatFragment", "Error sending message", e)
                val errorMsg = when (e) {
                    is HttpException -> "❌ Network error: ${e.code()}"
                    else -> "❌ Connection failed. Check your internet."
                }
                addMessage(errorMsg, isUser = false)
            } finally {
                showLoading(false)
                // Clear image after API request is completed
                if (hasImage) {
                    clearImage()
                }
            }
        }
    }
    
    private fun buildApiMessages(messageText: String, hasImage: Boolean): List<Message> {
        val apiMessages = mutableListOf<Message>()
        
        // Add conversation history (last 20 messages)
        val history = messages.takeLast(20).dropLast(1) // Exclude the message we just added
        for (msg in history) {
            val role = if (msg.isUser) "user" else "assistant"
            val cleanText = msg.text.replace("🖼️ ", "").trim()
            if (cleanText.isNotEmpty()) {
                apiMessages.add(Message(role, listOf(Content("text", cleanText))))
            }
        }
        
        // Add current message
        val currentContent = mutableListOf<Content>()
        
        // Add image if present
        if (hasImage && currentImageUri != null) {
            try {
                val base64Image = encodeImageToBase64(currentImageUri!!)
                val imageUrl = "data:image/jpeg;base64,$base64Image"
                Log.d("ChatFragment", "Image encoded successfully, size: ${base64Image.length} chars")
                Log.d("ChatFragment", "Image URL prefix: ${imageUrl.take(100)}...")
                currentContent.add(Content("image_url", image_url = ImageUrl(imageUrl)))
            } catch (e: Exception) {
                Log.e("ChatFragment", "Failed to encode image", e)
                showToast("Failed to process image")
            }
        }
        
        // Add text
        if (messageText.isNotEmpty()) {
            currentContent.add(Content("text", messageText))
        }
        
        if (currentContent.isNotEmpty()) {
            apiMessages.add(Message("user", currentContent))
        }
        
        return apiMessages
    }
    

    private fun encodeImageToBase64(uri: Uri): String {
        return requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
            var bitmap = BitmapFactory.decodeStream(inputStream)
                ?: throw IllegalStateException("Failed to decode image")
            
            // Resize image if it's too large (max 1024x1024)
            val maxSize = 1024
            if (bitmap.width > maxSize || bitmap.height > maxSize) {
                val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
                val newWidth = (bitmap.width * ratio).toInt()
                val newHeight = (bitmap.height * ratio).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                Log.d("ChatFragment", "Resized image to ${newWidth}x${newHeight}")
            }
            
            val baos = ByteArrayOutputStream()
            // Use higher quality for better results
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            val byteArray = baos.toByteArray()
            
            Log.d("ChatFragment", "Final image size: ${byteArray.size} bytes (${byteArray.size / 1024}KB)")
            
            if (byteArray.size > 4 * 1024 * 1024) { // 4MB limit
                throw IllegalStateException("Image too large (max 4MB)")
            }
            
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } ?: throw IllegalStateException("Failed to read image")
    }
    
    private fun addMessage(text: String, isUser: Boolean, hasImage: Boolean = false) {
        if (!isAdded) return
        
        // Get image data if present
        var imageData: String? = null
        if (hasImage && isUser && currentImageUri != null) {
            try {
                imageData = encodeImageToBase64(currentImageUri!!)
                Log.d("ChatFragment", "Saved image data for message, size: ${imageData.length} chars")
            } catch (e: Exception) {
                Log.e("ChatFragment", "Failed to encode image for storage", e)
            }
        }
        
        val messageItem = MessageItem(
            text = text,
            isUser = isUser,
            hasImage = hasImage,
            imageUri = if (hasImage && isUser) currentImageUri?.toString() else null, // Keep for current session
            imageData = imageData, // Save for persistence
            timestamp = System.currentTimeMillis()
        )
        
        messages.add(messageItem)
        
        // Update toolbar title based on first user message
        if (isUser && messages.count { it.isUser } == 1) {
            val title = generateChatTitle(text)
            updateToolbarTitle(title)
        }
        
        // Limit messages to prevent memory issues
        if (messages.size > 100) {
            messages.removeAt(0)
        }
        
        adapter.submitList(messages.toList()) {
            rvChat.scrollToPosition(messages.size - 1)
        }
        
        savePreferences()
    }
    
    // Generate a smart title from the first message
    private fun generateChatTitle(firstMessage: String): String {
        val cleanMessage = firstMessage.replace("🖼️ ", "").trim()
        
        // If message is too short, use default
        if (cleanMessage.length < 10) {
            return "AI Assistant"
        }
        
        // Take first 30 characters and add ellipsis if needed
        val title = if (cleanMessage.length > 30) {
            cleanMessage.take(27) + "..."
        } else {
            cleanMessage
        }
        
        // Capitalize first letter
        return title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    
    private fun requestImagePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (permissions.all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }) {
            pickImageLauncher.launch("image/*")
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }
    
    private fun handleImageSelection(uri: Uri?) {
        currentImageUri = uri
        if (uri != null) {
            ivImagePreview.setImageURI(uri)
            findViewById<View>(R.id.imagePreviewCard).visibility = View.VISIBLE
            showToast("Image selected. Add message and send.")
        } else {
            clearImage()
        }
    }
    
    private fun <T : View> findViewById(id: Int): T {
        return requireView().findViewById(id)
    }
    
    private fun clearImage() {
        currentImageUri = null
        findViewById<View>(R.id.imagePreviewCard).visibility = View.GONE
    }
    
    private fun showNewChatDialog() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("New Chat")
            .setMessage("Start a new conversation? This will clear the current chat.")
            .setPositiveButton("Start New") { _, _ ->
                clearChat()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun clearChat() {
        // Save current chat to history before clearing
        if (messages.isNotEmpty()) {
            historyManager.saveCurrentChat(messages, selectedModelId)
        }
        
        messages.clear()
        adapter.submitList(emptyList())
        clearImage()
        savePreferences()
        
        // Reset toolbar title
        updateToolbarTitle("AI Assistant")
        
        showToast("Chat cleared")
    }
    
    private fun copyMessage(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Message", text))
        showToast("Message copied")
    }
    
    private fun copyCode(code: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
        showToast("Code copied")
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSend.isEnabled = !show
    }
    
    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadChatHistory() {
        try {
            val json = prefs.getString("messages", "[]") ?: "[]"
            val type = object : TypeToken<List<MessageItem>>() {}.type
            val savedMessages = gson.fromJson<List<MessageItem>>(json, type) ?: emptyList()
            
            messages.clear()
            messages.addAll(savedMessages.takeLast(50)) // Load last 50 messages
            adapter.submitList(messages.toList())
            
            // Restore model selection
            val savedModelId = prefs.getString("selected_model", "mistralai/mistral-small-3.2-24b-instruct:free") ?: "mistralai/mistral-small-3.2-24b-instruct:free"
            selectedModelId = savedModelId
            
            if (messages.isNotEmpty()) {
                rvChat.scrollToPosition(messages.size - 1)
            }
        } catch (e: Exception) {
            Log.e("ChatFragment", "Error loading chat history", e)
        }
    }
    
    private fun savePreferences() {
        try {
            prefs.edit()
                .putString("messages", gson.toJson(messages.takeLast(50)))
                .putString("selected_model", selectedModelId)
                .apply()
        } catch (e: Exception) {
            Log.e("ChatFragment", "Error saving preferences", e)
        }
    }
    
    // Public method to change model from MainActivity
    fun changeModel(modelId: String) {
        selectedModelId = modelId
        savePreferences()
        showToast("Model changed to $modelId")
    }
    
    // Public method to start new chat from MainActivity
    fun startNewChat() {
        clearChat()
        // Reset toolbar title when starting new chat
        updateToolbarTitle("AI Assistant")
    }
    
    // Helper method to update toolbar title
    private fun updateToolbarTitle(title: String) {
        (activity as? MainActivity)?.findViewById<TextView>(R.id.tvToolbarTitle)?.text = title
    }
    
    // Public method to load chat session from history
    fun loadChatSession(chatMessages: List<MessageItem>, modelId: String) {
        try {
            // Check if fragment is properly initialized
            if (!isAdded || view == null || !::adapter.isInitialized || !::rvChat.isInitialized) {
                Log.w("ChatFragment", "Fragment not ready for loading chat session")
                return
            }
            
            messages.clear()
            messages.addAll(chatMessages)
            selectedModelId = modelId
            
            adapter.submitList(messages.toList()) {
                if (messages.isNotEmpty() && isAdded && ::rvChat.isInitialized) {
                    rvChat.post {
                        try {
                            rvChat.scrollToPosition(messages.size - 1)
                        } catch (e: Exception) {
                            android.util.Log.w("ChatFragment", "Scroll error ignored", e)
                        }
                    }
                }
            }
            savePreferences()
            showToast("Chat loaded from history")
        } catch (e: Exception) {
            android.util.Log.e("ChatFragment", "Error loading chat session", e)
            showToast("Error loading chat")
        }
    }
    
    private fun applyBackgroundSettings() {
        val background = preferenceManager.getChatBackground()
        val customBackground = preferenceManager.getCustomChatBackground()
        
        val backgroundImage = requireView().findViewById<ImageView>(R.id.background_image)
        
        when {
            customBackground != null -> {
                try {
                    val uri = Uri.parse(customBackground)
                    val bitmap = if (customBackground.startsWith("file://")) {
                        // Handle internal file URIs
                        val filePath = customBackground.removePrefix("file://")
                        val file = java.io.File(filePath)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(filePath)
                        } else {
                            null
                        }
                    } else {
                        // Handle content URIs (legacy support)
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        inputStream?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    
                    if (bitmap != null) {
                        backgroundImage.setImageBitmap(bitmap)
                        backgroundImage.alpha = 0.8f // Make it slightly transparent
                    } else {
                        Log.w("ChatFragment", "Custom background file not found, using default")
                        // Clear the invalid custom background preference
                        preferenceManager.setChatBackground("default")
                        setDefaultBackground(backgroundImage)
                    }
                } catch (e: Exception) {
                    Log.e("ChatFragment", "Error setting custom background, using default", e)
                    // Clear the invalid custom background preference
                    preferenceManager.setChatBackground("default")
                    setDefaultBackground(backgroundImage)
                }
            }
            background != "default" -> {
                val resourceId = getBackgroundResource(background)
                if (resourceId != 0) {
                    backgroundImage.setImageResource(resourceId)
                } else {
                    setDefaultBackground(backgroundImage)
                }
            }
            else -> {
                setDefaultBackground(backgroundImage)
            }
        }
    }
    
    private fun setDefaultBackground(imageView: ImageView) {
        imageView.setImageResource(R.drawable.chat_background_default)
    }
    
    private fun getBackgroundResource(backgroundKey: String): Int {
        return when (backgroundKey) {
            "gradient_blue" -> R.drawable.chat_background_blue
            "gradient_purple" -> R.drawable.chat_background_purple
            "gradient_green" -> R.drawable.chat_background_green
            "dark_pattern" -> R.drawable.chat_background_dark
            "neural_network" -> R.drawable.chat_background_neural
            else -> 0
        }
    }
    
    // Public method to refresh background when settings change
    fun refreshBackground() {
        if (isAdded && view != null) {
            applyBackgroundSettings()
        }
    }
}

// Data classes
data class MessageItem(
    val text: String,
    val isUser: Boolean,
    val hasImage: Boolean = false,
    val imageUri: String? = null, // Keep for backward compatibility
    val imageData: String? = null, // Base64 encoded image data
    val timestamp: Long = System.currentTimeMillis()
)