package com.example.petstep

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.petstep.databinding.ActivityIniciarSesionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.Executor

class IniciarSesionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIniciarSesionBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userRef: DatabaseReference
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    val TAG = "FIREBASE_APP"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIniciarSesionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        if (auth.currentUser != null) {
            // Entrar dependiendo su rol
            Log.d(TAG, "User already logged in, updating UI.")
            updateUI(auth.currentUser)
        }

        binding.registrese.setOnClickListener {
            startActivity(Intent(baseContext, SelectionActivity::class.java))
        }

        binding.ingresarButtom.setOnClickListener {
            val email = binding.correo.text.toString()
            val password = binding.contrasena.text.toString()

            Log.d(TAG, "Botón iniciar sesión presionado")
            if (validateForm(email, password)) {
                Log.d(TAG, "Formulario validado, intentando iniciar sesión.")
                loginUser(email, password)
            }
        }

        setupBiometricAuthentication()
        binding.biometricButton.setOnClickListener {
            val biometricManager = BiometricManager.from(this)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                Log.d(TAG, "Biometric authentication available, prompting user.")
                biometricPrompt.authenticate(promptInfo)
            } else {
                Log.e(TAG, "Biometric authentication is not available or not set up.")
                Toast.makeText(this, "Biometric authentication is not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBiometricAuthentication() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Log.d(TAG, "App can authenticate using biometrics.")
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Log.e(TAG, "No biometric hardware available.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Log.e(TAG, "Biometric features are currently unavailable.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Log.e(TAG, "The user hasn't enrolled any biometric credentials.")
        }

        val executor: Executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Biometric authentication succeeded, attempting Firebase login with saved credentials.")
                // Llama a la función para iniciar sesión en Firebase después de la autenticación exitosa.
                val sharedPref = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
                val savedEmail = sharedPref.getString("email", null)
                val savedPassword = sharedPref.getString("password", null)

                if (savedEmail != null && savedPassword != null) {
                    Log.d(TAG, "Saved credentials found, logging in.")
                    loginUser(savedEmail, savedPassword, isBiometric = true)
                } else {
                    Log.e(TAG, "No saved credentials found.")
                    Toast.makeText(applicationContext, "No saved credentials available for biometric login", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación biométrica")
            .setSubtitle("Inicia sesión con tu huella dactilar")
            .setNegativeButtonText("Usar contraseña")
            .build()
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val userId = currentUser.uid
            val duenoRef = database.getReference("users/duenos").child(userId)
            val paseadorRef = database.getReference("users/paseadores").child(userId)

            Log.d(TAG, "Attempting to retrieve user role for userId: $userId")

            duenoRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Log.d(TAG, "User is a 'dueno', navigating to HomeOwnerActivity.")
                        navigateToActivity(HomeOwnerActivity::class.java)
                    } else {
                        paseadorRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    Log.d(TAG, "User is a 'paseador', navigating to HomeWalkerActivity.")
                                    navigateToActivity(HomeWalkerActivity::class.java)
                                } else {
                                    Log.d(TAG, "User role not found")
                                    Toast.makeText(baseContext, "User role not found", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.e(TAG, "Error retrieving 'paseador' role: ${databaseError.message}")
                            }
                        })
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Error retrieving 'dueno' role: ${databaseError.message}")
                }
            })
        }
    }

    private fun loginUser(email: String, password: String, isBiometric: Boolean = false) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "Login successful, updating UI.")
                // Guardar credenciales para autenticación biométrica solo si no es autenticación biométrica
                if (!isBiometric) {
                    val sharedPref = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("email", email)
                        putString("password", password)
                        apply()
                    }
                    Log.d(TAG, "Credentials saved for biometric authentication.")
                }
                updateUI(auth.currentUser) // never null if authentication is successful
            } else {
                val message = it.exception?.message
                Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Login error: $message")
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

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }
}
