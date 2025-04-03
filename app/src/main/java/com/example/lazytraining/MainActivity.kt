package com.example.lazytraining

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnScan: Button
    private lateinit var txtDevices: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        btnScan = findViewById(R.id.btn_scan)
        txtDevices = findViewById(R.id.txt_devices)

        btnScan.setOnClickListener {
            txtDevices.text = "Buscando dispositivos...\n"

            BluetoothHelper.startScan(this) { device ->
                runOnUiThread {
                    val deviceName = device.name ?: "Desconhecido"
                    val deviceAddress = device.address
                    txtDevices.append("$deviceName - $deviceAddress\n")
                }
            }
        }
    }

    private fun checkPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 101)
        }
    }
}