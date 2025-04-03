package com.example.lazytraining

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var bleConnector: BleConnector
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_PERMISSIONS = 2

    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectionStatusTextView = findViewById<TextView>(R.id.connection_status)
        val heartRateTextView = findViewById<TextView>(R.id.heart_rate)
        val startButton = findViewById<Button>(R.id.start_recording_button)
        val stopButton = findViewById<Button>(R.id.stop_recording_button)

        bleConnector = BleConnector(this, connectionStatusTextView, heartRateTextView)

        startButton.setOnClickListener {
            if (checkAndRequestPermissions()) {
                bleConnector.startRecording()
                startButton.isEnabled = false
                stopButton.isEnabled = true
                Toast.makeText(this, "Gravação iniciada", Toast.LENGTH_SHORT).show()
            }
        }

        stopButton.setOnClickListener {
            val recordedData = bleConnector.stopRecording()
            saveHeartRateData(recordedData)
            startButton.isEnabled = true
            stopButton.isEnabled = false
            Toast.makeText(this, "Gravação parada e salva", Toast.LENGTH_SHORT).show()
        }

        if (!checkAndRequestPermissions()) {
            Toast.makeText(this, "Por favor, conceda as permissões de Bluetooth e localização", Toast.LENGTH_LONG).show()
        } else {
            checkBluetoothAndConnect()
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsToRequest = mutableListOf<String>()
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        return if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_PERMISSIONS)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                checkBluetoothAndConnect()
            } else {
                Toast.makeText(
                    this,
                    "Permissões de Bluetooth e localização não concedidas. O app não funcionará corretamente.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun checkBluetoothAndConnect() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth não suportado neste dispositivo", Toast.LENGTH_SHORT).show()
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            connectToBleDevice()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                connectToBleDevice()
            } else {
                Toast.makeText(this, "Bluetooth não foi habilitado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectToBleDevice() {
        val macAddress = "FB:2B:28:0D:25:BB" // Substitua pelo endereço MAC do seu dispositivo
        bleConnector.connectToDevice(macAddress)
    }

    private fun saveHeartRateData(data: List<Pair<Long, Int>>) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "HeartRate_$timeStamp.txt"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.bufferedWriter().use { writer ->
                        writer.write("Timestamp,Batimento Cardíaco (bpm)\n")
                        data.forEach { (timestamp, heartRate) ->
                            writer.write("$timestamp,$heartRate\n")
                        }
                    }
                }
                Toast.makeText(this, "Dados salvos em Downloads/$fileName", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Erro ao criar arquivo", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleConnector.disconnect()
    }
}