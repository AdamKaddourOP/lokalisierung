package com.example.take2.storage

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class DataStorageManager(private val context: Context) {

    private val fileName = "sensor_data.json"
    private val file: File = File(context.filesDir, fileName)

    fun saveData(sensorType: String, data: String) {
        val formattedTimestamp = getCurrentFormattedTimestamp()
        val jsonObject = JSONObject().apply {
            put("sensorType", sensorType)
            put("data", data)
            put("timestamp", formattedTimestamp)
        }
        file.writeText(jsonObject.toString())
    }

    fun readData(): String {
        return if (file.exists()) {
            file.readText()
        } else {
            ""
        }
    }

    private fun getCurrentFormattedTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(System.currentTimeMillis())
    }
}
