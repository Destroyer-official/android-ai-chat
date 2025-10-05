package com.example.androidimageapp

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.androidimageapp.fragments.MessageItem
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class ChatAdapter(
    private val markwon: Markwon,
    private val onCopyMessage: (String) -> Unit,
    private val onCopyCode: (String) -> Unit
) : ListAdapter<MessageItem, ChatAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_USER) {
            R.layout.item_message_user
        } else {
            R.layout.item_message_ai
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        holder.bind(message)
        
        // Add premium animation for new messages
        if (position == itemCount - 1) {
            val animation = android.view.animation.AnimationUtils.loadAnimation(
                holder.itemView.context, R.anim.message_slide_in
            )
            holder.itemView.startAnimation(animation)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val btnCopy: ImageButton = itemView.findViewById(R.id.btnCopy)
        private val btnCopyCode: ImageButton? = itemView.findViewById(R.id.btnCopyCode)

        fun bind(message: MessageItem) {
            // Set timestamp
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvTimestamp.text = timeFormat.format(Date(message.timestamp))

            // Handle image display for user messages
            val imageCard = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.imageCard)
            val ivMessageImage = itemView.findViewById<android.widget.ImageView>(R.id.ivMessageImage)
            
            if (message.hasImage && message.isUser && imageCard != null && ivMessageImage != null) {
                imageCard.visibility = View.VISIBLE
                
                var imageLoaded = false
                
                // Try to load from URI first (for current session)
                if (message.imageUri != null) {
                    try {
                        ivMessageImage.setImageURI(android.net.Uri.parse(message.imageUri))
                        imageLoaded = true
                    } catch (e: Exception) {
                        android.util.Log.w("ChatAdapter", "Failed to load image from URI", e)
                    }
                }
                
                // If URI failed, try base64 data (for persistent storage)
                if (!imageLoaded && message.imageData != null) {
                    try {
                        val imageBytes = android.util.Base64.decode(message.imageData, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        if (bitmap != null) {
                            ivMessageImage.setImageBitmap(bitmap)
                            imageLoaded = true
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("ChatAdapter", "Failed to load image from base64", e)
                    }
                }
                
                // Fallback to icon if both methods failed
                if (!imageLoaded) {
                    ivMessageImage.setImageResource(R.drawable.ic_image_premium)
                }
            } else {
                imageCard?.visibility = View.GONE
            }

            // Process message text
            val processedText = processMessageText(message.text, message.isUser)
            
            if (message.isUser) {
                // User messages - simple text
                tvMessage.text = processedText
            } else {
                // AI messages - full markdown rendering
                markwon.setMarkdown(tvMessage, processedText)
            }

            // Setup copy buttons
            btnCopy.setOnClickListener {
                onCopyMessage(message.text)
            }

            // Code copy button (only for AI messages)
            btnCopyCode?.let { copyCodeBtn ->
                val codeBlocks = extractCodeBlocks(message.text)
                if (codeBlocks.isNotEmpty()) {
                    copyCodeBtn.visibility = View.VISIBLE
                    copyCodeBtn.setOnClickListener {
                        val allCode = codeBlocks.joinToString("\n\n")
                        onCopyCode(allCode)
                    }
                } else {
                    copyCodeBtn.visibility = View.GONE
                }
            }
        }

        private fun processMessageText(text: String, isUser: Boolean): String {
            var processedText = text

            if (isUser) {
                // Clean user text - remove image indicators
                processedText = processedText.replace("🖼️ ", "")
            } else {
                // Enhance AI responses
                processedText = enhanceAIResponse(processedText)
            }

            return processedText
        }

        private fun enhanceAIResponse(text: String): String {
            var enhanced = text

            // Add better formatting for lists with sci-fi styling
            enhanced = enhanced.replace(Regex("(?m)^(\\d+\\.)\\s"), "**$1** ")
            enhanced = enhanced.replace(Regex("(?m)^(-|\\*)\\s"), ">> ")

            // Enhance code blocks with sci-fi styling and language detection
            enhanced = enhanced.replace(Regex("```(\\w+)?\\n([\\s\\S]*?)```")) { matchResult ->
                val language = matchResult.groupValues[1].ifEmpty { "code" }
                val code = matchResult.groupValues[2].trim()
                val languagePrefix = getLanguagePrefix(language)
                "\n\n**$languagePrefix ${language.uppercase()}**\n\n```$language\n$code\n```\n\n"
            }

            // Enhance inline code with sci-fi styling
            enhanced = enhanced.replace(Regex("`([^`]+)`")) { matchResult ->
                val code = matchResult.groupValues[1]
                "`[CODE] $code`"
            }

            // Add sci-fi emphasis to important words
            enhanced = enhanced.replace(Regex("\\b(Important|Note|Warning|Error|Success|Tips?|Example|Solution|Result)\\b"), "**[!] $1**")

            // Better formatting for headers with sci-fi prefixes
            enhanced = enhanced.replace(Regex("(?m)^(#{1,6})\\s*(.+)$")) { matchResult ->
                val level = matchResult.groupValues[1].length
                val title = matchResult.groupValues[2]
                when (level) {
                    1 -> "\n\n# [SYSTEM] **$title**\n"
                    2 -> "\n\n## [DATA] **$title**\n"
                    3 -> "\n\n### [INFO] **$title**\n"
                    else -> "\n\n#### [LOG] **$title**\n"
                }
            }

            // Enhance step-by-step instructions with sci-fi styling
            enhanced = enhanced.replace(Regex("(?m)^Step\\s+(\\d+):?\\s*(.+)$"), "**[STEP-$1] $2**")

            // Enhance questions and answers with sci-fi styling
            enhanced = enhanced.replace(Regex("(?m)^Q:?\\s*(.+)$"), "**[QUERY] $1**")
            enhanced = enhanced.replace(Regex("(?m)^A:?\\s*(.+)$"), "**[RESPONSE] $1**")

            return enhanced
        }

        private fun getLanguagePrefix(language: String): String {
            return when (language.lowercase()) {
                "python", "py" -> "[PY]"
                "javascript", "js" -> "[JS]"
                "java" -> "[JAVA]"
                "kotlin", "kt" -> "[KT]"
                "cpp", "c++" -> "[CPP]"
                "c" -> "[C]"
                "html" -> "[HTML]"
                "css" -> "[CSS]"
                "sql" -> "[SQL]"
                "json" -> "[JSON]"
                "xml" -> "[XML]"
                "yaml", "yml" -> "[YAML]"
                "bash", "shell" -> "[BASH]"
                "php" -> "[PHP]"
                "ruby" -> "[RUBY]"
                "go" -> "[GO]"
                "rust" -> "[RUST]"
                "swift" -> "[SWIFT]"
                "dart" -> "[DART]"
                else -> "[CODE]"
            }
        }

        private fun extractCodeBlocks(text: String): List<String> {
            val codeBlocks = mutableListOf<String>()
            val pattern = Pattern.compile("```[\\w]*\\n([\\s\\S]*?)```", Pattern.MULTILINE)
            val matcher = pattern.matcher(text)

            while (matcher.find()) {
                val codeBlock = matcher.group(1)?.trim()
                if (!codeBlock.isNullOrEmpty()) {
                    codeBlocks.add(codeBlock)
                }
            }

            // Also extract inline code
            val inlinePattern = Pattern.compile("`([^`]+)`")
            val inlineMatcher = inlinePattern.matcher(text)
            while (inlineMatcher.find()) {
                val inlineCode = inlineMatcher.group(1)?.trim()
                if (!inlineCode.isNullOrEmpty() && inlineCode.length > 10) { // Only longer inline code
                    codeBlocks.add(inlineCode)
                }
            }

            return codeBlocks
        }
    }

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<MessageItem>() {
    override fun areItemsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
        return oldItem.timestamp == newItem.timestamp
    }

    override fun areContentsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
        return oldItem == newItem
    }
}