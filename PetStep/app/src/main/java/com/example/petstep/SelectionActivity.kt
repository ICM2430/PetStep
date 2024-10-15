package com.example.petstep

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivitySelectionBinding

class SelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.confirmar.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            val rol = if (binding.roldueno.isChecked) "dueno" else "paseador"
            intent.putExtra("rol", rol)
            startActivity(intent)
        }
    }
}