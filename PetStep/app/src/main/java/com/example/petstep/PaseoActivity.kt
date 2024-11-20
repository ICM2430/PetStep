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

class PaseoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityPaseoBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST = 1
    private val database = FirebaseDatabase.getInstance()
    private val walkersRef = database.getReference("walkers")
    private val walkers = mutableListOf<Walker>()
    private val walkersAdapter = WalkersAdapter(walkers) { walker -> sendRequestToWalker(walker) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaseoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()
        
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
                        val walker = walkerSnapshot.getValue(Walker::class.java)
                        walker?.let {
                            it.id = walkerSnapshot.key ?: ""
                            walkers.add(it)
                            addWalkerMarkerToMap(it)
                        }
                    }
                    if (walkers.isEmpty()) {
                        Toast.makeText(this@PaseoActivity, 
                            "No hay paseadores disponibles", 
                            Toast.LENGTH_SHORT).show()
                    }
                    walkersAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PaseoActivity, 
                        "Error al cargar paseadores: ${error.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendRequestToWalker(walker: Walker) {
        val requestRef = database.getReference("requests")
        val request = hashMapOf(
            "ownerId" to FirebaseAuth.getInstance().currentUser!!.uid,
            "walkerId" to walker.id,
            "status" to "pending",
            "timestamp" to ServerValue.TIMESTAMP
        )
        
        requestRef.push().setValue(request)
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud enviada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al enviar solicitud", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addWalkerMarkerToMap(walker: Walker) {
        if (!::googleMap.isInitialized) return
        val location = LatLng(walker.latitude, walker.longitude)
        googleMap.addMarker(MarkerOptions()
            .position(location)
            .title("Paseador: ${walker.nombre}"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10f))
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
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        }
        for (walker in walkers) {
            addWalkerMarkerToMap(walker)
        }
    }
}
