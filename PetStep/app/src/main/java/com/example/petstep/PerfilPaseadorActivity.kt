package com.example.petstep

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petstep.databinding.ActivityPerfilPaseadorBinding

class PerfilPaseadorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPerfilPaseadorBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityPerfilPaseadorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonVerHistorial.setOnClickListener {
            startActivity(Intent(this, IniciarSesionActivity::class.java))
        }
    }
}