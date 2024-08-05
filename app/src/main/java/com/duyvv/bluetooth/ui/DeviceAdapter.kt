package com.duyvv.bluetooth.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.duyvv.bluetooth.databinding.ItemDeviceBinding
import com.duyvv.bluetooth.databinding.ItemHeaderBinding
import com.duyvv.bluetooth.domain.DeviceItem

class DeviceAdapter(
    private val deviceListener: DeviceListener
) : RecyclerView.Adapter<BaseViewHolder<out ViewBinding>>() {

    private val items = mutableListOf<DeviceItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<DeviceItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<out ViewBinding> {
        return when (viewType) {
            TYPE_HEADER -> {
                HeaderViewHolder(
                    ItemHeaderBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            TYPE_DEVICE -> {
                DeviceViewHolder(
                    ItemDeviceBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    deviceListener
                )
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<out ViewBinding>, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DeviceItem.Header -> TYPE_HEADER
            is DeviceItem.Device -> TYPE_DEVICE
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_DEVICE = 1
    }
}

interface DeviceListener {
    fun onClickDevice(device: DeviceItem.Device)
}

class DeviceViewHolder(
    binding: ItemDeviceBinding,
    private val deviceListener: DeviceListener
) : BaseViewHolder<ItemDeviceBinding>(binding) {
    @SuppressLint("MissingPermission")
    override fun bind(item: Any) {
        val device = item as DeviceItem.Device
        binding.deviceName.text = device.bluetoothDevice.name ?: "Unknown Device"
        binding.deviceAddress.text = device.bluetoothDevice.address
        binding.layoutItemDevice.setOnClickListener {
            deviceListener.onClickDevice(device)
        }
    }
}

class HeaderViewHolder(binding: ItemHeaderBinding) :
    BaseViewHolder<ItemHeaderBinding>(binding) {
    override fun bind(item: Any) {
        val header = item as DeviceItem.Header
        binding.headerTitle.text = header.title
    }
}

abstract class BaseViewHolder<B : ViewBinding>(val binding: B) :
    RecyclerView.ViewHolder(binding.root) {
    abstract fun bind(item: Any)
}
