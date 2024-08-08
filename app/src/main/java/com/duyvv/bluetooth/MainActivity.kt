package com.duyvv.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.duyvv.bluetooth.data.hasBluetoothConnectPermission
import com.duyvv.bluetooth.data.hasBluetoothScanPermission
import com.duyvv.bluetooth.data.hasPermission
import com.duyvv.bluetooth.data.isSDKVersionFromS
import com.duyvv.bluetooth.databinding.ActivityMainBinding
import com.duyvv.bluetooth.domain.DeviceItem
import com.duyvv.bluetooth.ui.BluetoothViewModel
import com.duyvv.bluetooth.ui.bluetooth.DeviceAdapter
import com.duyvv.bluetooth.ui.bluetooth.DeviceListener
import com.duyvv.bluetooth.ui.bluetooth.ProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}