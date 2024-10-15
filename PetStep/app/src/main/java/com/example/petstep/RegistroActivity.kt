package com.example.petstep

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.petstep.databinding.ActivityRegistroBinding
import com.example.petstep.model.MyUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var realtimeDb: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        realtimeDb = FirebaseDatabase.getInstance()

        val rol = intent.getStringExtra("rol") ?: "paseador" // Default to "user" if no role is provided

        binding.registroButtom.setOnClickListener {
            val nombre = binding.nombre.text.toString()
            val apellido = binding.apellido.text.toString()
            val telefono = binding.telefono.text.toString()
            val correo = binding.correo.text.toString()
            val contrasena = binding.contrasena.text.toString()
            val contrasena2 = binding.contrasena2.text.toString()

            if (validateForm(nombre, apellido, telefono, correo, contrasena, contrasena2)) {
                registerUser(nombre, apellido, telefono, correo, contrasena, rol)
            }
        }

        binding.iniciarsesionbutton.setOnClickListener {
            startActivity(Intent(baseContext, IniciarSesionActivity::class.java))
        }
    }

    private fun validateForm(nombre: String, apellido: String, telefono: String, correo: String, contrasena: String, contrasena2: String): Boolean {
        var valid = true

        if (nombre.isEmpty()) {
            binding.nombre.error = "Required!"
            valid = false
        }
        if (apellido.isEmpty()) {
            binding.apellido.error = "Required!"
            valid = false
        }
        if (telefono.isEmpty()) {
            binding.telefono.error = "Required!"
            valid = false
        }
        if (correo.isEmpty()) {
            binding.correo.error = "Required!"
            valid = false
        } else if (!validEmailAddress(correo)) {
            binding.correo.error = "Invalid email address"
            valid = false
        }
        if (contrasena.isEmpty()) {
            binding.contrasena.error = "Required!"
            valid = false
        } else if (contrasena.length < 6) {
            binding.contrasena.error = "Password should be at least 6 characters long!"
            valid = false
        }
        if (contrasena != contrasena2) {
            binding.contrasena2.error = "Passwords do not match!"
            valid = false
        }

        return valid
    }

    private fun validEmailAddress(email: String): Boolean {
        val regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        return email.matches(regex.toRegex())
    }

    private fun registerUser(nombre: String, apellido: String, telefono: String, correo: String, contrasena: String, rol: String) {
        auth.createUserWithEmailAndPassword(correo, contrasena).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = MyUser(nombre, apellido, telefono, correo, contrasena, rol)
                val userId = auth.currentUser!!.uid

                // Save user to Firestore
                db.collection("users").document(userId).set(user).addOnSuccessListener {
                    Toast.makeText(baseContext, "User registered successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(baseContext, IniciarSesionActivity::class.java))
                }.addOnFailureListener {
                    Toast.makeText(baseContext, "Failed to register user", Toast.LENGTH_SHORT).show()
                }

                // Save user to Realtime Database
                val userRolePath = if (rol == "dueno") "duenos" else "paseadores"
                realtimeDb.getReference("users/$userRolePath").child(userId).setValue(user).addOnSuccessListener {
                    Toast.makeText(baseContext, "User saved to Realtime Database", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(baseContext, IniciarSesionActivity::class.java))
                }.addOnFailureListener {
                    Toast.makeText(baseContext, "Failed to save user to Realtime Database", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(baseContext, "Failed to register user: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}