package com.example.petstep

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.petstep.databinding.ActivityPerfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PerfilActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPerfilBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid
        userRef = database.getReference("users/duenos").child(userId!!)

        binding.cerrarSesion.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, IniciarSesionActivity::class.java))
            finish()
        }
        binding.botonEditarFoto.setOnClickListener {
            startActivity(Intent(this, ProfilePhotoActivity::class.java))
        }

        // Fetch user data from Firebase Realtime Database
        fetchUserData()

        // Check if there is an image URI passed from ProfilePhotoActivity
        val imageUri = intent.getStringExtra("imageUri")
        if (imageUri != null) {
            val uri = Uri.parse(imageUri)
            Glide.with(this).load(uri).into(binding.userPhoto)
        }
    }

    private fun fetchUserData() {
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val nombre = dataSnapshot.child("nombre").getValue(String::class.java) ?: "Name not available"
                    val apellido = dataSnapshot.child("apellido").getValue(String::class.java) ?: "Last name not available"
                    val correo = dataSnapshot.child("correo").getValue(String::class.java) ?: "Email not available"
                    val telefono = dataSnapshot.child("telefono").getValue(String::class.java) ?: "Phone number not available"

                    binding.username.text = "$nombre $apellido"
                    binding.correoEditar.text = correo
                    binding.numeroEditar.text = telefono
                } else {
                    // Handle case where user data does not exist
                    binding.username.text = "Name not available"
                    binding.correoEditar.text = "Email not available"
                    binding.numeroEditar.text = "Phone number not available"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error
            }
        })
    }
}