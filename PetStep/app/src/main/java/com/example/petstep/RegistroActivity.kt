package com.example.petstep


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.petstep.databinding.ActivityRegistroBinding
import com.google.firebase.auth.FirebaseAuth

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {

        auth = FirebaseAuth.getInstance()

        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Botón para iniciar sesión
        binding.iniciarsesionbutton.setOnClickListener {
            startActivity(Intent(baseContext,IniciarSesionActivity::class.java))
        }

        //Botón para registrar usuario
        binding.registroButtom.setOnClickListener {
            startActivity(Intent(baseContext,ProfilePhotoActivity::class.java))
        }

        val role = intent.getStringExtra("rol")
    }
}