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
            if (binding.roldueno.isChecked) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else if (binding.rolpaseador.isChecked) {
                startActivity(Intent(this, HomeWalkerActivity::class.java))
            }
        }
        /*


         */

    }
}