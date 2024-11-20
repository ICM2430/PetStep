package com.example.petstep

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
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
import com.google.firebase.database.ServerValue
import java.util.Timer
import java.util.TimerTask
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import com.google.maps.android.PolyUtil

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
    private var isFirstLocation = true  // Añadir esta propiedad
    private var pendingPetMarker = false
    private var lastSavedLocation: Location? = null
    private val MIN_DISTANCE_CHANGE = 10f  // 10 metros
    private var serviceStatus = ""
    private var activeRequestId = ""
    private var timer: Timer? = null
    private var timerSeconds = 0
    private var remainingSeconds = 0
    private var routePoints = mutableListOf<LatLng>()
    private lateinit var geoApiContext: GeoApiContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsPaseadorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Instead of getting requestId from intent, get it from user's activeServiceId
        loadActiveServiceFromUser()

        binding.actionButton.setOnClickListener {
            when (serviceStatus) {
                "accepted" -> startWalk()
                "in_progress" -> finishService(activeRequestId)
            }
        }

        geoApiContext = GeoApiContext.Builder()
            .apiKey(BuildConfig.MAPS_API_KEY)
            .build()
    }

    private fun loadActiveServiceFromUser() {
        val userId = auth.currentUser!!.uid
        database.getReference("users/paseadores")
            .child(userId)
            .get()
            .addOnSuccessListener { userSnapshot ->
                val serviceId = userSnapshot.child("activeServiceId").getValue(String::class.java)
                if (serviceId != null) {
                    activeRequestId = serviceId
                    loadServiceStatus(serviceId)
                    loadPetLocation(serviceId) {
                        startLocationUpdates(serviceId)
                    }
                } else {
                    Toast.makeText(this, "No hay servicio activo", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading active service: ${e.message}")
                Toast.makeText(this, "Error al cargar servicio", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadServiceStatus(requestId: String) {
        database.getReference("walkRequests").child(requestId)
            .get()
            .addOnSuccessListener { snapshot ->
                serviceStatus = snapshot.child("status").getValue(String::class.java) ?: ""
                val durationMinutes = snapshot.child("duration").getValue(Int::class.java) ?: 0
                
                if (serviceStatus == "in_progress") {
                    val startTime = snapshot.child("startTime").getValue(Long::class.java) ?: 0L
                    remainingSeconds = calculateRemainingSeconds(startTime, durationMinutes)
                    startTimer()
                } else {
                    remainingSeconds = durationMinutes * 60
                }
                
                updateUIForStatus()
            }
    }

    private fun calculateRemainingSeconds(startTime: Long, durationMinutes: Int): Int {
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = ((currentTime - startTime) / 1000).toInt()
        val totalSeconds = durationMinutes * 60
        return maxOf(0, totalSeconds - elapsedSeconds)
    }

    private fun loadPetLocation(requestId: String, onComplete: () -> Unit) {
        database.getReference("walkRequests")
            .child(requestId)
            .get()
            .addOnSuccessListener { snapshot ->
                val ownerLat = snapshot.child("ownerLat").getValue(Double::class.java) ?: 0.0
                val ownerLng = snapshot.child("ownerLng").getValue(Double::class.java) ?: 0.0
                println("DEBUG: Pet location loaded - Lat: $ownerLat, Lng: $ownerLng")
                
                petLocation = LatLng(ownerLat, ownerLng)
                
                // Verificar si el mapa está listo o necesitamos esperar
                if (::googleMap.isInitialized || pendingPetMarker) {
                    showPetMarker()
                    pendingPetMarker = false
                }
                
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading pet location: ${e.message}")
            }
    }

    private fun showPetMarker() {
        petLocation?.let { location ->
            Log.d(TAG, "Adding pet marker at: ${location.latitude}, ${location.longitude}")
            petMarker?.remove()
            petMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Ubicación de la mascota")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )
            // Hacer zoom inicial solo si es la primera vez
            if (currentMarker == null) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        
        // Si ya tenemos la ubicación de la mascota, mostrarla inmediatamente
        if (petLocation != null) {
            showPetMarker()
        } else {
            pendingPetMarker = true
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
        
        if (currentMarker == null) {
            currentMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(currentLatLng)
                    .title("Tu ubicación")
            )
        } else {
            currentMarker!!.position = currentLatLng
        }

        when (serviceStatus) {
            "accepted" -> {
                // Obtener ruta por calles entre paseador y mascota
                petLocation?.let { petLoc ->
                    getDirectionsToLocation(currentLatLng, petLoc)
                }
            }
            "in_progress" -> {
                routePoints.add(currentLatLng)
                drawWalkingRoute()
            }
        }

        if (isFirstLocation) {
            isFirstLocation = false
            adjustMapZoom()
        }
    }

    private fun getDirectionsToLocation(origin: LatLng, destination: LatLng) {
        Thread {
            try {
                val result = DirectionsApi.newRequest(geoApiContext)
                    .mode(TravelMode.WALKING)
                    .origin("${origin.latitude},${origin.longitude}")
                    .destination("${destination.latitude},${destination.longitude}")
                    .await()

                runOnUiThread {
                    drawDirectionsRoute(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting directions: ${e.message}")
            }
        }.start()
    }

    private fun drawDirectionsRoute(result: DirectionsResult) {
        if (result.routes.isNotEmpty()) {
            val decodedPath = PolyUtil.decode(result.routes[0].overviewPolyline.encodedPath)
            
            if (!::polyline.isInitialized) {
                polyline = googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(decodedPath)
                        .color(Color.BLUE)
                        .width(5f)
                )
            } else {
                polyline.points = decodedPath
            }
        }
    }

    private fun drawDirectRoute(start: LatLng, end: LatLng) {
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

    private fun drawWalkingRoute() {
        if (!::polyline.isInitialized) {
            polyline = googleMap.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .color(Color.RED)
                    .width(5f)
            )
        } else {
            polyline.points = routePoints
        }
    }

    private fun loadExistingRoute(requestId: String) {
        database.getReference("walkRequests")
            .child(requestId)
            .child("route")
            .get()
            .addOnSuccessListener { snapshot ->
                routePoints.clear()
                snapshot.children.forEach { point ->
                    val lat = point.child("latitude").getValue(Double::class.java)
                    val lng = point.child("longitude").getValue(Double::class.java)
                    if (lat != null && lng != null) {
                        routePoints.add(LatLng(lat, lng))
                    }
                }
                if (routePoints.isNotEmpty() && serviceStatus == "in_progress") {
                    drawWalkingRoute()
                }
            }
    }

    private fun adjustMapZoom() {
        try {
            val builder = LatLngBounds.Builder()
            currentMarker?.position?.let { builder.include(it) }
            petMarker?.position?.let { builder.include(it) }
            
            val bounds = builder.build()
            val padding = 200 // Aumentar el padding para mejor visualización
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            googleMap.animateCamera(cameraUpdate)
        } catch (e: Exception) {
            Log.e(TAG, "Error ajustando zoom: ${e.message}")
        }
    }

    private fun updateLocationInFirebase(location: Location, requestId: String) {
        // Solo guardar ubicaciones si el servicio está en progreso
        if (serviceStatus != "in_progress") return

        val currentTime = System.currentTimeMillis()
        
        // Verificar si ha pasado suficiente tiempo desde la última actualización
        if (currentTime - lastLocationUpdate < MIN_UPDATE_INTERVAL) {
            return
        }

        // Verificar si la ubicación ha cambiado significativamente
        if (lastSavedLocation != null && 
            location.distanceTo(lastSavedLocation!!) < MIN_DISTANCE_CHANGE) {
            return
        }

        val geoPoint = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to currentTime
        )
        
        database.getReference("walkRequests")
            .child(requestId)
            .child("route")
            .child(currentTime.toString())
            .setValue(geoPoint)
            .addOnSuccessListener {
                lastLocationUpdate = currentTime
                lastSavedLocation = location
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

    private fun startWalk() {
        val updates = hashMapOf<String, Any>(
            "walkRequests/$activeRequestId/status" to "in_progress",
            "walkRequests/$activeRequestId/startTime" to ServerValue.TIMESTAMP
        )

        database.reference.updateChildren(updates)
            .addOnSuccessListener {
                serviceStatus = "in_progress"
                routePoints.clear() // Limpiar ruta anterior
                updateUIForStatus()
                startTimer()
            }
    }

    private fun updateUIForStatus() {
        when (serviceStatus) {
            "accepted" -> {
                binding.actionButton.text = "Ya llegué"
                binding.timerTextView.visibility = View.GONE
            }
            "in_progress" -> {
                binding.actionButton.text = "Terminar servicio"
                binding.timerTextView.visibility = View.VISIBLE
            }
        }
    }

    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (remainingSeconds > 0) {
                    remainingSeconds--
                    runOnUiThread {
                        val hours = remainingSeconds / 3600
                        val minutes = (remainingSeconds % 3600) / 60
                        val seconds = remainingSeconds % 60
                        binding.timerTextView.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        
                        // Si llegamos a cero, notificar al usuario
                        if (remainingSeconds == 0) {
                            Toast.makeText(applicationContext, "¡Tiempo del paseo completado!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }, 0, 1000)
    }

    override fun onStop() {
        super.onStop()
        timer?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "MapsActivityPaseador"
    }
}
