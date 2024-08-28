package com.example.petstep


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.petstep.databinding.ActivityRegistroBinding

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            startActivity(Intent(baseContext,IniciarSesionActivity::class.java))
        }

        binding.registroButtom.setOnClickListener {
            startActivity(Intent(baseContext,ProfilePhotoActivity::class.java))
        }
    }
}