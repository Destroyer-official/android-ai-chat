package com.example.androidimageapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatHistoryActivity : AppCompatActivity() {
    
    private lateinit var rvChatHistory: RecyclerView
    private lateinit var emptyState: View
    private lateinit var historyManager: ChatHistoryManager
    private lateinit var adapter: ChatHistoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_history)
        
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        loadChatHistory()
    }
    
    private fun initializeViews() {
        rvChatHistory = findViewById(R.id.rvChatHistory)
        emptyState = findViewById(R.id.emptyState)
        historyManager = ChatHistoryManager(this)
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupRecyclerView() {
        adapter = ChatHistoryAdapter(
            onChatClick = { chatSession ->
                // Load chat in main activity
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("load_chat_id", chatSession.id)
                startActivity(intent)
                finish()
            },
            onDeleteClick = { chatSession ->
                showDeleteDialog(chatSession)
            }
        )
        
        rvChatHistory.layoutManager = LinearLayoutManager(this)
        rvChatHistory.adapter = adapter
    }
    
    private fun loadChatHistory() {
        try {
            val chatSessions = historyManager.getAllChatSessions()
            
            if (chatSessions.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                rvChatHistory.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                rvChatHistory.visibility = View.VISIBLE
                adapter.submitList(chatSessions)
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatHistoryActivity", "Error loading chat history", e)
            emptyState.visibility = View.VISIBLE
            rvChatHistory.visibility = View.GONE
        }
    }
    
    private fun showDeleteDialog(chatSession: ChatSession) {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Delete Chat")
            .setMessage("Are you sure you want to delete \"${chatSession.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                historyManager.deleteChat(chatSession.id)
                loadChatHistory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class ChatHistoryAdapter(
    private val onChatClick: (ChatSession) -> Unit,
    private val onDeleteClick: (ChatSession) -> Unit
) : androidx.recyclerview.widget.ListAdapter<ChatSession, ChatHistoryAdapter.ViewHolder>(ChatHistoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_history, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvChatTitle = itemView.findViewById<android.widget.TextView>(R.id.tvChatTitle)
        private val tvChatPreview = itemView.findViewById<android.widget.TextView>(R.id.tvChatPreview)
        private val tvChatDate = itemView.findViewById<android.widget.TextView>(R.id.tvChatDate)
        private val tvMessageCount = itemView.findViewById<android.widget.TextView>(R.id.tvMessageCount)
        private val btnDeleteChat = itemView.findViewById<android.widget.ImageButton>(R.id.btnDeleteChat)
        
        fun bind(chatSession: ChatSession) {
            try {
                tvChatTitle.text = chatSession.title
                
                // Show preview of last AI message
                val lastAiMessage = chatSession.messages.findLast { !it.isUser }?.text?.take(100)
                tvChatPreview.text = lastAiMessage ?: "No AI response yet"
                
                // Format date
                val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                tvChatDate.text = dateFormat.format(Date(chatSession.timestamp))
                
                // Message count
                tvMessageCount.text = "${chatSession.messages.size} messages"
                
                // Click listeners
                itemView.setOnClickListener { onChatClick(chatSession) }
                btnDeleteChat.setOnClickListener { onDeleteClick(chatSession) }
            } catch (e: Exception) {
                android.util.Log.e("ChatHistoryAdapter", "Error binding chat session", e)
                tvChatTitle.text = "Error loading chat"
                tvChatPreview.text = "Unable to load chat data"
                tvChatDate.text = ""
                tvMessageCount.text = "0 messages"
            }
        }
    }
}

class ChatHistoryDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<ChatSession>() {
    override fun areItemsTheSame(oldItem: ChatSession, newItem: ChatSession): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: ChatSession, newItem: ChatSession): Boolean {
        return oldItem == newItem
    }
}