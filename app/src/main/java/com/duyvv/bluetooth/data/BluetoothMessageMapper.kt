package com.duyvv.bluetooth.data

import com.duyvv.bluetooth.domain.BluetoothMessage
import com.google.gson.Gson

fun String.toBluetoothMessage(isFromLocalUser: Boolean): BluetoothMessage {
    return Gson().fromJson(
        this,
        BluetoothMessage::class.java
    ).copy(isFromLocalUser = isFromLocalUser)
}

fun BluetoothMessage.toByteArray(): ByteArray {
    val jsonString = Gson().toJson(this)
    return jsonString.toByteArray()
}