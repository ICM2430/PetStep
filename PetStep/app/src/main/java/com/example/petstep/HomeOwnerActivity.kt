package com.example.petstep

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivityHomeOwnerBinding
import android.widget.Toast
import android.location.LocationManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class HomeOwnerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeOwnerBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST = 1001
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        binding.buttonBuscarPaseadores.setOnClickListener {
            if (!isLocationEnabled()) {
                showGPSDisabledAlert()
            } else {
                checkLocationPermission()
            }
        }
        binding.buttonCalificarServicio.setOnClickListener {
            startActivity(Intent(this, CalificarActivity::class.java))
        }
        binding.mascotasAcceso.setOnClickListener {
            startActivity(Intent(this, MascotasActivity::class.java))
        }
        binding.perfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
        binding.buttonServicioActual.setOnClickListener {
            getActiveWalkerIdAndStartRastreoActivity()
        }
    }

    private fun getActiveWalkerIdAndStartRastreoActivity() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val databaseReference = Firebase.database.reference

        // Assuming the active service ID is stored under users/duenos/{userId}/activeServiceId
        databaseReference.child("users/duenos/$userId/activeServiceId").get()
            .addOnSuccessListener { snapshot ->
                val activeServiceId = snapshot.getValue(String::class.java)
                if (activeServiceId != null) {
                    // Now get the walkerId from the walkRequests node
                    databaseReference.child("walkRequests/$activeServiceId/walkerId").get()
                        .addOnSuccessListener { walkerSnapshot ->
                            val walkerId = walkerSnapshot.getValue(String::class.java)
                            if (walkerId != null && walkerId.isNotEmpty()) {
                                val intent = Intent(this, RastreoActivity::class.java)
                                intent.putExtra("walkerId", walkerId)
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, "No se encontró el ID del paseador.", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al obtener el walkerId: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "No tienes un servicio activo.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener el servicio activo: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showGPSDisabledAlert() {
        AlertDialog.Builder(this)
            .setMessage("El GPS está desactivado. ¿Deseas activarlo?")
            .setCancelable(false)
            .setPositiveButton("Sí") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocationAndOpenMap()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showPermissionExplanationDialog()
            }
            else -> {
                requestLocationPermission()
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setMessage("Necesitamos acceder a tu ubicación para mostrar paseadores cercanos")
            .setPositiveButton("Aceptar") { _, _ ->
                requestLocationPermission()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST
        )
    }

    private fun getCurrentLocationAndOpenMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        startActivity(Intent(this, PaseoActivity::class.java).apply {
                            putExtra("USER_LAT", location.latitude)
                            putExtra("USER_LNG", location.longitude)
                        })
                    } else {
                        requestLocationUpdates()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, 
                        "Error al obtener ubicación: ${it.message}", 
                        Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun requestLocationUpdates() {
        Toast.makeText(this, 
            "Esperando señal GPS. Por favor, asegúrate de estar en un lugar abierto.", 
            Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, 
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocationAndOpenMap()
                } else {
                    Toast.makeText(this, 
                        "Se requiere permiso de ubicación para mostrar paseadores cercanos",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}