package com.duyvv.bluetooth.receiver

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.duyvv.bluetooth.data.toBluetoothDeviceDomain
import com.duyvv.bluetooth.domain.BluetoothDeviceDomain

class BluetoothReceiver(
    private val handleBluetoothState: (Int) -> Unit,
    private val handleBluetoothAction: (BluetoothDeviceDomain) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            when (it.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state =
                        it.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    handleBluetoothState.invoke(state)
                }

                else -> {
                    when (it.action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            // Found device
                            val device =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getParcelableExtra(
                                        BluetoothDevice.EXTRA_DEVICE,
                                        BluetoothDevice::class.java
                                    )
                                } else {
                                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                                }
                            device?.let { dv ->
                                handleBluetoothAction(dv.toBluetoothDeviceDomain())
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}