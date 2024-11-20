package com.example.petstep

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
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
import com.google.android.gms.maps.model.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth  // Agregar este import

class MapsActivityPaseador : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsPaseadorBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentMarker: Marker? = null
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()  // Agregar esta línea
    private var petMarker: Marker? = null
    private var petLocation: LatLng? = null
    private lateinit var polyline: Polyline
    private var lastLocationUpdate = 0L
    private val MIN_UPDATE_INTERVAL = 30000L // 30 segundos entre actualizaciones

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

        // Cargar ubicación de la mascota antes de iniciar actualizaciones
        loadPetLocation(requestId) {
            startLocationUpdates(requestId)
        }

        // Agregar botón para finalizar servicio
        binding.finishServiceButton.setOnClickListener {
            finishService(requestId)
        }
    }

    private fun loadPetLocation(requestId: String, onComplete: () -> Unit) {
        database.getReference("walkRequests")
            .child(requestId)
            .get()
            .addOnSuccessListener { snapshot ->
                val ownerLat = snapshot.child("ownerLat").getValue(Double::class.java) ?: 0.0
                val ownerLng = snapshot.child("ownerLng").getValue(Double::class.java) ?: 0.0
                petLocation = LatLng(ownerLat, ownerLng)
                
                if (::googleMap.isInitialized) {
                    showPetMarker()
                }
                onComplete()
            }
    }

    private fun showPetMarker() {
        petLocation?.let { location ->
            petMarker?.remove()
            petMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Ubicación de la mascota")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )
            adjustMapZoom()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        
        // Si ya tenemos la ubicación de la mascota, mostrarla
        if (petLocation != null) {
            showPetMarker()
        }
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
        
        // Actualizar marcador del paseador
        if (currentMarker == null) {
            currentMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(currentLatLng)
                    .title("Tu ubicación")
            )
        } else {
            currentMarker!!.position = currentLatLng
        }

        // Dibujar o actualizar la ruta
        petLocation?.let { petLoc ->
            drawRoute(currentLatLng, petLoc)
        }

        // Ajustar zoom solo si es la primera ubicación
        if (currentMarker == null) {
            adjustMapZoom()
        }
    }

    private fun drawRoute(start: LatLng, end: LatLng) {
        val points = listOf(start, end)
        if (!::polyline.isInitialized) {
            polyline = googleMap.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .color(Color.BLUE)
                    .width(5f)
            )
        } else {
            polyline.points = points
        }
    }

    private fun adjustMapZoom() {
        val builder = LatLngBounds.Builder()
        
        // Añadir puntos disponibles al bounds
        currentMarker?.position?.let { builder.include(it) }
        petMarker?.position?.let { builder.include(it) }
        
        // Si tenemos ambos puntos, ajustar el zoom
        if (currentMarker != null && petMarker != null) {
            val bounds = builder.build()
            val padding = 100 // padding en píxeles
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            googleMap.animateCamera(cameraUpdate)
        }
    }

    private fun updateLocationInFirebase(location: Location, requestId: String) {
        val currentTime = System.currentTimeMillis()
        
        // Verificar si ha pasado suficiente tiempo desde la última actualización
        if (currentTime - lastLocationUpdate < MIN_UPDATE_INTERVAL) {
            return
        }

        val geoPoint = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to currentTime
        )
        
        // Guardar en walkRequests/[requestId]/route/[timestamp]
        database.getReference("walkRequests")
            .child(requestId)
            .child("route")
            .child(currentTime.toString())
            .setValue(geoPoint)
            .addOnSuccessListener {
                lastLocationUpdate = currentTime
                Log.d(TAG, "Ubicación guardada: lat=${location.latitude}, lng=${location.longitude}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar ubicación: ", e)
            }
    }

    private fun finishService(requestId: String) {
        val updates = hashMapOf<String, Any?>(
            "walkRequests/$requestId/status" to "completed",
            "users/paseadores/${auth.currentUser!!.uid}/activeServiceId" to null,
            "users/paseadores/${auth.currentUser!!.uid}/activeService" to false
        )

        database.reference.updateChildren(updates)
            .addOnSuccessListener {
                println("DEBUG: Servicio finalizado")
                Toast.makeText(this, "Servicio finalizado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                println("ERROR: Fallo al finalizar servicio: ${e.message}")
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
