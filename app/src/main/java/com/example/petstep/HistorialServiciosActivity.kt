package com.example.petstep

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petstep.databinding.ActivityHistorialServiciosBinding

class HistorialServiciosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialServiciosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialServiciosBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}