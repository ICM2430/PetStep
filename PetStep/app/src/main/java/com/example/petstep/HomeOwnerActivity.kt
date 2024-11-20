package com.example.petstep

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivityHomeOwnerBinding

class HomeOwnerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeOwnerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonBuscarPaseadores.setOnClickListener {
            startActivity(Intent(this, PaseoActivity::class.java))
        }
        binding.buttonCalificarServicio.setOnClickListener {
            startActivity(Intent(this, CalificarActivity::class.java))
        }
        binding.mascotasAcceso.setOnClickListener {
            startActivity(Intent(this, MascotasActivity::class.java))
        }
        binding.perfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
        binding.buttonServicioActual.setOnClickListener {
            startActivity(Intent(this, RastreoActivity::class.java))
        }
    }


}