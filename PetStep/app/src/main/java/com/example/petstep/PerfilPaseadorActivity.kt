package com.example.petstep

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.petstep.databinding.ActivityPerfilPaseadorBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PerfilPaseadorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPerfilPaseadorBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilPaseadorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid
        userRef = database.getReference("users/paseadores").child(userId!!)

        binding.buttonVerHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialServiciosActivity::class.java))
        }
        binding.buttonCerrarSesion.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, IniciarSesionActivity::class.java))
            finish()
        }
        binding.editarFotoBoton.setOnClickListener {
            startActivity(Intent(this, ProfilePhotoPaseadorActivity::class.java))
        }
        binding.volverAlMenu.setOnClickListener {
            startActivity(Intent(this, HomeWalkerActivity::class.java))
        }

        // Load image URI from SharedPreferences
        loadImageUri()

        // Fetch user data from Firebase Realtime Database
        fetchUserData()
    }

    private fun fetchUserData() {
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val nombre = dataSnapshot.child("nombre").getValue(String::class.java) ?: "Name not available"
                    val apellido = dataSnapshot.child("apellido").getValue(String::class.java) ?: "Last name not available"
                    val correo = dataSnapshot.child("correo").getValue(String::class.java) ?: "Email not available"
                    val telefono = dataSnapshot.child("telefono").getValue(String::class.java) ?: "Phone number not available"

                    binding.textViewNombre.text = "$nombre $apellido"
                    binding.textViewCorreo.text = correo
                    binding.textViewTelefono.text = telefono
                    binding.textViewCalificacion.text = "4.9/5"
                    binding.textViewNumServicios.text = "2"
                } else {
                    // Handle case where user data does not exist
                    binding.textViewNombre.text = "Name not available"
                    binding.textViewCorreo.text = "Email not available"
                    binding.textViewTelefono.text = "Phone number not available"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error
            }
        })
    }

    private fun loadImageUri() {
        val sharedPreferences = getSharedPreferences("ProfilePhotoPaseadorPrefs", Context.MODE_PRIVATE)
        val imageUri = sharedPreferences.getString("imageUri", null)
        if (imageUri != null) {
            val uri = Uri.parse(imageUri)
            Glide.with(this).load(uri).into(binding.imageViewPerfil)
        }
    }
}