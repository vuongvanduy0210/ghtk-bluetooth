package com.duyvv.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

/*data class BltDevice(
    val name: String?,
    val address: String
) {
    companion object {
        @SuppressLint("MissingPermission")
        fun from(bluetoothDevice: BluetoothDevice): BltDevice {
            return BltDevice(
                name = bluetoothDevice.name,
                address = bluetoothDevice.address
            )
        }
    }
}*/

sealed class DeviceItem {
    data class Header(val title: String) : DeviceItem()
    data class Device(val bluetoothDevice: BluetoothDevice) : DeviceItem()
}
