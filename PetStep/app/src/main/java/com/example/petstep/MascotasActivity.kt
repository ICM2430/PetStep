package com.example.petstep

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petstep.adapters.PetAdapter
import com.example.petstep.databinding.ActivityMascotasBinding
import com.example.petstep.adapters.com.example.petstep.model.Pet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MascotasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMascotasBinding
    private val pets = mutableListOf<Pet>()
    private val petAdapter = PetAdapter(pets) { pet -> deletePet(pet.id) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMascotasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        fetchPets()

        binding.addPetButton.setOnClickListener {
            addPet()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MascotasActivity)
            adapter = petAdapter
        }
    }

    private fun fetchPets() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId).collection("pets")
            .get()
            .addOnSuccessListener { documents ->
                pets.clear()
                for (document in documents) {
                    val pet = document.toObject(Pet::class.java).copy(id = document.id)
                    pets.add(pet)
                }
                petAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar las mascotas.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addPet() {
        val newPet = Pet("Nuevo nombre", "Tipo", )

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId).collection("pets").add(newPet)
            .addOnSuccessListener {
                Toast.makeText(this, "Mascota añadida.", Toast.LENGTH_SHORT).show()
                fetchPets()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al añadir mascota.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deletePet(petId: String) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId).collection("pets").document(petId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Mascota eliminada.", Toast.LENGTH_SHORT).show()
                fetchPets() // Recargar mascotas
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar mascota.", Toast.LENGTH_SHORT).show()
            }
    }
}
