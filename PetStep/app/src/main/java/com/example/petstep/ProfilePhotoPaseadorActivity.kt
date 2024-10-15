package com.example.petstep

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.petstep.databinding.ActivityProfilePhotoPaseadorBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
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
                sendImageUri(it)
            }
        }
    )

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
        ActivityResultCallback {
            if (it) {
                loadImage(uri)
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

    private fun sendImageUri(uri: Uri) {
        val intent = Intent(this, PerfilPaseadorActivity::class.java)
        intent.putExtra("imageUri", uri.toString())
        startActivity(intent)
    }
}