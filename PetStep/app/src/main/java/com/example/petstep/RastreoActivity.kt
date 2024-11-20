package com.example.petstep

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petstep.databinding.ActivityRastreoBinding

class RastreoActivity : AppCompatActivity() {

    lateinit var binding : ActivityRastreoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRastreoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button4.setOnClickListener{
            Log.i("ChatActivity", "Entrando")
            startActivity(Intent(baseContext,ChatActivity::class.java))
        }
    }
}