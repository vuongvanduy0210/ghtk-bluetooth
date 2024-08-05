package com.duyvv.bluetooth

import DeviceAdapter
import DeviceListener
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.duyvv.bluetooth.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity(), DeviceListener {

    private lateinit var binding: ActivityMainBinding

    private var bluetoothAdapter: BluetoothAdapter? = null

    private val deviceAdapter by lazy { DeviceAdapter(this) }

    private val viewModel: BluetoothViewModel by viewModels()

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            // All permissions granted
            setupBluetooth()
        } else {
            // At least one permission denied
            toast("Permission Denied")
        }
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            toast("Bluetooth Enabled")
        } else {
            toast("Something went wrong")
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (it.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state =
                            it.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        handleBluetoothState(state)
                    }

                    else -> handleBluetoothAction(it)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setup()
    }

    private fun setup() {

        requestPermission()

        setupClickable()

        setupRecyclerview()

        setupObserver()
    }

    private fun setupObserver() {
        collectLifecycleFlow(
            viewModel.deviceItems
        ) {
            deviceAdapter.setData(it)
        }
    }

    private fun setupRecyclerview() {
        binding.rcvDevice.apply {
            adapter = deviceAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupClickable() {
        binding.btnConnect.setOnClickListener {
            if (bluetoothAdapter?.isEnabled == true) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    bluetoothAdapter?.disable()
                }
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activityResultLauncher.launch(enableBtIntent)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onClickDevice(device: DeviceItem.Device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            requestPermission()
            return
        }
        bluetoothAdapter?.let {

            lifecycleScope.launch {
                setupConnection()
                connectDevice(device)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupConnection() {
        lifecycleScope.launch(Dispatchers.IO) {
            currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "server",
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
                currentClientSocket?.let {
                    currentServerSocket?.close()
                    shouldLoop = false
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectDevice(device: DeviceItem.Device) {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }
        currentClientSocket = device.bluetoothDevice
            .createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_UUID))
        currentClientSocket?.let { socket ->
            try {
                socket.connect()
            } catch (e: IOException) {
                socket.close()
                currentClientSocket = null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        bluetoothAdapter?.let { adapter ->
            viewModel.loadDevices(adapter)
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            adapter.startDiscovery()
        }
    }

    private fun requestPermission() {
        if (
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            setupBluetooth()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            } else {
                setupBluetooth()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupBluetooth() {
        // check state bluetooth in first time
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter != null) {
            val currentState = bluetoothAdapter!!.state
            handleBluetoothState(currentState)
        } else {
            toast("This device does not support Bluetooth")
        }

        // setup listen state change
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    @SuppressLint("MissingPermission")
    private fun handleBluetoothAction(intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_FOUND -> {
                // Found device
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                } else {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                device?.let { bltDevice ->
                    // Kiểm tra nếu thiết bị chưa nằm trong danh sách
                    val isPaired =
                        bluetoothAdapter?.bondedDevices?.any { it.address == bltDevice.address }
                    val alreadyAdded =
                        viewModel.deviceItems.value.any {
                            it is DeviceItem.Device &&
                                    it.bluetoothDevice.address == bltDevice.address
                        }

                    if (isPaired == false && !alreadyAdded) {
                        viewModel.addDevice(DeviceItem.Device(bltDevice))
                    }
                }
            }

            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                // Quá trình quét bắt đầu
                toast("Discovery started")
            }

            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                // Quá trình quét kết thúc
                toast("Discovery finished")
            }
        }
    }

    private fun handleBluetoothState(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_OFF -> {
                // Bluetooth đã tắt
                Log.d("BluetoothState", "STATE_OFF")
                binding.btnConnect.apply {
                    text = getString(R.string.enable_bluetooth)
                    setBackgroundColor(getColor(android.R.color.holo_green_dark))
                }
                viewModel.clearItems()
            }

            BluetoothAdapter.STATE_ON -> {
                // Bluetooth đã bật
                Log.d("BluetoothState", "STATE_ON")
                binding.btnConnect.apply {
                    text = getString(R.string.disable_bluetooth)
                    setBackgroundColor(getColor(android.R.color.holo_red_dark))
                }
                startDiscovery()
            }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)

    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }
}

fun <T> ComponentActivity.collectLifecycleFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    lifecycleScope.launch {
        flow.collect(collect)
    }
}