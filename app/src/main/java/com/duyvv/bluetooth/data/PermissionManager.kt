package com.duyvv.bluetooth.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

fun hasBluetoothConnectPermission(context: Context): Boolean {
    return if (isSDKVersionFromS) {
        hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        true
    }
}

fun hasBluetoothScanPermission(context: Context): Boolean {
    return if (isSDKVersionFromS) {
        hasPermission(context, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        true
    }
}

fun hasPermission(context: Context, permission: String): Boolean {
    return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}

val isSDKVersionFromS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S