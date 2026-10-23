package com.example.metrogo

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.Line
import org.maplibre.android.plugins.annotation.LineManager
import org.maplibre.android.plugins.annotation.LineOptions
import org.maplibre.android.plugins.annotation.OnSymbolClickListener
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.style.layers.Property
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Suppress("DEPRECATION")
private fun announce(view: View, message: String) {
    view.announceForAccessibility(message)
}

class BusMapPage : AccessibleActivity() {

    // ---------- model ----------
    private class SimBus(
        val label: String,
        val line: String,
        val color: Int,
        val registration: String,
        val from: BusStop,
        val to: BusStop,
        val offsetSeconds: Double
    ) {
        val key get() = label + registration
    }

    private class Fix(
        val pos: LatLng,
        val heading: Float,
        val leg: Double,          // 0..1 along the current leg
        val towards: BusStop,
        val secondsLeft: Double
    )

    private class RouteLine(val line: String, val from: BusStop, val to: BusStop, val color: Int) {
        var annotation: Line? = null
    }

    // ---------- views / map ----------
    private lateinit var mapView: MapView
    private lateinit var tvNearby: TextView
    private lateinit var tvLiveCount: TextView
    private lateinit var tvFollowing: TextView
    private lateinit var chipRow: LinearLayout
    private lateinit var rvBuses: RecyclerView
    private lateinit var liveDot: View
    private lateinit var map: MapLibreMap
    private lateinit var style: Style
    private lateinit var symbolManager: SymbolManager
    private lateinit var lineManager: LineManager
    private lateinit var adapter: NearbyBusAdapter

    // ---------- state ----------
    private val busSymbols = mutableMapOf<String, Symbol>()
    private val symbolToKey = mutableMapOf<Long, String>()
    private val fixes = HashMap<String, Fix>()
    private val cards = LinkedHashMap<String, BusCard>()
    private var shownCards: List<BusCard> = emptyList()
    private val chipViews = mutableMapOf<String?, TextView>()

    private var filter: String? = null
    private var selectedKey: String? = null
    private var followKey: String? = null
    private var userLatLng: LatLng? = null
    private var locationEnabled = false
    private var locationDenied = false
    private var tilted = false
    private var hc = false
    private var mapReady = false
    private var panelCollapsed = false
    private var topInset = 0

    private var speaker: Speaker? = null
    private var liveDotPulse: ObjectAnimator? = null
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

    // Dark enough for white text/arrows (WCAG AA).
    private val palette = intArrayOf(
        0xFF0E7C89.toInt(), 0xFFC2410C.toInt(), 0xFF6D28D9.toInt(),
        0xFF1D4ED8.toInt(), 0xFFBE185D.toInt(), 0xFF15803D.toInt()
    )

    private fun lineCode(name: String) =
        name.split(" ").firstOrNull { it.matches(Regex("T\\d+")) } ?: "VR"

    private val buses: List<SimBus> by lazy {
        val codes = BusRouteRepository.sampleRoutes.map { lineCode(it.transportName) }.distinct()
        BusRouteRepository.sampleRoutes
            .distinctBy { Triple(it.transportName, it.origin, it.destination) }
            .mapNotNull { r ->
                val a = stops.find { it.stopName == r.origin }
                val b = stops.find { it.stopName == r.destination }
                if (a != null && b != null) {
                    val code = lineCode(r.transportName)
                    SimBus(
                        r.transportName, code, palette[codes.indexOf(code) % palette.size],
                        r.registration, a, b, r.id * 17.0
                    )
                } else null
            }
    }

    private val routeLines: List<RouteLine> by lazy {
        buses.distinctBy { Triple(it.line, it.from.stopId, it.to.stopId) }
            .map { RouteLine(it.line, it.from, it.to, it.color) }
    }

