package com.duyvv.bluetooth.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

data class BluetoothDeviceDomain(
    val name: String?,
    val address: String
) {
    companion object {
        @SuppressLint("MissingPermission")
        fun from(bluetoothDevice: BluetoothDevice): BluetoothDeviceDomain {
            return BluetoothDeviceDomain(
                name = bluetoothDevice.name,
                address = bluetoothDevice.address
            )
        }
    }
}

sealed class DeviceItem {
    data class Header(val title: String) : DeviceItem()
    data class Device(val bluetoothDevice: BluetoothDeviceDomain) : DeviceItem()
}
