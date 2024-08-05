package com.duyvv.bluetooth.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import com.duyvv.bluetooth.domain.BluetoothController
import com.duyvv.bluetooth.domain.BluetoothDeviceDomain
import com.duyvv.bluetooth.domain.ConnectionResult
import com.duyvv.bluetooth.domain.DeviceItem
import com.duyvv.bluetooth.receiver.BluetoothReceiver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

    private val bluetoothAdapter by lazy {
        context.getSystemService(BluetoothManager::class.java)?.adapter
    }

    private val _bluetoothState = MutableStateFlow(BluetoothAdapter.STATE_OFF)
    override val bluetoothState = _bluetoothState.asStateFlow()

    private val _bluetoothDevices = MutableStateFlow<List<DeviceItem>>(emptyList())
    override val bluetoothDevices: StateFlow<List<DeviceItem>> = _bluetoothDevices.asStateFlow()

    private val bluetoothReceiver = BluetoothReceiver(
        handleBluetoothState = { state ->
            _bluetoothState.value = state
        },
        handleBluetoothAction = { device ->
            val isPaired =
                bluetoothAdapter?.bondedDevices?.any { it.address == device.address }
            val alreadyAdded =
                bluetoothDevices.value.any { bluetoothDevice ->
                    bluetoothDevice is DeviceItem.Device &&
                            bluetoothDevice.bluetoothDevice.address == device.address
                }
            if (isPaired == false && !alreadyAdded) {
                _bluetoothDevices.value = bluetoothDevices.value + DeviceItem.Device(device)
            }
        }
    )

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }

    override fun loadPairedDevices() {
        if (!hasBluetoothConnectPermission(context)) return
        val items = mutableListOf<DeviceItem>()
        items.add(DeviceItem.Header("Paired Devices"))
        val pairedDevices = bluetoothAdapter?.bondedDevices
        items.addAll(
            pairedDevices?.map { device ->
                DeviceItem.Device(BluetoothDeviceDomain.from(device))
            } ?: emptyList()
        )
        items.add(DeviceItem.Header("Available Devices"))
        _bluetoothDevices.value = items
    }

    override fun clearListDevices() {
        _bluetoothDevices.value = emptyList()
    }

    override fun startDiscovery() {
        if (!hasBluetoothScanPermission(context)) return

        bluetoothAdapter?.let { adapter ->
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            adapter.startDiscovery()
        }
    }

    override fun stopDiscovery() {
        if (!hasBluetoothScanPermission(context)) return
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> {
        TODO("Not yet implemented")
    }

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        TODO("Not yet implemented")
    }

    override fun closeConnection() {
        TODO("Not yet implemented")
    }

    override fun release() {
        context.unregisterReceiver(bluetoothReceiver)
    }
}
