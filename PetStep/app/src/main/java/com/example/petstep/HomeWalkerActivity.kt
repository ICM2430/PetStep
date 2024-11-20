package com.example.petstep

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivityHomeWalkerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.gms.common.api.Status

class HomeWalkerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeWalkerBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val walkersRef = database.getReference("walkers")
    private var selectedAddress: String = ""
    private var selectedLatLng: Pair<Double, Double>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeWalkerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Places
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }

        setupPlacesAutocomplete()
        setupNavigation()

        binding.saveInfoButton.setOnClickListener {
            saveWalkerInfo()
        }

        loadWalkerData()
        fetchUserData()
    }

    private fun setupNavigation() {
        binding.casa.setOnClickListener {
            startActivity(Intent(this, HomeWalkerActivity::class.java))
        }

        binding.paseo.setOnClickListener {
            startActivity(Intent(this, PeticionesActivity::class.java))
        }

        binding.perfil.setOnClickListener {
            startActivity(Intent(this, PerfilPaseadorActivity::class.java))
        }

        binding.ubiActual.setOnClickListener {
            startActivity(Intent(this, MapsActivityPaseador::class.java))
        }
    }

    private fun setupPlacesAutocomplete() {
        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment

        // Personalizar el hint del campo de búsqueda
        autocompleteFragment.setHint("Ingresa tu zona de trabajo")
        
        autocompleteFragment.setPlaceFields(listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        ))

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                selectedAddress = place.address ?: ""
                selectedLatLng = place.latLng?.let { Pair(it.latitude, it.longitude) }
            }

            override fun onError(status: Status) {
                Toast.makeText(applicationContext, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadWalkerData() {
        val userId = auth.currentUser?.uid ?: return
        walkersRef.child(userId).get()
            .addOnSuccessListener { snapshot ->
                val price = snapshot.child("precioPorHora").getValue(Double::class.java)
                val workZone = snapshot.child("workZone").getValue(String::class.java)
                val available = snapshot.child("available").getValue(Boolean::class.java)

                binding.precioCop.setText(price?.toString() ?: "")
                // El AutocompleteSupportFragment maneja su propio estado
                if (available == true) {
                    binding.disponible.isChecked = true
                } else {
                    binding.noDisponible.isChecked = true
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveWalkerInfo() {
        val userId = auth.currentUser?.uid
        val price = binding.precioCop.text.toString()
        val workZone = selectedAddress // Usar la dirección seleccionada

        if (price.toDoubleOrNull() == null || workZone.isEmpty()) {
            Toast.makeText(this, "Ingrese precio y seleccione una zona de trabajo", Toast.LENGTH_SHORT).show()
            return
        }

        val walkerData = mapOf(
            "precioPorHora" to price.toDouble(),
            "workZone" to workZone,
            "workZoneLat" to (selectedLatLng?.first ?: 0.0),
            "workZoneLng" to (selectedLatLng?.second ?: 0.0),
            "available" to binding.disponible.isChecked
        )

        println("Attempting to save data to Realtime Database: $walkerData")

        walkersRef.child(userId!!).updateChildren(walkerData)
            .addOnSuccessListener {
                println("Success: Data saved to Realtime Database")
                Toast.makeText(this, "Información actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                println("Error saving to Realtime Database: ${e.message}")
                Toast.makeText(this, "Error al guardar información: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val nombre = snapshot.child("nombre").getValue(String::class.java) ?: "Usuario"
                binding.saludo.text = "Hola $nombre"
            }
            .addOnFailureListener {
                binding.saludo.text = "Hola"
            }
    }
}
