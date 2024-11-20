package com.example.petstep

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.petstep.databinding.ActivityProfilePhotoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

class ProfilePhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilePhotoBinding
    private lateinit var uri: Uri
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userRef: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(), ActivityResultCallback {
            if (it != null) {
                loadImage(it)
                saveImageUri(it)
                sendImageUri(it)
            }
        }
    )

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
        ActivityResultCallback {
            if (it) {
                loadImage(uri)
                saveImageUri(uri)
                sendImageUri(uri)
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilePhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference
        val userId = auth.currentUser?.uid
        if (userId != null) {
            userRef = database.getReference("users/duenos").child(userId)
        } else {
            // Handle error: user not logged in
            finish()
        }

        binding.buttonAbrirGaleria.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.buttonTomarFoto.setOnClickListener {
            val file = File(getFilesDir(), "profilePic")
            uri = FileProvider.getUriForFile(baseContext, baseContext.packageName + ".fileprovider", file)
            cameraLauncher.launch(uri)
        }

        binding.buttonConfirmar.setOnClickListener {
            if (::uri.isInitialized) {
                sendImageUri(uri)
            } else {
                updateUserProfile(null)
            }
        }
    }

    private fun loadImage(uri: Uri) {
        val imageStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(imageStream)
        binding.imageFotoperfil.setImageBitmap(bitmap)
    }

    private fun saveImageUri(uri: Uri) {
        val sharedPreferences = getSharedPreferences("ProfilePhotoPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("imageUri", uri.toString())
        editor.apply()
    }

    private fun sendImageUri(uri: Uri) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val fileRef = storageRef.child("users-photos/owner/$userId.jpg")
            fileRef.putFile(uri).addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    userRef.child("profilePhotoUrl").setValue(downloadUri.toString()).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            updateUserProfile(downloadUri.toString())
                        } else {
                            // Handle error
                        }
                    }
                }.addOnFailureListener {
                    // Handle error
                }
            }.addOnFailureListener {
                // Handle error
            }
        } else {
            // Handle error: user not logged in
        }
    }

    private fun updateUserProfile(photoUrl: String?) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            userRef = database.getReference("users/duenos").child(userId)
            val userUpdates = mutableMapOf<String, Any>(
                "nombre" to binding.nombre.text.toString(),
                "apellido" to binding.apellido.text.toString(),
                "telefono" to binding.telefono.text.toString()
            )
            if (photoUrl != null) {
                userUpdates["profilePhotoUrl"] = photoUrl
            }
            userRef.updateChildren(userUpdates).addOnCompleteListener {
                if (it.isSuccessful) {
                    val intent = Intent(this, PerfilActivity::class.java)
                    startActivity(intent)
                } else {
                    // Handle error
                }
            }
        } else {
            // Handle error: user not logged in
        }
    }
}