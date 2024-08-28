package com.example.petstep

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petstep.databinding.ActivityPeticionesBinding

class PeticionesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPeticionesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityPeticionesBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}