    // ---------- permission + loops ----------
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) {
                enableLocationComponent()
            } else {
                locationDenied = true
                Toast.makeText(this, "Location permission denied. Showing all buses.", Toast.LENGTH_LONG).show()
                fitVisible()
            }
        }

    private val ticker = object : Runnable {
        override fun run() {
            refreshCards()
            if (mapReady) {
                syncSymbols()
                followCamera()
            }
            updateStatusLine()
            adapter.refresh()
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
                if (first) {
                    refreshCards()
                    resortCards()
                    if (selectedKey == null) centerOn(newLatLng)
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    // ---------- lifecycle ----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // MapLibre.getInstance() is already called in MetroGoApp.
        setContentView(R.layout.activity_bus_map)

        hc = AccessibilitySettings.isHighContrast(this)
        speaker = Speaker(this)

        mapView = findViewById(R.id.mapView)
        tvNearby = findViewById(R.id.tvNearby)
        tvLiveCount = findViewById(R.id.tvLiveCount)
        tvFollowing = findViewById(R.id.tvFollowing)
        chipRow = findViewById(R.id.chipRow)
        rvBuses = findViewById(R.id.rvBuses)
        liveDot = findViewById(R.id.liveDot)

        liveDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFF15803D.toInt())
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topInset = bars.top
            findViewById<View>(R.id.topStack).updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = bars.top + dp(8)
            }
            findViewById<View>(R.id.panel).setPadding(dp(16), 0, dp(16), bars.bottom + dp(12))
            insets
        }

        findViewById<View>(R.id.btnMapBack).setOnClickListener { finish() }
        findViewById<View>(R.id.fabCenterMe).setOnClickListener {
            userLatLng?.let { centerOn(it) }
                ?: Toast.makeText(this, "Still finding your location…", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.fabFitAll).setOnClickListener { clearSelection(); fitVisible() }
        findViewById<View>(R.id.fabSpeak).setOnClickListener { readNearestAloud() }
        findViewById<View>(R.id.fabTilt).setOnClickListener { toggleTilt() }
        tvFollowing.setOnClickListener { clearSelection() }
        findViewById<View>(R.id.panelHandle).setOnClickListener { togglePanel() }

        adapter = NearbyBusAdapter { card -> onCardClicked(card) }
        rvBuses.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvBuses.itemAnimator = null   // 1-second refreshes shouldn't animate
        rvBuses.adapter = adapter

        buses.forEach { b ->
            cards[b.key] = BusCard(b.key, b.line, b.label, b.color, b.from.stopName, b.to.stopName)
        }
        buildChips()
        refreshCards()
        resortCards()

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { maplibreMap ->
            map = maplibreMap
            map.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) stopFollowing()
            }
            map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty")) { loadedStyle ->
                style = loadedStyle
                setupMapContent()
            }
        }
    }

    private fun setupMapContent() {
        style.addImage("stop-icon", stopDot())
        buses.map { it.line }.distinct().forEach { code ->
            val color = buses.first { it.line == code }.color
            style.addImage("bus-$code", busIcon(color))
        }

        // Created first, so route lines sit underneath the markers.
        lineManager = LineManager(mapView, map, style)
        routeLines.forEach { rl ->
            rl.annotation = lineManager.create(
                LineOptions()
                    .withLatLngs(listOf(
                        LatLng(rl.from.latitude, rl.from.longitude),
                        LatLng(rl.to.latitude, rl.to.longitude)
                    ))
                    .withLineColor(String.format("#%06X", 0xFFFFFF and rl.color))
                    .withLineWidth(4f)
                    .withLineOpacity(0.75f)
            )
        }

        symbolManager = SymbolManager(mapView, map, style)
        symbolManager.iconAllowOverlap = true
        symbolManager.textAllowOverlap = true
        symbolManager.setIconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP)
        symbolManager.addClickListener(OnSymbolClickListener { symbol ->
            symbolToKey[symbol.id]?.let { key -> cards[key]?.let { onCardClicked(it) } }
            true
        })

        stops.forEach { s ->
            symbolManager.create(
                SymbolOptions()
                    .withLatLng(LatLng(s.latitude, s.longitude))
                    .withIconImage("stop-icon")
                    .withTextField(s.stopName)
                    .withTextFont(textFont)
                    .withTextSize(if (hc) 13f else 11f)
                    .withTextColor(if (hc) "#000000" else "#263238")
                    .withTextHaloColor("#FFFFFF")
                    .withTextHaloWidth(if (hc) 2.5f else 1.5f)
                    .withTextOffset(arrayOf(0f, 1.6f))
            )
        }

        mapReady = true
        restyleLines()
        syncSymbols()

        // Cinematic intro: start tilted over Johannesburg, then swoop out to fit every route.
        map.cameraPosition = CameraPosition.Builder()
            .target(LatLng(-26.2041, 28.0473))
            .zoom(9.5)
            .tilt(if (reducedMotion()) 0.0 else 50.0)
            .build()
        if (reducedMotion()) fitVisible(animated = false)
        else handler.postDelayed({ fitVisible() }, 450)

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
        if (!reducedMotion()) {
            liveDotPulse = ObjectAnimator.ofFloat(liveDot, "alpha", 1f, 0.25f).apply {
                duration = 900
                repeatMode = ObjectAnimator.REVERSE
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
        }
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }

    override fun onStop() {
        handler.removeCallbacks(ticker)
        handler.removeCallbacks(locationPoller)
        liveDotPulse?.cancel(); liveDotPulse = null
        speaker?.stop()
        mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        speaker?.shutdown(); speaker = null
        if (::symbolManager.isInitialized) symbolManager.onDestroy()
        if (::lineManager.isInitialized) lineManager.onDestroy()
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    // ---------- location ----------
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
                LocationComponentOptions.builder(this).pulseEnabled(true).build()
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

    // ---------- simulation ----------
    private fun fixOf(b: SimBus): Fix {
        val t = (SystemClock.elapsedRealtime() / 1000.0 + b.offsetSeconds) % (tripSeconds * 2)
        val outbound = t <= tripSeconds
        val u = if (outbound) t / tripSeconds else (t - tripSeconds) / tripSeconds
        val start = if (outbound) b.from else b.to
        val end = if (outbound) b.to else b.from
        val lat = start.latitude + (end.latitude - start.latitude) * u
        val lng = start.longitude + (end.longitude - start.longitude) * u
        return Fix(LatLng(lat, lng), bearing(start, end), u, end, (1 - u) * tripSeconds)
    }

    private fun bearing(a: BusStop, b: BusStop): Float {
        val lat1 = Math.toRadians(a.latitude); val lat2 = Math.toRadians(b.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }

    // Sample data: replace with a real occupancy feed when you have one.
    private fun crowdOf(b: SimBus): String {
        val minute = (SystemClock.elapsedRealtime() / 60_000L).toInt()
        return when (((b.registration.hashCode() and 0x7fffffff) + minute) % 3) {
            0 -> "Seats free"
            1 -> "Filling up"
            else -> "Standing room"
        }
    }

    private fun distanceMeters(a: LatLng, b: LatLng): Float {
        val out = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, out)
        return out[0]
    }

    private fun isVisible(b: SimBus) = filter == null || b.line == filter

    /** Updates the data behind the cards. Works before the map has loaded. */
    private fun refreshCards() {
        buses.forEach { bus ->
            val fix = fixOf(bus)
            fixes[bus.key] = fix
            cards[bus.key]?.apply {
                nextStop = fix.towards.stopName
                etaSeconds = fix.secondsLeft.toInt()
                progress = (fix.leg * 100).toInt()
                distM = userLatLng?.let { distanceMeters(it, fix.pos) }
                crowd = crowdOf(bus)
            }
        }
    }

    private fun resortCards() {
        val list = buses.filter { isVisible(it) }.map { cards.getValue(it.key) }
        shownCards =
            if (userLatLng != null) list.sortedBy { it.distM ?: Float.MAX_VALUE }
            else list.sortedBy { it.line }
        adapter.submit(shownCards)
        tvLiveCount.text = "${shownCards.size} running"
    }

    private fun updateStatusLine() {
        tvNearby.text = when {
            locationDenied -> "Allow location to sort buses by distance."
            userLatLng == null -> "Finding your location… showing all ${shownCards.size} buses"
            else -> {
                val near = shownCards.count { (it.distM ?: Float.MAX_VALUE) <= radiusMeters }
                "$near within ${(radiusMeters / 1000).toInt()} km  \u2022  sorted by distance"
            }
        }
    }

    // ---------- map markers ----------
    private fun syncSymbols() {
        val hcScale = if (hc) 1.2f else 1f
        buses.forEach { bus ->
            val fix = fixes[bus.key] ?: return@forEach
            val existing = busSymbols[bus.key]
            val size = (if (bus.key == selectedKey) 1.35f else 1f) * hcScale

            when {
                !isVisible(bus) -> existing?.let {
                    symbolManager.delete(it)
                    busSymbols.remove(bus.key)
                    symbolToKey.remove(it.id)
                }
                existing == null -> {
                    val symbol = symbolManager.create(
                        SymbolOptions()
                            .withLatLng(fix.pos)
                            .withIconImage("bus-${bus.line}")
                            .withIconRotate(fix.heading)
                            .withIconSize(size)
                            .withTextField(bus.line)
                            .withTextFont(textFont)
                            .withTextSize(if (hc) 14f else 12f)
                            .withTextColor(if (hc) "#000000" else "#1A1A1A")
                            .withTextHaloColor("#FFFFFF")
                            .withTextHaloWidth(2f)
                            .withTextOffset(arrayOf(0f, -2.2f))
                    )
                    if (symbol != null) {
                        busSymbols[bus.key] = symbol
                        symbolToKey[symbol.id] = bus.key
                    }
                }
                else -> {
                    existing.latLng = fix.pos
                    existing.iconRotate = fix.heading
                    existing.iconSize = size
                    symbolManager.update(existing)
                }
            }
        }
    }

    private fun restyleLines() {
        if (!::lineManager.isInitialized) return
        val sel = selectedKey?.let { k -> buses.find { it.key == k } }
        routeLines.forEach { rl ->
            val l = rl.annotation ?: return@forEach
            val isSel = sel != null && sel.line == rl.line &&
                    sel.from.stopId == rl.from.stopId && sel.to.stopId == rl.to.stopId
            val inFilter = filter == null || rl.line == filter
            l.lineOpacity = when {
                !inFilter -> 0f
                sel != null && !isSel -> 0.25f
                hc -> 1f
                else -> 0.75f
            }
            l.lineWidth = (if (isSel) 7f else 4f) + if (hc) 2f else 0f
            lineManager.update(l)
        }
    }

    // ---------- camera ----------
    private fun centerOn(latLng: LatLng) {
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(latLng).zoom(12.0).build()
            )
        )
    }

    private fun fitVisible(animated: Boolean = true) {
        if (!::map.isInitialized) return
        val pts = buses.filter { isVisible(it) }
            .flatMap { listOf(it.from, it.to) }
            .map { LatLng(it.latitude, it.longitude) }
        if (pts.size < 2) return
        val bounds = LatLngBounds.Builder().includes(pts).build()
        val bottomPad = maxOf(findViewById<View>(R.id.bottomStack).height, dp(300)) + dp(16)
        val update = CameraUpdateFactory.newLatLngBounds(bounds, dp(40), topInset + dp(150), dp(40), bottomPad)
        if (animated) map.animateCamera(update, 1800) else map.moveCamera(update)
    }

    private fun followCamera() {
        val k = followKey ?: return
        val fix = fixes[k] ?: return
        map.easeCamera(CameraUpdateFactory.newLatLng(fix.pos), 950)
    }

    private fun toggleTilt() {
        if (!::map.isInitialized) return
        tilted = !tilted
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder(map.cameraPosition).tilt(if (tilted) 55.0 else 0.0).build()
            ), 700
        )
        findViewById<View>(R.id.fabTilt).announceForAccessibility(if (tilted) "3D view on" else "3D view off")
    }

    // ---------- selection / filters ----------
    private fun onCardClicked(card: BusCard) {
        if (selectedKey == card.key) clearSelection() else select(card.key)
    }

    private fun select(key: String) {
        selectedKey = key
        followKey = key
        cards.values.forEach { it.selected = it.key == key }
        restyleLines()
        if (mapReady) syncSymbols()
        adapter.refresh()
        adapter.positionOf(key).takeIf { it >= 0 }?.let { rvBuses.smoothScrollToPosition(it) }
        fixes[key]?.let {
            if (::map.isInitialized)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it.pos, 12.5), 900)
        }
        val bus = buses.first { it.key == key }
        tvFollowing.text = "Following ${bus.line}  \u2022  tap to stop"
        tvFollowing.contentDescription = "Following ${bus.label}. Double tap to stop following."
        tvFollowing.visibility = View.VISIBLE
        tvFollowing.announceForAccessibility(
            "Following ${bus.label}, heading to ${fixes[key]?.towards?.stopName ?: bus.to.stopName}"
        )
    }

    private fun stopFollowing() {
        if (followKey == null) return
        followKey = null
        tvFollowing.visibility = View.GONE
    }

    private fun clearSelection() {
        selectedKey = null
        followKey = null
        cards.values.forEach { it.selected = false }
        tvFollowing.visibility = View.GONE
        restyleLines()
        if (mapReady) syncSymbols()
        adapter.refresh()
    }

    private fun setFilter(code: String?) {
        filter = code
        if (selectedKey != null && buses.none { it.key == selectedKey && isVisible(it) }) clearSelection()
        updateChipStyles()
        restyleLines()
        resortCards()
        if (mapReady) syncSymbols()
        fitVisible()
        chipRow.announceForAccessibility(if (code == null) "Showing all lines" else "Showing line $code only")
    }

    private fun togglePanel() {
        panelCollapsed = !panelCollapsed
        rvBuses.visibility = if (panelCollapsed) View.GONE else View.VISIBLE
        findViewById<View>(R.id.panelHandle).announceForAccessibility(
            if (panelCollapsed) "Bus list hidden" else "Bus list shown"
        )
    }

    private fun buildChips() {
        chipRow.removeAllViews()
        chipViews.clear()
        val codes: List<String?> = listOf<String?>(null) + buses.map { it.line }.distinct()
        codes.forEach { code ->
            val tv = TextView(this).apply {
                text = code ?: "All lines"
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                minHeight = dp(48)
                minWidth = dp(64)
                setPadding(dp(16), 0, dp(16), 0)
                isClickable = true
                isFocusable = true
                elevation = dp(4).toFloat()
                setOnClickListener { setFilter(code) }
            }
            chipRow.addView(tv, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(8); bottomMargin = dp(4) })
            chipViews[code] = tv
        }
        updateChipStyles()
    }

    private fun updateChipStyles() {
        chipViews.forEach { (code, tv) ->
            val on = code == filter
            val onColor = code?.let { c -> buses.first { it.line == c }.color } ?: 0xFF0E7C89.toInt()
            tv.background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(if (on) onColor else ContextCompat.getColor(this@BusMapPage, R.color.white))
                setStroke(if (hc) dp(2) else dp(1),
                    if (on) onColor else ContextCompat.getColor(this@BusMapPage, if (hc) R.color.black else R.color.divider_gray))
            }
            tv.setTextColor(if (on) Color.WHITE else ContextCompat.getColor(this, R.color.black))
            tv.contentDescription = (code?.let { "Line $it" } ?: "All lines") +
                    if (on) ", selected" else ", filter"
        }
    }

    // ---------- accessibility: read aloud ----------
    private fun readNearestAloud() {
        val card = selectedKey?.let { cards[it] } ?: shownCards.firstOrNull()
        if (card == null) {
            speaker?.speak("No buses are running right now."); return
        }
        val mins = card.etaSeconds / 60
        val secs = card.etaSeconds % 60
        val where = card.distM?.let { "%.1f kilometres away".format(it / 1000f) }
            ?: "distance unknown until your location is found"
        val prefix = if (selectedKey == null && userLatLng != null) "Nearest bus" else "Bus"
        speaker?.speak(
            "$prefix ${card.label}, ${card.from} to ${card.to}. $where. " +
                    "Next stop ${card.nextStop} in $mins minutes $secs seconds. " +
                    "Step-free boarding. ${card.crowd}."
        )
    }

    // ---------- helpers ----------
    private fun reducedMotion() = AccessibilitySettings.isReduceMotion(this)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    /** Round marker with a heading arrow at the top. Rotate it with the bus bearing. */
    private fun busIcon(color: Int): Bitmap {
        val size = 108
        val cx = size / 2f
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val outline = if (hc) Color.BLACK else Color.WHITE

        val tri = Path().apply {
            moveTo(cx, 3f); lineTo(cx - 17f, 30f); lineTo(cx + 17f, 30f); close()
        }
        p.color = outline; p.style = Paint.Style.FILL_AND_STROKE; p.strokeWidth = 6f
        c.drawPath(tri, p)
        p.style = Paint.Style.FILL; p.color = color
        c.drawPath(tri, p)

        p.color = outline; c.drawCircle(cx, cx, 36f, p)
        p.color = color;   c.drawCircle(cx, cx, 30f, p)

        // tiny bus glyph
        p.color = Color.WHITE
        c.drawRoundRect(cx - 12f, cx - 11f, cx + 12f, cx + 11f, 5f, 5f, p)
        p.color = color
        c.drawRect(cx - 9f, cx - 8f, cx + 9f, cx - 1f, p)
        c.drawCircle(cx - 6f, cx + 5f, 2.5f, p)
        c.drawCircle(cx + 6f, cx + 5f, 2.5f, p)
        return bmp
    }

    private fun stopDot(): Bitmap {
        val size = 48
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = if (hc) Color.BLACK else Color.WHITE
        c.drawCircle(size / 2f, size / 2f, size / 2f, p)
        p.color = if (hc) Color.WHITE else Color.parseColor("#455A64")
        c.drawCircle(size / 2f, size / 2f, size / 2f - 7f, p)
        return bmp
    }
}