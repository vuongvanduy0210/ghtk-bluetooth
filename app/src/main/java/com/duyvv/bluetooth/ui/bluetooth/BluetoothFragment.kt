package com.duyvv.bluetooth.ui.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.duyvv.bluetooth.R
import com.duyvv.bluetooth.base.BaseFragment
import com.duyvv.bluetooth.data.hasBluetoothConnectPermission
import com.duyvv.bluetooth.data.hasBluetoothScanPermission
import com.duyvv.bluetooth.data.hasPermission
import com.duyvv.bluetooth.data.isSDKVersionFromS
import com.duyvv.bluetooth.databinding.FragmentBluetoothBinding
import com.duyvv.bluetooth.domain.DeviceItem
import com.duyvv.bluetooth.ui.BluetoothViewModel

class BluetoothFragment : BaseFragment<FragmentBluetoothBinding>() {

    private val bluetoothAdapter by lazy {
        requireContext().getSystemService(BluetoothManager::class.java)?.adapter
    }

    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    private val progressDialog by lazy { ProgressDialog(requireContext()) }

    private val deviceAdapter by lazy {
        DeviceAdapter(object : DeviceListener {
            override fun onClickDevice(device: DeviceItem.Device) {
                viewModel.connectDevice(device.bluetoothDevice)
            }
        })
    }

    private lateinit var viewModel: BluetoothViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.isNotEmpty() && result.values.all { it }) {
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

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentBluetoothBinding.inflate(inflater, container, false)

    override fun init() {
        viewModel = ViewModelProvider(requireActivity())[BluetoothViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setup()
    }

    private fun setup() {

        requestPermission()

        setupRecyclerview()

        setupObserver()
    }

    private fun setupObserver() {
        collectLifecycleFlow(
            viewModel.bluetoothDevices
        ) {
            deviceAdapter.setData(it)
        }
        collectLifecycleFlow(viewModel.bluetoothState) {
            handleBluetoothState(it)
        }
        collectLifecycleFlow(viewModel.isConnected) {
            if (it) {
                toastLong("Your device is connected!")
                navigate(
                    BluetoothFragmentDirections.actionBluetoothFragmentToChatFragment()
                )
            }
        }
        collectLifecycleFlow(viewModel.isConnecting) {
            if (it) {
                if (progressDialog.isShowing) progressDialog.dismiss()
                progressDialog.show()
            } else {
                progressDialog.dismiss()
            }
        }
        collectLifecycleFlow(viewModel.error) { error ->
            error?.let { toast(it) }
        }
    }

    private fun setupRecyclerview() {
        binding.rcvDevice.apply {
            adapter = deviceAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupClickable() {
        binding.btnControlBluetooth.setOnClickListener {
            if (isBluetoothEnabled) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    bluetoothAdapter?.disable()
                }
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activityResultLauncher.launch(enableBtIntent)
            }
        }

        binding.btnStartServer.setOnClickListener {
            viewModel.waitForIncomingConnections()
        }
    }

    private fun handleBluetoothState(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_OFF -> {
                // Bluetooth đã tắt
                Log.d("BluetoothState", "STATE_OFF")
                binding.btnControlBluetooth.apply {
                    text = getString(R.string.enable_bluetooth)
                    setBackgroundColor(
                        requireContext().getColor(android.R.color.holo_green_dark)
                    )
                }
                viewModel.clearItems()
            }

            BluetoothAdapter.STATE_ON -> {
                // Bluetooth đã bật
                Log.d("BluetoothState", "STATE_ON")
                binding.btnControlBluetooth.apply {
                    text = getString(R.string.disable_bluetooth)
                    setBackgroundColor(
                        requireContext().getColor(android.R.color.holo_red_dark)
                    )
                }
                viewModel.startDiscovery()
            }
        }
    }

    private fun setupBluetooth() {
        if (bluetoothAdapter != null) {
            val currentState = bluetoothAdapter!!.state
            handleBluetoothState(currentState)
        } else {
            toast("This device does not support Bluetooth")
        }

        setupClickable()
    }

    private fun requestPermission() {
        if (
            hasBluetoothConnectPermission(requireContext()) &&
            hasBluetoothScanPermission(requireContext()) &&
            hasPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            setupBluetooth()
        } else {
            if (isSDKVersionFromS) {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            } else {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun toastLong(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}