package com.example.petstep.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petstep.R
import com.example.petstep.model.MessageModel
import com.google.firebase.auth.FirebaseAuth

class ChatRecyclerAdapter(private val messages: List<MessageModel>) : RecyclerView.Adapter<ChatRecyclerAdapter.MessageViewHolder>() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_message_recycler_row, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position], auth.currentUser?.uid)
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val leftChatLayout: LinearLayout = itemView.findViewById(R.id.left_chat_bubble)
        private val rightChatLayout: LinearLayout = itemView.findViewById(R.id.right_chat_bubble)
        private val leftChatTextView: TextView = itemView.findViewById(R.id.left_message)
        private val rightChatTextView: TextView = itemView.findViewById(R.id.right_message)

        fun bind(message: MessageModel, currentUserId: String?) {
            if (message.senderId == currentUserId) {
                rightChatLayout.visibility = View.VISIBLE
                leftChatLayout.visibility = View.GONE
                rightChatTextView.text = message.message
            } else {
                leftChatLayout.visibility = View.VISIBLE
                rightChatLayout.visibility = View.GONE
                leftChatTextView.text = message.message
            }
        }
    }
}