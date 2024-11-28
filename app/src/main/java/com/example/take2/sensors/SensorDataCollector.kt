package com.example.take2.sensors

import android.content.Context
import android.hardware.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class SensorDataCollector(private val context: Context) : SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    var onSensorDataCollected: ((sensorType: String, data: String) -> Unit)? = null
    var onLocationUpdated: ((latitude: Double, longitude: Double) -> Unit)? = null

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latitude = location.latitude
                    val longitude = location.longitude
                    onLocationUpdated?.invoke(latitude, longitude)
                    val locationData = "GPS: Lat=$latitude, Lon=$longitude"
                    onSensorDataCollected?.invoke("GPS", locationData)
                }
            }
        }
    }

    fun startCollecting() {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        startLocationUpdates()
    }

    fun stopCollecting() {
        sensorManager.unregisterListener(this)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // Hohe Genauigkeit für GPS-Standort
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            val sensorData = when (sensorEvent.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> "Accelerometer: x=${sensorEvent.values[0]}, y=${sensorEvent.values[1]}, z=${sensorEvent.values[2]}"
                Sensor.TYPE_GYROSCOPE -> "Gyroscope: x=${sensorEvent.values[0]}, y=${sensorEvent.values[1]}, z=${sensorEvent.values[2]}"
                Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometer: x=${sensorEvent.values[0]}, y=${sensorEvent.values[1]}, z=${sensorEvent.values[2]}"
                else -> ""
            }
            onSensorDataCollected?.invoke(sensorEvent.sensor.name, sensorData)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
