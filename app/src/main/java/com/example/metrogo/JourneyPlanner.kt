package com.example.metrogo

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.LineManager
import org.maplibre.android.plugins.annotation.LineOptions
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

class JourneyPlanner : AppCompatActivity() {

    private lateinit var tvFromStation: TextView
    private lateinit var tvToStation: TextView
    private lateinit var inputFrom: LinearLayout
    private lateinit var inputTo: LinearLayout
    private lateinit var btnSwap: ImageButton
    private lateinit var btnFindRoutes: Button
    private lateinit var btnBack: ImageButton
    private lateinit var tvMatchingSubtitle: TextView
    private lateinit var tvNoMatches: TextView
    private lateinit var rvMatchingBuses: RecyclerView
    private lateinit var rvOtherBuses: RecyclerView

    // Map
    private var mapView: MapView? = null
    private var map: MapLibreMap? = null
    private var symbolManager: SymbolManager? = null
    private var lineManager: LineManager? = null
    private var mapReady = false

    // Must exist in the glyphs of the style you load. If labels go blank after
    // changing the style URL, change this to a font that style provides.
    private val textFont = arrayOf("Noto Sans Regular")

    private companion object {
        const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
        const val ICON_STOP = "planner-stop-icon"
        const val ICON_FROM = "planner-from-icon"
        const val ICON_TO = "planner-to-icon"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_journey_planner)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val circleSettings = findViewById<FrameLayout>(R.id.circleSettings)
        circleSettings.setOnClickListener {
            intent = Intent(this, SettingsPage::class.java)
            startActivity(intent)
        }

        val circleProfile = findViewById<FrameLayout>(R.id.circleProfile)
        circleProfile.setOnClickListener {
            val intent = Intent(this, ProfileManagement::class.java)
            startActivity(intent)
        }

        val circleNotification = findViewById<FrameLayout>(R.id.circleNotification)
        circleNotification.setOnClickListener{
            val intent = Intent(this , NotificationPage::class.java)
            startActivity(intent)
        }

        initViews()
        setupMap(savedInstanceState)
        setupClickListeners()
        setupBusLists()

