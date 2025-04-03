package com.example.lazytraining

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.util.Log
import java.util.*

class BluetoothHelper(context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private val deviceMacAddress = "FB:2B:28:0D:25:BB" // MAC do XOSS X2
    private val heartRateUUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

    fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        val scanFilter = ScanFilter.Builder().setDeviceAddress(deviceMacAddress).build()
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        scanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device.address == deviceMacAddress) {
                Log.d("BluetoothHelper", "Dispositivo encontrado: ${device.name} - ${device.address}")
                bluetoothGatt = device.connectGatt(null, false, gattCallback)
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(this)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BluetoothHelper", "Conectado ao dispositivo BLE")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BluetoothHelper", "Desconectado do dispositivo BLE")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            for (service in gatt.services) {
                for (characteristic in service.characteristics) {
                    if (characteristic.uuid == heartRateUUID) {
                        Log.d("BluetoothHelper", "Característica de frequência cardíaca encontrada")
                        gatt.setCharacteristicNotification(characteristic, true)
                    }
                }
            }
        }
    }
}
