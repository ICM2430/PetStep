package com.example.petstep

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petstep.databinding.ActivityCalificarBinding

class CalificarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalificarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalificarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*
        val starIds = listOf(
            binding.star1, binding.star2, binding.star3, binding.star4, binding.star5,
            binding.star6, binding.star7, binding.star8, binding.star9, binding.star10,
            binding.star11, binding.star12, binding.star13, binding.star14, binding.star15
        )

        val newDrawable = R.drawable.estrellaamarilla // Replace with your drawable

        starIds.forEach { star ->
            star.setOnClickListener {
                (it as ImageView).setImageResource(newDrawable)
            }
        }
        */

    }

}