package com.example.petstep

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.petstep.databinding.ActivityIniciarSesionBinding

class IniciarSesionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIniciarSesionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIniciarSesionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registrese.setOnClickListener {
            startActivity(Intent(baseContext,SelectionActivity::class.java))
        }
        binding.ingresarButtom.setOnClickListener {
            startActivity(Intent(baseContext,HomeOwnerActivity::class.java))
        }
    }
}