        findRoutes()
    }

    private fun initViews() {
        tvFromStation = findViewById(R.id.tvFromStation)
        tvToStation = findViewById(R.id.tvToStation)
        inputFrom = findViewById(R.id.inputFrom)
        inputTo = findViewById(R.id.inputTo)
        btnSwap = findViewById(R.id.btnSwap)
        btnFindRoutes = findViewById(R.id.btnFindRoutes)
        btnBack = findViewById(R.id.btnBack)

        tvMatchingSubtitle = findViewById(R.id.tvMatchingSubtitle)
        tvNoMatches = findViewById(R.id.tvNoMatches)
        rvMatchingBuses = findViewById(R.id.rvMatchingBuses)
        rvOtherBuses = findViewById(R.id.rvOtherBuses)
    }

    // ---------------------------------------------------------------- map

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMap(savedInstanceState: Bundle?) {
        val container = findViewById<FrameLayout>(R.id.mapContainer)

        // Texture mode renders correctly inside a scrolling parent (and respects the
        // container's rounded corners). MapLibre.getInstance() is called in MetroGoApp.
        val options = MapLibreMapOptions.createFromAttributes(this, null).textureMode(true)
        val mv = MapView(this, options)
        container.addView(
            mv,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        mapView = mv

        // The map sits inside a NestedScrollView. Without this, dragging the map would
        // scroll the page instead of panning the map.
        mv.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.parent?.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        mv.onCreate(savedInstanceState)
        mv.getMapAsync { maplibreMap ->
            map = maplibreMap
            maplibreMap.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                style.addImage(ICON_STOP, dot(Color.parseColor("#455A64"), 36))
                style.addImage(ICON_FROM, dot(Color.parseColor("#00897B"), 64))
                style.addImage(ICON_TO, dot(Color.parseColor("#D32F2F"), 64))

                // Line manager first so the route line is drawn underneath the stop markers.
                lineManager = LineManager(mv, maplibreMap, style)
                symbolManager = SymbolManager(mv, maplibreMap, style).apply {
                    iconAllowOverlap = true
                    textAllowOverlap = true
                }

                mapReady = true
                updateMap(tvFromStation.text.toString(), tvToStation.text.toString())
            }
        }
    }

    /**
     * Redraws the stops for the current selection: every stop as a small dot, the From stop
     * in teal, the To stop in red, a straight line between them, and the camera fitted to both.
     */
    private fun updateMap(from: String, to: String) {
        if (!mapReady) return
        val m = map ?: return
        val sm = symbolManager ?: return
        val lm = lineManager ?: return

        sm.deleteAll()
        lm.deleteAll()

        val stops = TransportRouteRepository.busStops
        val fromStop = stops.find { it.stopName.equals(from, ignoreCase = true) }
        val toStop = stops.find { it.stopName.equals(to, ignoreCase = true) }

        stops.forEach { s ->
            val icon = when (s.stopId) {
                fromStop?.stopId -> ICON_FROM
                toStop?.stopId -> ICON_TO
                else -> ICON_STOP
            }
            sm.create(
                SymbolOptions()
                    .withLatLng(LatLng(s.latitude, s.longitude))
                    .withIconImage(icon)
                    .withTextField(s.stopName)
                    .withTextFont(textFont)
                    .withTextSize(11f)
                    .withTextOffset(arrayOf(0f, 1.6f))
            )
        }

        val points: List<LatLng>
        if (fromStop != null && toStop != null && fromStop.stopId != toStop.stopId) {
            val a = LatLng(fromStop.latitude, fromStop.longitude)
            val b = LatLng(toStop.latitude, toStop.longitude)
            lm.create(
                LineOptions()
                    .withLatLngs(listOf(a, b))
                    .withLineColor("#00897B")
                    .withLineWidth(4f)
            )
            points = listOf(a, b)
        } else {
            // Nothing valid selected: show every stop.
            points = stops.map { LatLng(it.latitude, it.longitude) }
        }

        val bounds = LatLngBounds.Builder().apply { points.forEach { include(it) } }.build()
        m.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120), 600)
    }

    private fun dot(color: Int, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.WHITE
        c.drawCircle(size / 2f, size / 2f, size / 2f, p)
        p.color = color
        c.drawCircle(size / 2f, size / 2f, size / 2f - 6f, p)
        return bmp
    }

    // ---------------------------------------------------------------- planner

    private fun setupClickListeners() {
        inputFrom.setOnClickListener {
            showStationSelectionDialog("From")
        }

        inputTo.setOnClickListener {
            showStationSelectionDialog("To")
        }

        btnSwap.setOnClickListener {
            swapLocations()
        }

        btnFindRoutes.setOnClickListener {
            findRoutes()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupBusLists() {
        rvMatchingBuses.layoutManager = LinearLayoutManager(this)
        rvOtherBuses.layoutManager = LinearLayoutManager(this)
        rvMatchingBuses.isNestedScrollingEnabled = false
        rvOtherBuses.isNestedScrollingEnabled = false
    }

    private fun showStationSelectionDialog(type: String) {
        val stations = TransportRouteRepository.stationNames.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select $type Station")
            .setItems(stations) { _, which ->
                val selectedStation = stations[which]
                if (type == "From") {
                    tvFromStation.text = selectedStation
                } else {
                    tvToStation.text = selectedStation
                }

                findRoutes()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun swapLocations() {
        val fromText = tvFromStation.text.toString()
        val toText = tvToStation.text.toString()

        tvFromStation.text = toText
        tvToStation.text = fromText

        Toast.makeText(this, "Locations swapped", Toast.LENGTH_SHORT).show()
        findRoutes()
    }

    private fun findRoutes() {
        val from = tvFromStation.text.toString()
        val to = tvToStation.text.toString()

        if (from == to) {
            Toast.makeText(this, "Please select different locations", Toast.LENGTH_SHORT).show()
            return
        }

        val matchingRoutes = TransportRouteRepository.scheduleDetailsBetween(from, to)
        val otherRoutes = TransportRouteRepository.scheduleDetailsExcluding(from, to)

        tvMatchingSubtitle.text = "$from \u2192 $to"
        tvNoMatches.visibility = if (matchingRoutes.isEmpty()) View.VISIBLE else View.GONE
        rvMatchingBuses.visibility = if (matchingRoutes.isEmpty()) View.GONE else View.VISIBLE

        rvMatchingBuses.adapter = BusRouteAdapter(matchingRoutes) { selected ->
            openPayment(selected)
        }
        rvOtherBuses.adapter = BusRouteAdapter(otherRoutes) { selected ->
            openPayment(selected)
        }

        updateMap(from, to)
    }

    private fun openPayment(selected: TransportRouteRepository.ScheduleDetails) {
        val intent = Intent(this, PaymentActivity::class.java)
        intent.putExtra(PaymentActivity.EXTRA_SCHEDULE_ID, selected.schedule.scheduleId)
        startActivity(intent)
    }

    // ---------------------------------------------------------------- map lifecycle

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        symbolManager?.onDestroy()
        lineManager?.onDestroy()
        mapView?.onDestroy()
        super.onDestroy()
    }
}