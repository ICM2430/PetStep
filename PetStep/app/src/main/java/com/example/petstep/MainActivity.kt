package com.example.petstep

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.petstep.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.iniciarsesionbutton.setOnClickListener {
            startActivity(Intent(baseContext,IniciarSesionActivity::class.java))
        }

        binding.registrarseButtom.setOnClickListener {
            startActivity(Intent(baseContext,SelectionActivity::class.java))
        }
    }
}