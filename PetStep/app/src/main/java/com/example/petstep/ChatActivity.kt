package com.example.petstep

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petstep.databinding.ActivityChatBinding
import com.example.petstep.model.ChatroomModel
import com.example.petstep.model.MyUser
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {

    lateinit var binding : ActivityChatBinding
    private var userId: String? = null
    private var username: String? = null
    private var chatroomId: String? = null
    private lateinit var auth: FirebaseAuth
    lateinit var ChatroomModel : ChatroomModel
    var userIds: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        //userId = intent.getStringExtra("USER_ID")
        //username = intent.getStringExtra("USER_NAME")

        userId = "123"
        username = "Juan"

        chatroomId = getChatroomId(auth.currentUser!!.uid, userId!!)
        binding.otherUserName.text = username

        userIds.add(auth.currentUser!!.uid)
        userIds.add(userId!!)


        getOrCreateChatroomModel()

    }

    private fun getOrCreateChatroomModel() {
        getChatroomReferece(chatroomId!!).get().addOnSuccessListener { chatroomSnapshot ->
            if(chatroomSnapshot.exists()){
                //Ya existe el chatroom
                //Cargar mensajes
                ChatroomModel = chatroomSnapshot.getValue(ChatroomModel::class.java)!!
            }else{
                //No existe el chatroom
                //Crear chatroom
                ChatroomModel = ChatroomModel(chatroomId!!, userIds, Timestamp.now(),"")
                getChatroomReferece(chatroomId!!).setValue(ChatroomModel)
            }
        }
    }
    private fun getChatroomId(userId1 : String, userId2 : String) : String {
        if(userId1.hashCode()<userId2.hashCode()){
            return userId1+"_"+userId2
        }else{
            return userId2+"_"+userId1
        }
    }

    private fun getChatroomReferece(chatroomId : String) : DatabaseReference {
        return FirebaseDatabase.getInstance().getReference("chatrooms").child(chatroomId)
    }
}