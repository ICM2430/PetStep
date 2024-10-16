package com.example.petstep

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.petstep.databinding.ActivityMapsPaseadorBinding
import com.example.petstep.model.MyLocation
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.sql.Date
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class MapsActivityPaseador : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsPaseadorBinding

    // OSRM Routing
    private lateinit var geocoder: Geocoder
    private lateinit var roadManager: RoadManager
    private var roadOverlay: Polyline? = null

    // Location
    val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(), ActivityResultCallback {
            if (it) { // granted
                locationSettings()
            } else { // denied
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
            }
        })
    private val locationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ActivityResultCallback {
            if (it.resultCode == RESULT_OK) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "GPS OFF!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback

    var burnLoc = LatLng(4.619693158601781, -74.08496767920794)

    // User location
    lateinit var locationClient: FusedLocationProviderClient
    private var posActual: Location? = null

    var locations = mutableListOf<JSONObject>()

    // Sensor Management
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private lateinit var sensorEventListener: SensorEventListener

    // Step Counter
    private var stepCount: Int = 0
    private lateinit var stepCounterTextView: TextView

    private var previousY: Float = 0f
    private var threshold: Float = 2.0f // Example threshold for step detection
    private var isStepDetected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsPaseadorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize UI components
        stepCounterTextView = findViewById(R.id.stepCounter) // Ensure this TextView exists in your layout

        geocoder = Geocoder(baseContext)
        roadManager = OSRMRoadManager(this, "ANDROID")

        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallBack()
        locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        // Allow network operations on main thread (if necessary)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        binding.ruta.setOnClickListener {
            posActual?.let {
                drawRoute(
                    GeoPoint(burnLoc.latitude, burnLoc.longitude),
                    GeoPoint(it.latitude, it.longitude)
                )
                drawMarker(
                    burnLoc,
                    "Quemadero",
                    R.drawable.baseline_location_pin_24
                )
            } ?: Toast.makeText(this, "Current location not available.", Toast.LENGTH_SHORT).show()
        }

        // Initialize Sensor Manager and Sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorEventListener = createSensorEventListener()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val bogota = LatLng(4.34, -74.3) // Corrected longitude sign for Bogotá
        mMap.moveCamera(CameraUpdateFactory.newLatLng(bogota))
        mMap.moveCamera(CameraUpdateFactory.zoomTo(17f))
    }

    fun drawMarker(location: LatLng, description: String?, icon: Int) {
        val addressMarker = mMap.addMarker(
            MarkerOptions()
                .position(location)
                .icon(bitmapDescriptorFromVector(this, icon))
        )!!
        description?.let {
            addressMarker.title = it
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(location))
        mMap.moveCamera(CameraUpdateFactory.zoomTo(17f))
    }

    fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
        val vectorDrawable: Drawable = ContextCompat.getDrawable(context, vectorResId)!!
        vectorDrawable.setBounds(
            0, 0, vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun locationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            startLocationUpdates()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val isr = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettings.launch(isr)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Handle the exception
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5000)
            .build()
    }

    fun createLocationCallBack(): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val location = result.lastLocation
                if (location != null) {
                    if (posActual == null) {
                        posActual = location
                    } else {
                        if (distancia(
                                LatLng(posActual!!.latitude, posActual!!.longitude),
                                location
                            ) > 0.01
                        ) {
                            posActual = location
                            mMap.clear()
                            writeJSON()
                        }
                    }
                    Log.i("LocationHelp", "${posActual!!.latitude}")
                    posActual = result.lastLocation!!
                    drawMarker(
                        LatLng(posActual!!.latitude, posActual!!.longitude),
                        "PosActual",
                        R.drawable.paseador
                    )
                }
            }
        }
    }

    private fun writeJSON() {
        val myLocation =
            MyLocation(Date(System.currentTimeMillis()), posActual!!.latitude, posActual!!.longitude)
        locations.add(myLocation.toJSON())
        val filename = "locations.json"
        val file = File(baseContext.getExternalFilesDir(null), filename)
        val output = BufferedWriter(FileWriter(file))
        output.write(locations.toString())
        output.close()
        Log.i("LOCATION", "File modified at path $file")
    }

    fun distancia(longpress: LatLng, actual: Location): Float {
        val pk = (180f / Math.PI).toFloat()

        val a1: Double = longpress.latitude / pk
        val a2: Double = longpress.longitude / pk
        val b1: Double = actual.latitude / pk
        val b2: Double = actual.longitude / pk

        val t1 = cos(a1) * cos(a2) * cos(b1) * cos(b2)
        val t2 = cos(a1) * sin(a2) * cos(b1) * sin(b2)
        val t3 = sin(a1) * sin(b1)
        val tt = acos(t1 + t2 + t3)

        return (6366000 * tt).toFloat()
    }

    fun drawRoute(finish: GeoPoint, start: GeoPoint) {
        val routePoints = ArrayList<GeoPoint>()
        routePoints.add(start)
        routePoints.add(finish)
        val road = roadManager.getRoad(routePoints)
        if (::mMap.isInitialized) {
            roadOverlay?.let {
                mMap.clear() // Clear all overlays
            }
            roadOverlay = RoadManager.buildRoadOverlay(road)
            roadOverlay!!.outlinePaint.color = Color.BLUE
            roadOverlay!!.outlinePaint.strokeWidth = 10F

            val polylineOptions = PolylineOptions()
            for (point in roadOverlay!!.points) {
                polylineOptions.add(LatLng(point.latitude, point.longitude))
            }
            polylineOptions.color(Color.WHITE)
            polylineOptions.width(10F)
            mMap.addPolyline(polylineOptions)
        }
    }

    // Sensor Event Listener
    private fun createSensorEventListener(): SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                when (event?.sensor?.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val y = event.values[1]
                        // Simple step detection logic
                        if (previousY != 0f) {
                            val deltaY = y - previousY
                            if (deltaY > threshold && !isStepDetected) {
                                stepCount++
                                updateStepCounter()
                                isStepDetected = true
                            } else if (deltaY < -threshold) {
                                isStepDetected = false
                            }
                        }
                        previousY = y
                    }
                    Sensor.TYPE_LIGHT -> {
                        val light = event.values[0]
                        if (::mMap.isInitialized) {
                            if (light < 1000) {
                                // Apply dark map style
                                try {
                                    val success = mMap.setMapStyle(
                                        com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
                                            this@MapsActivityPaseador,
                                            R.raw.map_dark
                                        )
                                    )
                                    if (!success) {
                                        Log.e("MapStyle", "Dark style parsing failed.")
                                    }
                                } catch (e: Exception) {
                                    Log.e("MapStyle", "Can't find dark style. Error: ", e)
                                }
                            } else {
                                // Apply light map style
                                try {
                                    val success = mMap.setMapStyle(
                                        com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
                                            this@MapsActivityPaseador,
                                            R.raw.map_light
                                        )
                                    )
                                    if (!success) {
                                        Log.e("MapStyle", "Light style parsing failed.")
                                    }
                                } catch (e: Exception) {
                                    Log.e("MapStyle", "Can't find light style. Error: ", e)
                                }
                            }
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    override fun onResume() {
        super.onResume()
        // Register sensor listeners
        accelerometer?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        lightSensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister sensor listeners
        sensorManager.unregisterListener(sensorEventListener)
    }

    private fun updateStepCounter() {
        runOnUiThread {
            stepCounterTextView.text = "Steps: $stepCount"
        }
    }
}
