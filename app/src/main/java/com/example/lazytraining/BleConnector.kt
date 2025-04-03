package com.example.lazytraining

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import android.widget.TextView
import java.util.UUID

class BleConnector(private val context: Context, private val statusTextView: TextView, private val heartRateTextView: TextView) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var isRecording = false
    private val heartRateData = mutableListOf<Pair<Long, Int>>()

    private val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
    private val HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    (context as? MainActivity)?.runOnUiThread {
                        statusTextView.text = "Status da Conexão: Conectado"
                    }
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    (context as? MainActivity)?.runOnUiThread {
                        statusTextView.text = "Status da Conexão: Desconectado"
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID)
                if (heartRateService != null) {
                    val heartRateCharacteristic = heartRateService.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
                    if (heartRateCharacteristic != null) {
                        gatt.setCharacteristicNotification(heartRateCharacteristic, true)
                        val descriptor = heartRateCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    } else {
                        Log.e("BLE", "Característica de batimento não encontrada")
                    }
                } else {
                    Log.e("BLE", "Serviço de batimento não encontrado")
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                val heartRate = parseHeartRate(characteristic)
                (context as? MainActivity)?.runOnUiThread {
                    heartRateTextView.text = "Batimento Cardíaco: $heartRate bpm"
                }
                if (isRecording) {
                    heartRateData.add(Pair(System.currentTimeMillis(), heartRate))
                }
            }
        }
    }

    private fun parseHeartRate(characteristic: BluetoothGattCharacteristic): Int {
        val flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
        val format = if (flag and 0x01 != 0) BluetoothGattCharacteristic.FORMAT_UINT16 else BluetoothGattCharacteristic.FORMAT_UINT8
        return characteristic.getIntValue(format, 1)
    }

    fun connectToDevice(macAddress: String) {
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(macAddress)
        if (device == null) {
            Log.e("BLE", "Dispositivo não encontrado.")
            return
        }
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    fun startRecording() {
        isRecording = true
        heartRateData.clear()
    }

    fun stopRecording(): List<Pair<Long, Int>> {
        isRecording = false
        return heartRateData.toList()
    }
}