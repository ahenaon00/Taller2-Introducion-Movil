package com.example.taller2
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
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
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.taller2.databinding.ActivityOsmBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.LatLng
import org.osmdroid.views.overlay.Polyline
import com.google.android.gms.tasks.Task
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.Date

class OsmActivity : AppCompatActivity() {
    val RADIUS_OF_EARTH_METERS = 6371000.0
    private lateinit var binding: ActivityOsmBinding
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var map: MapView
    private var longPressedMarker: Marker? = null
    private var currentMarker: Marker? = null
    private var currentBusqueda: Marker? = null
    private val bogota = GeoPoint(4.62, -74.07)
    private lateinit var geocoder: Geocoder
    private var busqueda: Boolean = false
    private var ultimaUbicacion: Location? = null
    private var primeraApertura: Boolean = true
    private var centrarCamara = false
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var lightEventListener: SensorEventListener
    private var locations = mutableListOf<JSONObject>()
    private lateinit var roadManager: RoadManager
    private var roadOverlay: Polyline? = null

    val locationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ActivityResultCallback {
            if (it.resultCode == RESULT_OK) {
                startLocationUpdates()
            } else {
                //gps is off
            }
        }
    )
    val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            if (it) {
                locationSettings()
            } else {
                //no permission to gps
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOsmBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initSensors()
        initLocation()
        initMap()
        initNetworkPolicy()
        setListenerDireccion()
        setListenerRutas()
    }

    override fun onResume() {
        super.onResume()
        locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        map.onResume()
        map.controller.setZoom(18.0)
        map.controller.animateTo(bogota)
        sensorManager.registerListener(
            lightEventListener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        stopLocationUpdates()
        sensorManager.unregisterListener(lightEventListener)
    }

    private fun initSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!
        lightEventListener = createLightSensorListener()
    }

    private fun initLocation() {
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallback()
        geocoder = Geocoder(baseContext)
    }

    private fun initMap() {
        Configuration.getInstance()
            .load(this, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        map = binding.osmMap
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.overlays.add(createOverlayEvents())
        roadManager = OSRMRoadManager(this, "ANDROID")
    }

    private fun initNetworkPolicy() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    private fun setListenerDireccion() {
        binding.dirInput.setOnEditorActionListener { vista, id, evento ->
            if (id == EditorInfo.IME_ACTION_SEND) {
                val input = binding.dirInput.text.toString()
                if (input == "actual") {
                    busqueda = false
                    updateUI(ultimaUbicacion!!)
                } else {
                    busqueda = true
                    val location = findLocation(input)
                    val loc: Location
                    loc = Location("")
                    if (location != null) {
                        loc.latitude = location.latitude
                        loc.longitude = location.longitude
                    }
                    updateUI(loc)
                }
            }
            true
        }
    }

    private fun setListenerRutas(){
        binding.pintarRutas.setOnClickListener {
            drawRouteLonger()
        }
    }
    private fun createLightSensorListener(): SensorEventListener {
        val ret : SensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if(this@OsmActivity::map.isInitialized){
                    if (event != null) {
                        if(event.values[0] > 5000){
                            map.overlayManager.tilesOverlay.setColorFilter(null)
                        }else{
                            map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                        }
                    }
                }
            }
            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            }
        }
        return ret
    }
    fun findLocation(address : String):LatLng?{
        val addresses = geocoder.getFromLocationName(address, 2)
        if(addresses != null && !addresses.isEmpty()){
            val addr = addresses.get(0)
            val location = LatLng(addr.
            latitude, addr.
            longitude)
            return location
        }
        return null
    }
    fun locationSettings(){
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            startLocationUpdates()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                try {
                    val isr : IntentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettings.launch(isr)
                } catch (sendEx: IntentSender.SendIntentException) {
                    //no hay gps
                }
            }
        }
    }

    private fun createLocationRequest(): LocationRequest {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5000)
            .build()
        return request
    }
    private fun createLocationCallback(): LocationCallback {
        val callback = object : LocationCallback(){
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val loc = result.lastLocation
                if(loc != null){
                    if(ultimaUbicacion != null){
                        if(distance(loc.latitude, loc.longitude, ultimaUbicacion!!.latitude, ultimaUbicacion!!.longitude) > 30) {
                            if(distance(loc.latitude, loc.longitude, ultimaUbicacion!!.latitude, ultimaUbicacion!!.longitude) > 200) {
                                centrarCamara = true
                            }
                            ultimaUbicacion = Location(loc)
                            writeJSONObject()
                        }
                    }
                    ultimaUbicacion = Location(loc)
                    if(busqueda!=true) {
                        updateUI(loc)
                    }
                }
                else {
                    ultimaUbicacion = null
                }
            }
        }
        return callback
    }
    fun updateUI(location : Location){
        val localizacion = GeoPoint(location.latitude, location.longitude)
        val ubicacion = GeoPoint(ultimaUbicacion!!.latitude, ultimaUbicacion!!.longitude)
        val address = findAddress(LatLng(location.latitude, location.longitude))
        val snippet : String
        if(address != null) {
            snippet = address
        }
        else {
            snippet = ""
        }
        if(busqueda == true) {
            addMarker(localizacion, snippet, false)
            map.controller.animateTo(localizacion)
            drawRoute(ubicacion, localizacion)
            Toast.makeText(this, "Desde tu ubicacion al destino hay : " + distance(ultimaUbicacion!!.latitude, ultimaUbicacion!!.longitude, location.latitude, location.longitude) + "metros!", Toast.LENGTH_LONG).show()
        }
        else{
            addMarkerActual(localizacion, snippet)
            if(ultimaUbicacion!=null) {
                if (centrarCamara || primeraApertura) {
                    map.controller.animateTo(localizacion)
                    primeraApertura = false
                    centrarCamara = false
                }
            }
        }

    }
    fun startLocationUpdates(){
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }
    fun stopLocationUpdates(){
        locationClient.removeLocationUpdates(locationCallback)
    }
    fun createOverlayEvents() : MapEventsOverlay {
        val overlayEvents = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }
            override fun longPressHelper(p: GeoPoint?): Boolean {
                if(p!=null) {
                    longPressOnMap(p)
                }
                return true
            }
        })
        return overlayEvents
    }

    fun findAddress (location : LatLng):String?{
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 2)
        if(addresses != null && !addresses.isEmpty()){
            val addr = addresses.get(0)
            val locname = addr.getAddressLine(0)
            return locname
        }
        return null
    }
    fun longPressOnMap(p:GeoPoint){
        val ubicacion = GeoPoint(ultimaUbicacion!!.latitude, ultimaUbicacion!!.longitude)
        if(longPressedMarker!=null)
            map.overlays.remove(longPressedMarker)
        val address = findAddress(LatLng(p.latitude, p.longitude))
        val snippet : String
        if(address!=null) {
            snippet = address
        }else{
            snippet = ""
        }
        drawRoute(p, ubicacion)

        addMarker(p, snippet, true)
    }
    fun addMarker(p:GeoPoint, snippet : String, longPressed : Boolean){

        if(longPressed) {
            longPressedMarker = createMarker(p, snippet, "Destino presionado", R.drawable.baseline_add_location_24)
            if (longPressedMarker != null) {
                map.overlays.remove(currentBusqueda)
                map.overlays.add(longPressedMarker)
                Toast.makeText(this, "Desde tu ubicacion al destino hay : " + distance(ultimaUbicacion!!.latitude, ultimaUbicacion!!.longitude, p.latitude, p.longitude) + "metros!", Toast.LENGTH_LONG).show()
            }
        }else{
            map.overlays.remove(currentBusqueda)
            val searchMarker = createMarker(p, snippet, "Destino buscado", R.drawable.baseline_add_location_24)
            map.overlays.add(searchMarker)
            currentBusqueda = searchMarker
        }
    }
    fun addMarkerActual(p:GeoPoint, snippet : String) {
        if (currentMarker != null) {
            map.overlays.remove(currentMarker)
        }
        val markerActual = createMarker(p, "Snippet", "", R.drawable.baseline_arrow_circle_down_24)
        map.overlays.add(markerActual)
        currentMarker = markerActual
    }
    fun createMarker(p:GeoPoint, title: String, desc: String, iconID : Int) : Marker? {
        var marker : Marker? = null
        if(map!=null) {
            marker = Marker(map)
            if (title != null) marker.setTitle(title)
            if (desc != null) marker.setSubDescription(desc)
            if (iconID != 0) {
                val myIcon = getResources().getDrawable(iconID, this.getTheme())
                marker.setIcon(myIcon)
            }
            marker.setPosition(p)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        return marker
    }

    fun distance(lat1 : Double, long1: Double, lat2:Double, long2:Double) : Double{
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)+
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val result = RADIUS_OF_EARTH_METERS * c
        return Math.round(result*100.0)/100.0
    }

    fun writeJSONObject() {
        val customLocation = LocationClass(
            Date(System.currentTimeMillis()),
            ultimaUbicacion!!.latitude,
            ultimaUbicacion!!.longitude
        )
        locations.add(customLocation.toJSON())
        val filename = "locations.json"
        val file = File(baseContext.getExternalFilesDir(null), filename)
        val output = BufferedWriter(FileWriter(file))
        output.write(locations.toString())
        output.close()
        Log.i("LOCATION", "File modified at path: " + file)
    }
    fun drawRoute(start : GeoPoint, finish : GeoPoint){
        var routePoints = ArrayList<GeoPoint>()
        routePoints.add(start)
        routePoints.add(finish)
        val road = roadManager.getRoad(routePoints)
        Log.i("MapsApp", "Route length: "+road.mLength+" klm")
        Log.i("MapsApp", "Duration: "+road.mDuration/60+" min")
        if(map!=null){
            if(roadOverlay != null){
                map.overlays.remove(roadOverlay)
            }
            roadOverlay = RoadManager.buildRoadOverlay(road)
            roadOverlay!!.getOutlinePaint().setColor(Color.RED)
            roadOverlay!!.getOutlinePaint().setStrokeWidth(10F)
            map.getOverlays().add(roadOverlay)
        }
    }
    fun drawRouteLonger(){
        val filename = "locations.json"
        val file = File(baseContext.getExternalFilesDir(null), filename)
        val input = file.readText()
        val routePoints = arrayListOf<GeoPoint>()
        val array = JSONArray(input)
        for(i in 0..array.length() -1){
            val locationObj = array.getJSONObject(i)
            val latitude = locationObj.getDouble("latitude")
            val longitude = locationObj.getDouble("longitude")
            val geoPoint = GeoPoint(latitude, longitude)
            routePoints.add(geoPoint)
        }

        val road = roadManager.getRoad(routePoints)
        if(map!=null){
            if(roadOverlay != null){
                map.overlays.remove(roadOverlay)
            }
            roadOverlay = RoadManager.buildRoadOverlay(road)
            roadOverlay!!.getOutlinePaint().setColor(Color.RED)
            roadOverlay!!.getOutlinePaint().setStrokeWidth(10F)
            map.getOverlays().add(roadOverlay)
        }
    }
}