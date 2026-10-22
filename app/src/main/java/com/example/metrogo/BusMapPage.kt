package com.example.metrogo

import android.Manifest
import android.annotation.SuppressLint
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
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

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
    private lateinit var map: MapLibreMap
    private lateinit var style: Style
    private lateinit var symbolManager: SymbolManager

    private val busSymbols = mutableMapOf<String, Symbol>()
    private var userLatLng: LatLng? = null
    private var locationEnabled = false
    private val handler = Handler(Looper.getMainLooper())

    private val radiusMeters = 15_000f
    private val tripSeconds = 180.0

    // Must exist in the glyphs of the style you load. If labels go blank after
    // changing the style URL, change this to a font that style provides.
    private val textFont = arrayOf("Noto Sans Regular")

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
                enableLocationComponent()
            } else {
                Toast.makeText(this, "Location permission denied. Showing Johannesburg.", Toast.LENGTH_LONG).show()
                showDefaultCamera()
                tvNearby.text = "Allow location to see buses near you."
            }
        }

    private val ticker = object : Runnable {
        override fun run() {
            updateBuses()
            handler.postDelayed(this, 1000)
        }
    }

    private val locationPoller = object : Runnable {
        override fun run() {
            val loc = if (::map.isInitialized) map.locationComponent.lastKnownLocation else null
            if (loc != null) {
                val newLatLng = LatLng(loc.latitude, loc.longitude)
                val first = userLatLng == null
                userLatLng = newLatLng
                if (first) centerOn(newLatLng)
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // MapLibre.getInstance() is already called in MetroGoApp.
        setContentView(R.layout.activity_bus_map)

        mapView = findViewById(R.id.mapView)
        tvNearby = findViewById(R.id.tvNearby)
        findViewById<Button>(R.id.btnMapBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnCenterMe).setOnClickListener {
            userLatLng?.let { centerOn(it) }
                ?: Toast.makeText(this, "Still finding your location…", Toast.LENGTH_SHORT).show()
        }

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { maplibreMap ->
            map = maplibreMap
            map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty")) { loadedStyle ->
                style = loadedStyle
                setupMapContent()
            }
        }
    }

    private fun setupMapContent() {
        val stopIcon = dot(Color.parseColor("#455A64"))
        style.addImage("stop-icon", stopIcon)

        symbolManager = SymbolManager(mapView, map, style)
        symbolManager.iconAllowOverlap = true
        symbolManager.textAllowOverlap = true

        stops.forEach { s ->
            symbolManager.create(
                SymbolOptions()
                    .withLatLng(LatLng(s.latitude, s.longitude))
                    .withIconImage("stop-icon")
                    .withTextField(s.stopName)
                    .withTextFont(textFont)
                    .withTextSize(11f)
                    .withTextOffset(arrayOf(0f, 1.6f))
            )
        }

        style.addImage("bus-icon", busIconBitmap)

        showDefaultCamera()
        requestLocation()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        handler.post(ticker)
        if (locationEnabled) {
            handler.removeCallbacks(locationPoller)
            handler.post(locationPoller)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        handler.removeCallbacks(ticker)
        handler.removeCallbacks(locationPoller)
        mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::symbolManager.isInitialized) symbolManager.onDestroy()
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private fun requestLocation() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            enableLocationComponent()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent() {
        val locationComponent = map.locationComponent
        val activationOptions = LocationComponentActivationOptions.builder(this, style)
            .locationComponentOptions(
                LocationComponentOptions.builder(this)
                    .pulseEnabled(true)
                    .build()
            )
            .useDefaultLocationEngine(true)
            .build()

        locationComponent.activateLocationComponent(activationOptions)
        locationComponent.isLocationComponentEnabled = true
        locationComponent.cameraMode = CameraMode.NONE

        locationEnabled = true
        handler.removeCallbacks(locationPoller)
        handler.post(locationPoller)
    }

    private fun centerOn(latLng: LatLng) {
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(latLng)
                    .zoom(12.0)
                    .build()
            )
        )
    }

    private fun showDefaultCamera() {
        map.cameraPosition = CameraPosition.Builder()
            .target(LatLng(-26.2041, 28.0473))
            .zoom(9.5)
            .build()
    }

    private fun posOf(b: SimBus): LatLng {
        val t = ((SystemClock.elapsedRealtime() / 1000.0 + b.offsetSeconds) % (tripSeconds * 2)) / tripSeconds
        val f = if (t <= 1.0) t else 2.0 - t
        val lat = b.from.latitude + (b.to.latitude - b.from.latitude) * f
        val lng = b.from.longitude + (b.to.longitude - b.from.longitude) * f
        return LatLng(lat, lng)
    }

    private fun distanceMeters(a: LatLng, b: LatLng): Float {
        val out = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, out)
        return out[0]
    }

    private fun updateBuses() {
        // The ticker starts in onStart(), before the map style has finished loading.
        if (!::symbolManager.isInitialized) return

        val nearby = mutableListOf<Pair<SimBus, Float>>()

        buses.forEach { bus ->
            val p = posOf(bus)
            val key = bus.label + bus.registration
            val existing = busSymbols[key]

            if (existing == null) {
                val symbol = symbolManager.create(
                    SymbolOptions()
                        .withLatLng(p)
                        .withIconImage("bus-icon")
                        .withTextField(bus.label)
                        .withTextFont(textFont)
                        .withTextSize(11f)
                        .withTextOffset(arrayOf(0f, -1.8f))
                )
                if (symbol != null) {
                    busSymbols[key] = symbol
                }
            } else {
                existing.latLng = p
                symbolManager.update(existing)
            }

            userLatLng?.let { u ->
                val d = distanceMeters(u, p)
                if (d <= radiusMeters) nearby += bus to d
            }
        }

        tvNearby.text = when {
            userLatLng == null -> "Finding your location…"
            nearby.isEmpty() ->
                "No buses within ${(radiusMeters / 1000).toInt()} km of you.\n" +
                        "(On an emulator? Set its location to Johannesburg in Extended controls → Location.)"
            else -> nearby.sortedBy { it.second }.take(4).joinToString("\n") { (bus, d) ->
                "${bus.label}  ${bus.from.stopName} → ${bus.to.stopName}  ·  %.1f km away".format(d / 1000f)
            }
        }
    }

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