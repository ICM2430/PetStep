package com.example.petstep

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petstep.databinding.ActivityMascotasBinding

class MascotasActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMascotasBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMascotasBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}