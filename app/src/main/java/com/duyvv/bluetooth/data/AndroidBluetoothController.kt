package com.duyvv.bluetooth.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import com.duyvv.bluetooth.domain.BluetoothController
import com.duyvv.bluetooth.domain.BluetoothDeviceDomain
import com.duyvv.bluetooth.domain.BluetoothMessage
import com.duyvv.bluetooth.domain.ConnectionResult
import com.duyvv.bluetooth.domain.DeviceItem
import com.duyvv.bluetooth.receiver.BluetoothConnectReceiver
import com.duyvv.bluetooth.receiver.BluetoothReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

    private val bluetoothAdapter by lazy {
        context.getSystemService(BluetoothManager::class.java)?.adapter
    }

    private var dataTransferService: BluetoothDataTransferService? = null

    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    override val error: SharedFlow<String> = _error.asSharedFlow()

    private val _bluetoothState = MutableStateFlow(
        if (isBluetoothEnabled) BluetoothAdapter.STATE_ON else BluetoothAdapter.STATE_OFF
    )
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

    private val bluetoothConnectReceiver =
        BluetoothConnectReceiver { isConnected, bluetoothDevice ->
            if (bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true) {
                _isConnected.update { isConnected }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    _error.emit("Can't connect to a non-paired device.")
                }
            }
        }

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null

    init {
        context.registerReceiver(
            bluetoothReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
        )

        context.registerReceiver(
            bluetoothConnectReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
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
        return flow {
            if (!hasBluetoothConnectPermission(context)) {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }

            currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "BluetoothChatApp",
                UUID.fromString(SERVICE_UUID)
            )

            var shouldLoop = true
            while (shouldLoop) {
                currentClientSocket = try {
                    currentServerSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }
                emit(ConnectionResult.ConnectionEstablished)
                currentClientSocket?.let {
                    currentServerSocket?.close()
                    val service = BluetoothDataTransferService(it)
                    dataTransferService = service

                    emitAll(
                        service
                            .listenForIncomingMessages()
                            .map { message ->
                                ConnectionResult.TransferSucceeded(message)
                            }
                    )
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if (!hasBluetoothConnectPermission(context)) {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }

            currentClientSocket = bluetoothAdapter
                ?.getRemoteDevice(device.address)
                ?.createRfcommSocketToServiceRecord(
                    UUID.fromString(SERVICE_UUID)
                )
            stopDiscovery()

            currentClientSocket?.let { socket ->
                try {
                    socket.connect()
                    emit(ConnectionResult.ConnectionEstablished)
                    BluetoothDataTransferService(socket).also {
                        dataTransferService = it
                        emitAll(
                            it.listenForIncomingMessages()
                                .map { ConnectionResult.TransferSucceeded(it) }
                        )
                    }
                } catch (e: IOException) {
                    socket.close()
                    currentServerSocket = null
                    emit(ConnectionResult.Error("Connection was interrupted"))
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun sendMessage(message: String): BluetoothMessage? {
        if (!hasBluetoothConnectPermission(context) || dataTransferService == null) {
            return null
        }

        val bluetoothMessage = BluetoothMessage(
            message = message,
            senderName = bluetoothAdapter?.name ?: "Unknown name",
            isFromLocalUser = true
        )

        dataTransferService?.sendMessage(bluetoothMessage.toByteArray())

        return bluetoothMessage
    }

    override fun closeConnection() {
        currentServerSocket?.close()
        currentClientSocket?.close()
        currentServerSocket = null
        currentClientSocket = null
    }

    override fun release() {
        context.unregisterReceiver(bluetoothReceiver)
        context.unregisterReceiver(bluetoothConnectReceiver)
        closeConnection()
    }

    companion object {
        const val SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }
}
