package com.example.androidimageapp.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.androidimageapp.R
import com.example.androidimageapp.utils.PreferenceManager

class SettingsFragment : Fragment() {
    
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var backgroundPreview: ImageView
    private lateinit var backgroundOptions: LinearLayout
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                setCustomBackground(uri)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        
        preferenceManager = PreferenceManager(requireContext())
        
        initViews(view)
        setupBackgroundOptions()
        loadCurrentBackground()
        
        return view
    }
    
    private fun initViews(view: View) {
        backgroundPreview = view.findViewById(R.id.background_preview)
        backgroundOptions = view.findViewById(R.id.background_options)
        
        view.findViewById<Button>(R.id.btn_custom_background).setOnClickListener {
            openImagePicker()
        }
        
        view.findViewById<Button>(R.id.btn_reset_background).setOnClickListener {
            resetToDefault()
        }
    }
    
    private fun setupBackgroundOptions() {
        val backgrounds = listOf(
            "default" to "Default",
            "gradient_blue" to "Blue Gradient",
            "gradient_purple" to "Purple Gradient", 
            "gradient_green" to "Green Gradient",
            "dark_pattern" to "Dark Pattern",
            "neural_network" to "Neural Network"
        )
        
        backgrounds.forEach { (key, name) ->
            val button = Button(requireContext()).apply {
                text = name
                setOnClickListener { setPresetBackground(key) }
            }
            backgroundOptions.addView(button)
        }
    }
    
    private fun setPresetBackground(backgroundKey: String) {
        preferenceManager.setChatBackground(backgroundKey)
        loadCurrentBackground()
        Toast.makeText(requireContext(), "Background updated", Toast.LENGTH_SHORT).show()
    }
    
    private fun setCustomBackground(uri: Uri) {
        try {
            // Copy the image to internal storage for persistent access
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream != null) {
                inputStream.use { stream ->
                    // Create a file in internal storage
                    val internalFile = requireContext().filesDir.resolve("custom_background.jpg")
                    internalFile.outputStream().use { output ->
                        stream.copyTo(output)
                    }
                    
                    // Store the internal file path instead of the original URI
                    preferenceManager.setCustomChatBackground("file://${internalFile.absolutePath}")
                    loadCurrentBackground()
                    Toast.makeText(requireContext(), "Custom background set", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Failed to access selected image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "Error setting custom background", e)
            Toast.makeText(requireContext(), "Error setting custom background", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun resetToDefault() {
        // Clean up custom background file if it exists
        val customBackground = preferenceManager.getCustomChatBackground()
        if (customBackground?.startsWith("file://") == true) {
            try {
                val filePath = customBackground.removePrefix("file://")
                val file = java.io.File(filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                android.util.Log.w("SettingsFragment", "Could not delete custom background file", e)
            }
        }
        
        preferenceManager.setChatBackground("default")
        loadCurrentBackground()
        Toast.makeText(requireContext(), "Reset to default background", Toast.LENGTH_SHORT).show()
    }
    
    private fun loadCurrentBackground() {
        val background = preferenceManager.getChatBackground()
        val customBackground = preferenceManager.getCustomChatBackground()
        
        when {
            customBackground != null -> {
                try {
                    if (customBackground.startsWith("file://")) {
                        // Handle internal file URIs
                        val filePath = customBackground.removePrefix("file://")
                        val file = java.io.File(filePath)
                        if (file.exists()) {
                            val bitmap = android.graphics.BitmapFactory.decodeFile(filePath)
                            backgroundPreview.setImageBitmap(bitmap)
                        } else {
                            backgroundPreview.setImageResource(R.drawable.chat_background_default)
                        }
                    } else {
                        // Handle content URIs (legacy support)
                        backgroundPreview.setImageURI(Uri.parse(customBackground))
                    }
                } catch (e: Exception) {
                    backgroundPreview.setImageResource(R.drawable.chat_background_default)
                }
            }
            background != "default" -> {
                val resourceId = getBackgroundResource(background)
                if (resourceId != 0) {
                    backgroundPreview.setImageResource(resourceId)
                }
            }
            else -> {
                backgroundPreview.setImageResource(R.drawable.chat_background_default)
            }
        }
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
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        imagePickerLauncher.launch(intent)
    }
}