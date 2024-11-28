package com.example.take2

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.take2.sensors.SensorDataCollector
import com.example.take2.storage.DataStorageManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.MapController
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var dataStorageManager: DataStorageManager
    private lateinit var sensorDataCollector: SensorDataCollector
    private lateinit var startStopButton: Button
    private lateinit var newButton: Button
    private lateinit var sensorDataTextView: TextView
    private lateinit var frequencySpinner: Spinner

    private lateinit var mapView: MapView
    private lateinit var mapController: MapController
    private var currentLocationMarker: Marker? = null

    private var isCollecting: Boolean = false
    private val sensorDataMap = mutableMapOf<String, String>()

    private val CREATE_FILE_REQUEST_CODE = 1
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Anfrage für Standortberechtigungen
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        // Initialisiere UI-Elemente
        startStopButton = findViewById(R.id.startStopButton)
        newButton = findViewById(R.id.newButton)
        sensorDataTextView = findViewById(R.id.sensorDataTextView)
        frequencySpinner = findViewById(R.id.frequencySpinner)

        // Initialisiere Speicher-Manager und Sensor-Daten-Sammler
        dataStorageManager = DataStorageManager(this)
        sensorDataCollector = SensorDataCollector(this)

        // Initialisiere OSMDroid-Konfiguration
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", Context.MODE_PRIVATE))

        // Initialisiere MapView und füge es dem Layout hinzu
        mapView = MapView(this)
        mapView.setMultiTouchControls(true)
        mapController = mapView.controller as MapController
        mapController.setZoom(15.0)

        findViewById<ViewGroup>(R.id.mapContainer).addView(mapView)
        setDefaultMapLocation()

        // Setze den Spinner für die Frequenz
        val frequencies = arrayOf("Normal", "UI", "Game", "Fastest")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, frequencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        frequencySpinner.adapter = adapter

        // Start-/Stopp-Button für die Datensammlung
        startStopButton.setOnClickListener {
            toggleDataCollection()
        }

        // Neuer Button zum Speichern der Daten
        newButton.setOnClickListener {
            openFilePicker()
        }

        // Sensor-Daten-Listener
        sensorDataCollector.onSensorDataCollected = { sensorType, data ->
            runOnUiThread {
                sensorDataMap[sensorType] = data
                updateSensorDataTextView()
                dataStorageManager.saveData(sensorType, data)
            }
        }

        // Aktualisierung der Karte bei Standortänderungen
        sensorDataCollector.onLocationUpdated = { latitude, longitude ->
            runOnUiThread {
                updateMapLocation(latitude, longitude)
            }
        }
    }

    // Startet oder stoppt die Datensammlung
    private fun toggleDataCollection() {
        if (isCollecting) {
            sensorDataCollector.stopCollecting()
            startStopButton.text = "Start Collection"
        } else {
            sensorDataCollector.startCollecting()
            startStopButton.text = "Stop Collection"
        }
        isCollecting = !isCollecting
    }

    // Aktualisiert das TextView mit den aktuellen Sensordaten
    private fun updateSensorDataTextView() {
        val displayText = sensorDataMap.entries.joinToString(separator = "\n") { (sensor, data) ->
            "$sensor: $data"
        }
        sensorDataTextView.text = displayText
    }

    // Öffnet einen Dateiauswahldialog, damit der Benutzer den Speicherort der Daten auswählen kann
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "sensor_data.json")
        }
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE)
    }

    // Speichert gesammelte Daten an der ausgewählten Datei-URI
    private fun saveDataToUri(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val data = dataStorageManager.readData()
                outputStream.write(data.toByteArray())
                Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("SaveData", "Failed to save data: ${e.message}")
            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
        }
    }

    // Handhabt das Ergebnis des Dateiauswahldialogs
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                saveDataToUri(uri)
            }
        }
    }

    // Setzt den Startpunkt auf der Karte
    private fun setDefaultMapLocation() {
        val startPoint = GeoPoint(52.5200, 13.4050) // Beispielstandort (Berlin)
        mapController.setCenter(startPoint)
    }

    // Aktualisiert den Standort auf der Karte
    private fun updateMapLocation(latitude: Double, longitude: Double) {
        val geoPoint = GeoPoint(latitude, longitude)
        mapController.setCenter(geoPoint)

        currentLocationMarker?.let { mapView.overlays.remove(it) }

        currentLocationMarker = Marker(mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Current Location"
        }
        mapView.overlays.add(currentLocationMarker)
        mapView.invalidate()
    }

    // Anfrageergebnis für Standortberechtigungen
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Berechtigung erteilt, Standortabfrage kann beginnen
            sensorDataCollector.startCollecting()
        } else {
            Toast.makeText(this, "Standortberechtigung erforderlich", Toast.LENGTH_SHORT).show()
        }
    }
}
