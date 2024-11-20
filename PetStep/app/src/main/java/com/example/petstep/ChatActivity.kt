package com.example.petstep

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petstep.adapters.ChatRecyclerAdapter
import com.example.petstep.databinding.ActivityChatBinding
import com.example.petstep.model.ChatroomModel
import com.example.petstep.model.MessageModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    lateinit var binding: ActivityChatBinding
    private var userId: String? = null
    private var username: String? = null
    private var chatroomId: String? = null
    private lateinit var auth: FirebaseAuth
    lateinit var chatroomModel: ChatroomModel
    var userIds: MutableList<String> = mutableListOf()
    lateinit var adapter: ChatRecyclerAdapter
    private var messagesList: MutableList<MessageModel> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityChatBinding.inflate(layoutInflater)
            setContentView(binding.root)

            auth = FirebaseAuth.getInstance()

            if (auth.currentUser != null) {
                userId = "123"
                username = "Juan"

                chatroomId = getChatroomId(auth.currentUser!!.uid, userId!!)
                binding.otherUserName.text = username

                userIds.add(auth.currentUser!!.uid)
                userIds.add(userId!!)

                binding.sendButton.setOnClickListener {
                    val message = binding.messageEditText.text.toString()
                    if (message.isNotEmpty()) {
                        sendMessageToUser(message)
                        binding.messageEditText.text.clear()
                    }
                }

                getOrCreateChatroomModel()
                setupChatRecyclerView()
                loadMessages()
            } else {
                Log.e("ChatActivity", "User is not authenticated")
                // Handle the case where the user is not authenticated
            }
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error in onCreate", e)
        }
    }

    private fun setupChatRecyclerView() {
        adapter = ChatRecyclerAdapter(messagesList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun sendMessageToUser(message: String) {
        chatroomModel.lastMessageTimestamp = Timestamp.now()
        chatroomModel.lastMessageSenderId = auth.currentUser!!.uid
        getChatroomReference(chatroomId!!).setValue(chatroomModel)

        val messageModel = MessageModel(message, auth.currentUser!!.uid, Timestamp.now())
        getChatroomMessageReference(chatroomId!!).push().setValue(messageModel).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.i("ChatActivity", "Message sent successfully")
                binding.messageEditText.text.clear()
            } else {
                Log.e("ChatActivity", "Error sending message", task.exception)
            }
        }
    }

    private fun getOrCreateChatroomModel() {
        try {
            getChatroomReference(chatroomId!!).get().addOnSuccessListener { chatroomSnapshot ->
                if (chatroomSnapshot.exists()) {
                    Log.i("ChatActivity", "Chatroom exists")
                    // Chatroom already exists
                    chatroomModel = chatroomSnapshot.getValue(ChatroomModel::class.java)!!
                } else {
                    Log.i("ChatActivity", "Chatroom does not exist, creating new one")
                    // Chatroom does not exist, create a new one
                    chatroomModel = ChatroomModel(chatroomId!!, userIds, Timestamp.now(), "")
                    getChatroomReference(chatroomId!!).setValue(chatroomModel)
                }
            }.addOnFailureListener { e ->
                Log.e("ChatActivity", "Error getting chatroom", e)
            }
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error in getOrCreateChatroomModel", e)
        }
    }

    private fun loadMessages() {
        getChatroomMessageReference(chatroomId!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messagesList.clear()
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(MessageModel::class.java)
                    if (message != null) {
                        messagesList.add(message)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Error loading messages", error.toException())
            }
        })
    }

    private fun getChatroomId(userId1: String, userId2: String): String {
        return if (userId1.hashCode() < userId2.hashCode()) {
            "$userId1-$userId2"
        } else {
            "$userId2-$userId1"
        }
    }

    private fun getChatroomReference(chatroomId: String): DatabaseReference {
        Log.i("ChatActivity", "Chatroom reference: $chatroomId")
        return FirebaseDatabase.getInstance().getReference("chatrooms").child(chatroomId)
    }

    private fun getChatroomMessageReference(chatroomId: String): DatabaseReference {
        return getChatroomReference(chatroomId).child("Chats")
    }
}