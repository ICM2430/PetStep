package com.example.petstep

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.petstep.databinding.ActivityProfilePhotoPaseadorBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class ProfilePhotoPaseadorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilePhotoPaseadorBinding
    private lateinit var uri: Uri
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userRef: DatabaseReference

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
        binding = ActivityProfilePhotoPaseadorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid

        binding.buttonAbrirGaleria.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.buttonTomarFoto.setOnClickListener {
            val file = File(getFilesDir(), "profilePic")
            uri = FileProvider.getUriForFile(baseContext, baseContext.packageName + ".fileprovider", file)
            cameraLauncher.launch(uri)
        }

        binding.buttonConfirmar.setOnClickListener {
            startActivity(Intent(baseContext, IniciarSesionActivity::class.java))
        }
    }

    private fun loadImage(uri: Uri) {
        val imageStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(imageStream)
        binding.imageFotoperfil.setImageBitmap(bitmap)
    }

    private fun saveImageUri(uri: Uri) {
        val sharedPreferences = getSharedPreferences("ProfilePhotoPaseadorPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("imageUri", uri.toString())
        editor.apply()
    }

    private fun sendImageUri(uri: Uri) {
        uploadImageToFirebase(uri)
        val intent = Intent(this, PerfilPaseadorActivity::class.java)
        startActivity(intent)
    }


    private fun uploadImageToFirebase(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("profilePhotos/$userId.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveImageUri(downloadUri)
                    Toast.makeText(this, "Imagen subida exitosamente.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al subir la imagen.", Toast.LENGTH_SHORT).show()
            }
    }

}