package com.example.petstep

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petstep.databinding.ActivityHomeWalkerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeWalkerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeWalkerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userRef: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)

        binding=ActivityHomeWalkerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid
        userRef = database.getReference("users/paseadores").child(userId!!)

        binding.casa.setOnClickListener {
            startActivity(Intent(this, HomeWalkerActivity::class.java))
        }
        binding.paseo.setOnClickListener {
            startActivity(Intent(this, PeticionesActivity::class.java))
        }
        binding.perfil.setOnClickListener {
            startActivity(Intent(this, PerfilPaseadorActivity::class.java))
        }
        fetchUserData()

    }
    private fun fetchUserData() {
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val nombre = dataSnapshot.child("nombre").getValue(String::class.java) ?: "Name not available"


                    binding.saludo.text = "Hola $nombre"
                } else {
                    // Handle case where user data does not exist
                    binding.saludo.text = "Hola"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error
            }
        })
    }
}