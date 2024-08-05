package com.duyvv.bluetooth.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {

    val bluetoothState: StateFlow<Int>

    val bluetoothDevices: StateFlow<List<DeviceItem>>

    fun loadPairedDevices()
    fun clearListDevices()

    fun startDiscovery()
    fun stopDiscovery()

    fun startBluetoothServer(): Flow<ConnectionResult>
    fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult>

    fun closeConnection()
    fun release()
}