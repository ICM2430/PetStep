package com.example.petstep

import android.content.Intent
import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.petstep.databinding.ActivityIniciarSesionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class IniciarSesionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIniciarSesionBinding
    private lateinit var auth: FirebaseAuth
    val TAG = "FIREBASE_APP"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIniciarSesionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()



        binding.registrese.setOnClickListener {
            startActivity(Intent(baseContext,SelectionActivity::class.java))
        }
        binding.ingresarButtom.setOnClickListener {

            val email = binding.correo.text.toString()
            val password = binding.contrasena.text.toString()

            if(validateForm(email,password)){
                //API Firebase
                loginUser(email,password)

            }
            //startActivity(Intent(baseContext,HomeOwnerActivity::class.java))
        }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if(currentUser!= null){
            val intent = Intent(this, HomeWalkerActivity::class.java)
            intent.putExtra("email", currentUser.email)
            startActivity(intent)
        }

    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener{
            if(it.isSuccessful){
                updateUI(auth.currentUser) //nunca nulo si la autenticación es exitosa
            }else{
                val message = it.exception?.message
                Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error: $message")
                binding.correo.text.clear()
                binding.contrasena.text.clear()
            }
        }
    }


    private fun validateForm(email : String, password: String) : Boolean {
        var valid = false
        if (email.isEmpty()) {
            binding.correo.setError("Required!")
        } else if (!validEmailAddress(email)) {
            binding.correo.setError("Invalid email address")
        } else if (password.isEmpty()) {
            binding.contrasena.setError("Required!")
        } else if (password.length < 6){
            binding.contrasena.setError("Password should be at least 6 characters long!")
        }else {
            valid = true
        }
        return valid
    }

    private fun validEmailAddress(email:String):Boolean{
        val regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        return email.matches(regex.toRegex())
    }
}