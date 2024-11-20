package com.example.petstep

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petstep.adapters.WalkersAdapter
import com.example.petstep.databinding.ActivityPaseoBinding
import com.example.petstep.com.example.petstep.model.Walker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import android.location.Location

class PaseoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityPaseoBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST = 1
    private val database = FirebaseDatabase.getInstance()
    private val walkersRef = database.getReference("walkers")
    private val walkers = mutableListOf<Walker>()
    private val walkersAdapter = WalkersAdapter(walkers) { walker ->
        // Cambiamos esto para mostrar el diálogo primero
        showDurationPetPicker(walker.id)
    }
    private lateinit var userLocation: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaseoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userLocation = LatLng(
            intent.getDoubleExtra("USER_LAT", 0.0),
            intent.getDoubleExtra("USER_LNG", 0.0)
        )
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()
        
        setupMap()
        setupRecyclerView()
        loadAvailableWalkers()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PaseoActivity)
            adapter = walkersAdapter
        }
    }

    private fun loadAvailableWalkers() {
        walkersRef.orderByChild("available").equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    walkers.clear()
                    for (walkerSnapshot in snapshot.children) {
                        val walkerId = walkerSnapshot.key ?: continue
                        val walkerBasicInfo = walkerSnapshot.getValue(Walker::class.java) ?: continue
                        
                        // Fetch additional walker information from users/paseadores node
                        database.getReference("users/paseadores/$walkerId")
                            .get()
                            .addOnSuccessListener { userSnapshot ->
                                val walker = Walker(
                                    id = walkerId,
                                    available = walkerBasicInfo.available,
                                    precioPorHora = walkerBasicInfo.precioPorHora,
                                    workZone = walkerBasicInfo.workZone,
                                    latitude = walkerSnapshot.child("workZoneLat").getValue(Double::class.java) ?: 0.0,
                                    longitude = walkerSnapshot.child("workZoneLng").getValue(Double::class.java) ?: 0.0,
                                    nombre = userSnapshot.child("nombre").getValue(String::class.java) ?: "",
                                    apellido = userSnapshot.child("apellido").getValue(String::class.java) ?: "",
                                    profilePhotoUrl = userSnapshot.child("profilePhotoUrl").getValue(String::class.java) ?: ""
                                )
                                
                                // Calculate distance
                                val walkerLocation = LatLng(walker.latitude, walker.longitude)
                                walker.distancia = calculateDistance(userLocation, walkerLocation).toDouble()
                                
                                // Add to list and update UI
                                walkers.add(walker)
                                walkers.sortBy { it.distancia }
                                walkersAdapter.notifyDataSetChanged()
                                
                                // Update map markers
                                googleMap.clear()
                                addUserMarker()
                                walkers.forEach { addWalkerMarkerToMap(it) }
                            }
                    }
                    
                    if (snapshot.children.count() == 0) {
                        Toast.makeText(this@PaseoActivity, 
                            "No hay paseadores disponibles", 
                            Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PaseoActivity, 
                        "Error al cargar paseadores: ${error.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            })
    }

    /*private fun sendRequestToWalker(walker: Walker) {
        // Este método ya no se usa
    }*/

    private fun addUserMarker() {
        if (!::googleMap.isInitialized) return
        googleMap.addMarker(MarkerOptions()
            .position(userLocation)
            .title("Tu ubicación")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
    }

    private fun addWalkerMarkerToMap(walker: Walker) {
        if (!::googleMap.isInitialized) return
        
        val walkerLocation = LatLng(walker.latitude, walker.longitude)
        googleMap.addMarker(MarkerOptions()
            .position(walkerLocation)
            .title("${walker.nombre} - %.2f km".format(walker.distancia))
            .snippet("$${walker.precioPorHora}/hora")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        return results[0] / 1000 // Convert to kilometers
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            setupMap()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupMap()
                } else {
                    Toast.makeText(this, "Se requiere permiso de ubicación", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Configurar controles del mapa
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true  // Mostrar controles de zoom
            isZoomGesturesEnabled = true  // Permitir gestos de zoom
            isScrollGesturesEnabled = true // Permitir scroll/pan
            isRotateGesturesEnabled = true // Permitir rotación
            isCompassEnabled = true       // Mostrar brújula
            isMyLocationButtonEnabled = true // Botón para centrar en ubicación
        }
        
        // Centrar mapa en ubicación del usuario
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14f))
        
        // Añadir marcador del usuario
        addUserMarker()
        
        if (ActivityCompat.checkSelfPermission(this, 
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }
        
        // Cargar marcadores de paseadores existentes
        walkers.forEach { walker ->
            addWalkerMarkerToMap(walker)
        }
        
        // Listener para cuando se presiona el botón de ubicación
        googleMap.setOnMyLocationButtonClickListener {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14f))
            true
        }
    }

    private fun showDurationPetPicker(walkerId: String) {
        DurationPickerDialog(this).apply {
            onConfirm = { duration, petId ->
                createWalkRequest(walkerId, duration, petId)
            }
            show()
        }
    }

    private fun createWalkRequest(walkerId: String, duration: Int, petId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val requestRef = FirebaseDatabase.getInstance().getReference("walkRequests")
        val requestId = requestRef.push().key ?: return

        // Get walker's location from walkers list
        val walker = walkers.find { it.id == walkerId }
        val distance = walker?.let { 
            calculateDistance(
                userLocation,
                LatLng(it.latitude, it.longitude)
            )
        } ?: 0f

        val request = hashMapOf(
            "id" to requestId,
            "userId" to userId,
            "walkerId" to walkerId,
            "petId" to petId,
            "duration" to duration,
            "status" to "pending",
            "timestamp" to ServerValue.TIMESTAMP,
            "ownerLat" to userLocation.latitude,
            "ownerLng" to userLocation.longitude,
            "distance" to distance
        )

        requestRef.child(requestId).setValue(request)
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud enviada exitosamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al enviar la solicitud", Toast.LENGTH_SHORT).show()
            }
    }
}
