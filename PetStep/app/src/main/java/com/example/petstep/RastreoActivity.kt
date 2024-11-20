package com.example.petstep

import android.os.Bundle
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

        }
    }
}