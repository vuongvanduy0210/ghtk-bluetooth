package com.duyvv.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothViewModel : ViewModel() {

    private val _deviceItems = MutableStateFlow<List<DeviceItem>>(emptyList())
    val deviceItems: StateFlow<List<DeviceItem>> = _deviceItems.asStateFlow()

    @SuppressLint("MissingPermission")
    fun loadDevices(bluetoothAdapter: BluetoothAdapter) {
        val items = mutableListOf<DeviceItem>()
        items.add(DeviceItem.Header("Paired Devices"))
        val pairedDevices = bluetoothAdapter.bondedDevices
        pairedDevices.forEach { device ->
            items.add(DeviceItem.Device(device))
        }
        items.add(DeviceItem.Header("Available Devices"))
        _deviceItems.value = items
    }

    fun addDevice(device: DeviceItem) {
        _deviceItems.value = mutableListOf<DeviceItem>().apply {
            addAll(_deviceItems.value)
            add(device)
        }
    }

    fun clearItems() {
        _deviceItems.value = emptyList()
    }
}