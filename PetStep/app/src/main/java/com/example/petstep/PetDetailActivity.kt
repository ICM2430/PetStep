// PetDetailActivity.kt
package com.example.petstep

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivityPetDetailBinding
import com.example.petstep.model.MyPet
import com.squareup.picasso.Picasso

class PetDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PetDetailActivity", "onCreate called")
        binding = ActivityPetDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pet = intent.getParcelableExtra<MyPet>("pet")
        Log.d("PetDetailActivity", "Received pet: $pet")

        if (pet != null) {
            binding.petNameTextView.text = "Nombre: " + pet.nombre
            binding.petBreedTextView.text = "Raza: " + pet.raza
            binding.petAgeTextView.text = "Edad: " + pet.edad
            binding.petWeightTextView.text = "Peso: " + pet.peso
            Picasso.get().load(pet.photoUrl).into(binding.petPhotoImageView)
        } else {
            Log.e("PetDetailActivity", "Pet is null")
            finish() // Close the activity if pet is null
        }

        binding.edit.setOnClickListener {
            val petId = intent.getStringExtra("petId")
            Log.d("PetDetailActivity", "Edit button clicked, petId: $petId")
            if (petId != null) {
                val intent = Intent(this, EditPetActivity::class.java)
                intent.putExtra("petId", petId)
                startActivity(intent)
            } else {
                Log.e("PetDetailActivity", "Pet ID is null")
            }
        }
    }
}