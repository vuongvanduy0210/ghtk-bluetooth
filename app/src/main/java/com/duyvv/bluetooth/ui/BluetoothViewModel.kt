package com.duyvv.bluetooth.ui

import androidx.lifecycle.ViewModel
import com.duyvv.bluetooth.domain.BluetoothController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
) : ViewModel() {

    val bluetoothDevices = bluetoothController.bluetoothDevices

    val bluetoothState = bluetoothController.bluetoothState

    private fun loadDevices() {
        bluetoothController.loadPairedDevices()
    }

    fun clearItems() {
        bluetoothController.clearListDevices()
    }

    fun startDiscovery() {
        loadDevices()
        bluetoothController.startDiscovery()
    }

    fun stopDiscovery() {
        bluetoothController.stopDiscovery()
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}