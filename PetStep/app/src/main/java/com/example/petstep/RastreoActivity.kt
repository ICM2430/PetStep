package com.example.petstep

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivityRastreoBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore

class RastreoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityRastreoBinding
    private lateinit var googleMap: GoogleMap
    private val db = FirebaseFirestore.getInstance()
    private var walkerMarker: Marker? = null
    private val walkerRoute = mutableListOf<LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRastreoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val walkerId = intent.getStringExtra("walkerId") ?: return
        trackWalkerLocation(walkerId)
    }

    private fun trackWalkerLocation(walkerId: String) {
        db.collection("locations").document(walkerId).addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val lat = it.getDouble("latitude") ?: return@addSnapshotListener
                val lng = it.getDouble("longitude") ?: return@addSnapshotListener
                val walkerLatLng = LatLng(lat, lng)

                updateWalkerLocation(walkerLatLng)
                walkerRoute.add(walkerLatLng)
                drawRoute()
            }
        }
    }

    private fun updateWalkerLocation(location: LatLng) {
        if (walkerMarker == null) {
            walkerMarker = googleMap.addMarker(MarkerOptions().position(location).title("Paseador"))
        } else {
            walkerMarker!!.position = location
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }

    private fun drawRoute() {
        val polylineOptions = PolylineOptions().addAll(walkerRoute).width(8f).color(android.graphics.Color.BLUE)
        googleMap.addPolyline(polylineOptions)
    }
}
