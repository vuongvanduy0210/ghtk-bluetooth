package com.duyvv.bluetooth.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.duyvv.bluetooth.domain.BluetoothDeviceDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address
    )
}