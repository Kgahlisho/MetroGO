package com.example.metrogo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location

class BusMapPage : AppCompatActivity() {

    private class SimBus(
        val label: String,
        val registration: String,
        val from: BusStop,
        val to: BusStop,
        val offsetSeconds: Double
    )

    private lateinit var mapView: MapView
    private lateinit var tvNearby: TextView
    private lateinit var busManager: PointAnnotationManager

    private val busMarkers = mutableMapOf<String, PointAnnotation>()
    private var userPoint: Point? = null
    private val handler = Handler(Looper.getMainLooper())

    private val radiusMeters = 15_000f          // "in the vicinity"
    private val tripSeconds = 180.0             // demo speed: one-way trip in 3 minutes

    // Approximate coordinates for the stations used in BusRouteRepository.
    private val stops = listOf(
        BusStop("PS", "Park Station", -26.2052, 28.0420),
        BusStop("RB", "Rosebank", -26.1467, 28.0436),
        BusStop("SD", "Sandton", -26.1076, 28.0567),
        BusStop("MR", "Midrand", -25.9992, 28.1263),
        BusStop("SW", "Soweto", -26.2485, 27.8540),
        BusStop("PT", "Parktown", -26.1816, 28.0368),
        BusStop("CT", "Centurion", -25.8603, 28.1894),
        BusStop("SS", "Soshanguve", -25.5200, 28.0900)
    )

    private val buses: List<SimBus> by lazy {
        BusRouteRepository.sampleRoutes
            .distinctBy { Triple(it.transportName, it.origin, it.destination) }
            .mapNotNull { r ->
                val a = stops.find { it.stopName == r.origin }
                val b = stops.find { it.stopName == r.destination }
                if (a != null && b != null) SimBus(r.transportName, r.registration, a, b, r.id * 17.0)
                else null
            }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) {
                enableLocation()
            } else {
                Toast.makeText(this, "Location permission denied. Showing Johannesburg.", Toast.LENGTH_LONG).show()
                showDefaultCamera()
                tvNearby.text = "Allow location to see buses near you."
            }
        }

    private val positionListener = object : OnIndicatorPositionChangedListener {
        override fun onIndicatorPositionChanged(point: Point) {
            val first = userPoint == null
            userPoint = point
            if (first) centerOn(point)
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            updateBuses()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_map)

        mapView = findViewById(R.id.mapView)
        tvNearby = findViewById(R.id.tvNearby)
        findViewById<Button>(R.id.btnMapBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnCenterMe).setOnClickListener {
            userPoint?.let { centerOn(it) }
                ?: Toast.makeText(this, "Still finding your location…", Toast.LENGTH_SHORT).show()
        }

        // Stops (static markers)
        val stopManager = mapView.annotations.createPointAnnotationManager()
        val stopIcon = dot(Color.parseColor("#455A64"))
        stops.forEach { s ->
            stopManager.create(
                PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(s.longitude, s.latitude))
                    .withIconImage(stopIcon)
                    .withTextField(s.stopName)
                    .withTextSize(11.0)
                    .withTextOffset(listOf(0.0, 1.6))
            )
        }

        // Buses (moving markers)
        busManager = mapView.annotations.createPointAnnotationManager()

        showDefaultCamera()
        requestLocation()
    }

    override fun onStart() {
        super.onStart()
        handler.post(ticker)
    }

    override fun onStop() {
        handler.removeCallbacks(ticker)
        super.onStop()
    }

    override fun onDestroy() {
        mapView.location.removeOnIndicatorPositionChangedListener(positionListener)
        super.onDestroy()
    }

    // ---------------- Location ----------------

    private fun requestLocation() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            enableLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun enableLocation() {
        mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true
        }
        mapView.location.addOnIndicatorPositionChangedListener(positionListener)
    }

    private fun centerOn(point: Point) {
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder().center(point).zoom(12.0).build()
        )
    }

    private fun showDefaultCamera() {
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(28.0473, -26.2041))   // Johannesburg
                .zoom(9.5)
                .build()
        )
    }

    // ---------------- Simulated buses ----------------

    private fun posOf(b: SimBus): Point {
        val t = ((SystemClock.elapsedRealtime() / 1000.0 + b.offsetSeconds) % (tripSeconds * 2)) / tripSeconds
        val f = if (t <= 1.0) t else 2.0 - t          // there and back
        val lat = b.from.latitude + (b.to.latitude - b.from.latitude) * f
        val lng = b.from.longitude + (b.to.longitude - b.from.longitude) * f
        return Point.fromLngLat(lng, lat)
    }

    private fun distanceMeters(a: Point, b: Point): Float {
        val out = FloatArray(1)
        Location.distanceBetween(a.latitude(), a.longitude(), b.latitude(), b.longitude(), out)
        return out[0]
    }

    private fun updateBuses() {
        val busIcon = busIconBitmap
        val nearby = mutableListOf<Pair<SimBus, Float>>()

        buses.forEach { bus ->
            val p = posOf(bus)
            val key = bus.label + bus.registration
            val marker = busMarkers[key]
            if (marker == null) {
                busMarkers[key] = busManager.create(
                    PointAnnotationOptions()
                        .withPoint(p)
                        .withIconImage(busIcon)
                        .withTextField(bus.label)
                        .withTextSize(11.0)
                        .withTextOffset(listOf(0.0, -1.8))
                )
            } else {
                marker.point = p
                busManager.update(marker)
            }
            userPoint?.let { u ->
                val d = distanceMeters(u, p)
                if (d <= radiusMeters) nearby += bus to d
            }
        }

        tvNearby.text = when {
            userPoint == null -> "Finding your location…"
            nearby.isEmpty() ->
                "No buses within ${(radiusMeters / 1000).toInt()} km of you.\n" +
                        "(On an emulator? Set its location to Johannesburg in Extended controls → Location.)"
            else -> nearby.sortedBy { it.second }.take(4).joinToString("\n") { (bus, d) ->
                "${bus.label}  ${bus.from.stopName} → ${bus.to.stopName}  ·  %.1f km away".format(d / 1000f)
            }
        }
    }

    // ---------------- Marker bitmaps ----------------

    private val busIconBitmap: Bitmap by lazy { dot(Color.parseColor("#00897B"), 64) }

    private fun dot(color: Int, size: Int = 48): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.WHITE
        c.drawCircle(size / 2f, size / 2f, size / 2f, p)
        p.color = color
        c.drawCircle(size / 2f, size / 2f, size / 2f - 6f, p)
        return bmp
    }
}