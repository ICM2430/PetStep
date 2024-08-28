package com.example.petstep

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petstep.databinding.ActivityHomeWalkerBinding

class HomeWalkerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeWalkerBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding=ActivityHomeWalkerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.casa.setOnClickListener {
            startActivity(Intent(this, HomeWalkerActivity::class.java))
        }
        binding.paseo.setOnClickListener {
            startActivity(Intent(this, PeticionesActivity::class.java))
        }
        binding.perfil.setOnClickListener {
            startActivity(Intent(this, PerfilPaseadorActivity::class.java))
        }
    }
}