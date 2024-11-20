package com.example.petstep

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivityHomeWalkerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeWalkerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeWalkerBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeWalkerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()

        binding.saveInfoButton.setOnClickListener {
            saveWalkerInfo()
        }

        binding.disponible.setOnClickListener {
            updateAvailability(true)
        }

        binding.noDisponible.setOnClickListener {
            updateAvailability(false)
        }

        fetchUserData()
    }

    private fun setupNavigation() {
        binding.casa.setOnClickListener {
            startActivity(Intent(this, HomeWalkerActivity::class.java))
        }

        binding.paseo.setOnClickListener {
            startActivity(Intent(this, PeticionesActivity::class.java))
        }

        binding.perfil.setOnClickListener {
            startActivity(Intent(this, PerfilPaseadorActivity::class.java))
        }

        binding.ubiActual.setOnClickListener {
            startActivity(Intent(this, MapsActivityPaseador::class.java))
        }
    }

    private fun saveWalkerInfo() {
        val userId = auth.currentUser?.uid ?: return
        val price = binding.precioCop.text.toString().toDoubleOrNull()
        val workZone = binding.zonaa.text.toString()

        if (price == null || workZone.isEmpty()) {
            Toast.makeText(this, "Ingrese precio y zona de trabajo", Toast.LENGTH_SHORT).show()
            return
        }

        val walkerData = hashMapOf(
            "precioPorHora" to price,
            "workZone" to workZone,
            "available" to binding.disponible.isChecked
        )

        firestore.collection("walkers").document(userId)
            .set(walkerData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Información actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar información: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace() // Log del error para diagnosticar
            }
    }


    private fun updateAvailability(isAvailable: Boolean) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("walkers").document(userId)
            .update("available", isAvailable)
            .addOnSuccessListener {
                Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar estado", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val nombre = document.getString("nombre") ?: "Usuario"
                binding.saludo.text = "Hola $nombre"
            }
            .addOnFailureListener {
                binding.saludo.text = "Hola"
            }
    }
}
