package com.example.petstep

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petstep.databinding.ActivityChatBinding
import com.example.petstep.model.MyUser

class ChatActivity : AppCompatActivity() {

    lateinit var binding : ActivityChatBinding
    lateinit var UserChat : MyUser
    private var userId: String? = null
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //userId = intent.getStringExtra("USER_ID")
        //username = intent.getStringExtra("USER_NAME")
        binding.otherUserName.text = username


    }
}