package com.example.petstep

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivityHomeWalkerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomeWalkerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeWalkerBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val walkersRef = database.getReference("walkers")

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
        val userId = auth.currentUser?.uid
        val price = binding.precioCop.text.toString()
        val workZone = binding.zonaa.text.toString()

        println("Debug - SaveWalkerInfo:")
        println("UserId: $userId")
        println("Price: $price")
        println("WorkZone: $workZone")
        println("Is Disponible checked: ${binding.disponible.isChecked}")

        if (price.toDoubleOrNull() == null || workZone.isEmpty()) {
            println("Error: Invalid price or empty work zone")
            Toast.makeText(this, "Ingrese precio y zona de trabajo", Toast.LENGTH_SHORT).show()
            return
        }

        val walkerData = mapOf(
            "precioPorHora" to price.toDouble(),
            "workZone" to workZone,
            "available" to binding.disponible.isChecked
        )

        println("Attempting to save data to Realtime Database: $walkerData")

        walkersRef.child(userId!!).updateChildren(walkerData)
            .addOnSuccessListener {
                println("Success: Data saved to Realtime Database")
                Toast.makeText(this, "Información actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                println("Error saving to Realtime Database: ${e.message}")
                Toast.makeText(this, "Error al guardar información: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    private fun updateAvailability(isAvailable: Boolean) {
        val userId = auth.currentUser?.uid ?: return

        walkersRef.child(userId).child("available").setValue(isAvailable)
            .addOnSuccessListener {
                Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar estado", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val nombre = snapshot.child("nombre").getValue(String::class.java) ?: "Usuario"
                binding.saludo.text = "Hola $nombre"
            }
            .addOnFailureListener {
                binding.saludo.text = "Hola"
            }
    }
}
