package com.duyvv.bluetooth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duyvv.bluetooth.domain.BluetoothController
import com.duyvv.bluetooth.domain.BluetoothDeviceDomain
import com.duyvv.bluetooth.domain.BluetoothMessage
import com.duyvv.bluetooth.domain.ConnectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
) : ViewModel() {

    val bluetoothDevices = bluetoothController.bluetoothDevices

    val bluetoothState = bluetoothController.bluetoothState

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting = _isConnecting.asStateFlow()

    private val _error = MutableSharedFlow<String?>()
    val error = _error.asSharedFlow()

    private val _messages = MutableStateFlow<List<BluetoothMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private var deviceConnectionJob: Job? = null

    init {
        bluetoothController.isConnected.onEach { isConnected ->
            _isConnected.update { isConnected }
        }
        bluetoothController.error.onEach { error ->
            _error.emit(error)
        }
    }

    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when (result) {
                ConnectionResult.ConnectionEstablished -> {
                    _isConnected.value = true
                    _isConnecting.value = false
                    _error.emit(null)
                }

                is ConnectionResult.Error -> {
                    _isConnected.value = false
                    _isConnecting.value = false
                    _error.emit(result.message)
                }

                is ConnectionResult.TransferSucceeded -> {
                    _messages.value += result.message
                }
            }
        }
            .catch {
                bluetoothController.closeConnection()
                _isConnected.value = false
                _isConnecting.value = false
            }
            .launchIn(viewModelScope)
    }

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

    fun waitForIncomingConnections() {
        _isConnecting.value = true
        deviceConnectionJob = bluetoothController
            .startBluetoothServer()
            .listen()
    }

    fun connectDevice(device: BluetoothDeviceDomain) {
        _isConnecting.value = true
        deviceConnectionJob = bluetoothController
            .connectToDevice(device)
            .listen()
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            val bluetoothMessage = bluetoothController.sendMessage(message)
            if (bluetoothMessage != null) {
                _messages.value += bluetoothMessage
            }
        }
    }

    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        bluetoothController.closeConnection()
        _isConnected.value = false
        _isConnecting.value = false
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
        deviceConnectionJob?.cancel()
    }
}