package com.example.codiecontroller

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val SERVICE_UUID = UUID.fromString("52af0001-978a-628d-c845-0a104ca2b8dd")
    private val WRITE_UUID = UUID.fromString("52af0002-978a-628d-c845-0a104ca2b8dd")
    private val NOTIFY_UUID = UUID.fromString("52af0003-978a-628d-c845-0a104ca2b8dd")

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()

        val btnForward = findViewById<Button>(R.id.btnForward)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnLeft = findViewById<Button>(R.id.btnLeft)
        val btnRight = findViewById<Button>(R.id.btnRight)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnForward.setOnClickListener { sendCmd("F") }
        btnBack.setOnClickListener { sendCmd("B") }
        btnLeft.setOnClickListener { sendCmd("L") }
        btnRight.setOnClickListener { sendCmd("R") }
        btnStop.setOnClickListener { sendCmd("S") }

        // Auto-connect: find device by name "Codie" and connect (simple approach)
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val paired = adapter?.bondedDevices
        val codie = paired?.firstOrNull { it.name?.contains("Codie", true) == true }
        codie?.let { connectToDevice(it) } ?: run {
            Toast.makeText(this, "Codie not paired. Pair in system Bluetooth first.", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestPermissions() {
        val perms = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        ActivityCompat.requestPermissions(this, perms, 101)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                }
            }
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val svc: BluetoothGattService? = gatt.getService(SERVICE_UUID)
                writeChar = svc?.getCharacteristic(WRITE_UUID)
                val notify = svc?.getCharacteristic(NOTIFY_UUID)
                if (notify != null) {
                    gatt.setCharacteristicNotification(notify, true)
                }
            }
        })
    }

    private fun sendCmd(c: String) {
        val b = c.toByteArray()
        writeChar?.setValue(b)
        writeChar?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        bluetoothGatt?.writeCharacteristic(writeChar)
    }
}
