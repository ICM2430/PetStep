package com.example.petstep

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivityIniciarSesionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class IniciarSesionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIniciarSesionBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userRef: DatabaseReference
    val TAG = "FIREBASE_APP"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIniciarSesionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.registrese.setOnClickListener {
            startActivity(Intent(baseContext, SelectionActivity::class.java))
        }
        binding.ingresarButtom.setOnClickListener {
            val email = binding.correo.text.toString()
            val password = binding.contrasena.text.toString()

            if (validateForm(email, password)) {
                loginUser(email, password)
            }
        }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val userId = currentUser.uid
            val duenoRef = database.getReference("users/duenos").child(userId)
            val paseadorRef = database.getReference("users/paseadores").child(userId)

            duenoRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        startActivity(Intent(this@IniciarSesionActivity, HomeOwnerActivity::class.java))
                    } else {
                        paseadorRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    startActivity(Intent(this@IniciarSesionActivity, HomeWalkerActivity::class.java))
                                } else {
                                    Toast.makeText(baseContext, "User role not found", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.e(TAG, "Error: ${databaseError.message}")
                            }
                        })
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Error: ${databaseError.message}")
                }
            })
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                updateUI(auth.currentUser) // never null if authentication is successful
            } else {
                val message = it.exception?.message
                Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error: $message")
                binding.correo.text.clear()
                binding.contrasena.text.clear()
            }
        }
    }

    private fun validateForm(email: String, password: String): Boolean {
        var valid = false
        if (email.isEmpty()) {
            binding.correo.error = "Required!"
        } else if (!validEmailAddress(email)) {
            binding.correo.error = "Invalid email address"
        } else if (password.isEmpty()) {
            binding.contrasena.error = "Required!"
        } else if (password.length < 6) {
            binding.contrasena.error = "Password should be at least 6 characters long!"
        } else {
            valid = true
        }
        return valid
    }

    private fun validEmailAddress(email: String): Boolean {
        val regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        return email.matches(regex.toRegex())
    }
}