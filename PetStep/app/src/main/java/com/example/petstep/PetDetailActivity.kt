// PetDetailActivity.kt
package com.example.petstep

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivityPetDetailBinding
import com.example.petstep.model.MyPet
import com.squareup.picasso.Picasso

class PetDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pet = intent.getParcelableExtra<MyPet>("pet")

        pet?.let {
            binding.petNameTextView.text = it.nombre
            binding.petBreedTextView.text = it.raza
            binding.petAgeTextView.text = it.edad
            binding.petWeightTextView.text = it.peso
            Picasso.get().load(it.photoUrl).into(binding.petPhotoImageView)
        }
    }
}