package com.example.petstep

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.petstep.databinding.ActivityMapsPaseadorBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.FirebaseDatabase  // Añadir este import

class MapsActivityPaseador : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsPaseadorBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentMarker: Marker? = null
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsPaseadorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val requestId = intent.getStringExtra("requestId") ?: run {
            Toast.makeText(this, "ID de solicitud no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        startLocationUpdates(requestId)

        // Agregar botón para finalizar servicio
        binding.finishServiceButton.setOnClickListener {
            finishService(requestId)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
    }

    private fun startLocationUpdates(serviceId: String) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationOnMap(location)
                    updateLocationInFirebase(location, serviceId)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun updateLocationOnMap(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)
        if (currentMarker == null) {
            currentMarker = googleMap.addMarker(MarkerOptions().position(currentLatLng).title("Tu ubicación actual"))
        } else {
            currentMarker!!.position = currentLatLng
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
    }

    private fun updateLocationInFirebase(location: Location, requestId: String) {
        val locationData = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to System.currentTimeMillis()
        )
        
        database.getReference("active_services")
            .child(requestId)
            .child("currentLocation")
            .setValue(locationData)
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al actualizar ubicación: ", e)
            }
    }

    private fun finishService(requestId: String) {
        database.getReference("requests")
            .child(requestId)
            .child("status")
            .setValue("completed")
            .addOnSuccessListener {
                Toast.makeText(this, "Servicio finalizado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al finalizar servicio", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStop() {
        super.onStop()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "MapsActivityPaseador"
    }
}
