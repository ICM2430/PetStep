package com.example.petstep

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petstep.databinding.ActivityHomeBinding
import com.example.petstep.databinding.ActivitySelectionBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonBuscarPaseadores.setOnClickListener {
            startActivity(Intent(this, PaseoActivity::class.java))
        }
        binding.buttonCalificarServicio.setOnClickListener {
            startActivity(Intent(this, CalificarActivity::class.java))
        }
    }
}