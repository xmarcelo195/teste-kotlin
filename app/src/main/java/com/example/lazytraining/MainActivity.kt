package com.example.lazytraining

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var bleConnector: BleConnector
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_PERMISSIONS = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectionStatusTextView = findViewById<TextView>(R.id.connection_status)
        val heartRateTextView = findViewById<TextView>(R.id.heart_rate)
        bleConnector = BleConnector(this, connectionStatusTextView, heartRateTextView)

        if (!hasPermissions()) {
            requestPermissions()
        } else {
            checkBluetoothAndConnect()
        }
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkBluetoothAndConnect()
        } else {
            Toast.makeText(this, "Permissões necessárias não concedidas", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        bleConnector.disconnect()
    }
}