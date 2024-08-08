package com.duyvv.bluetooth.ui.chat

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.viewbinding.ViewBinding
import com.duyvv.bluetooth.databinding.ItemLocalMessageBinding
import com.duyvv.bluetooth.databinding.ItemReceivedMessageBinding
import com.duyvv.bluetooth.domain.BluetoothMessage

class MessageAdapter : Adapter<MessageViewHolder<out ViewBinding>>() {

    private val messages = mutableListOf<BluetoothMessage>()

    @SuppressLint("NotifyDataSetChanged")
    fun setMessages(list: List<BluetoothMessage>) {
        messages.clear()
        messages.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MessageViewHolder<out ViewBinding> {
        when (viewType) {
            TYPE_LOCAL_MESSAGE -> {
                return LocalMessageViewHolder(
                    ItemLocalMessageBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            TYPE_RECEIVED_MESSAGE -> {
                return ReceivedMessageViewHolder(
                    ItemReceivedMessageBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder<out ViewBinding>, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isFromLocalUser)
            TYPE_LOCAL_MESSAGE
        else
            TYPE_RECEIVED_MESSAGE
    }

    companion object {
        const val TYPE_LOCAL_MESSAGE = 0
        const val TYPE_RECEIVED_MESSAGE = 1
    }

    inner class LocalMessageViewHolder(binding: ItemLocalMessageBinding) :
        MessageViewHolder<ItemLocalMessageBinding>(binding) {
        override fun bind(item: Any) {
            binding.tvContent.text = (item as BluetoothMessage).message
        }
    }

    inner class ReceivedMessageViewHolder(binding: ItemReceivedMessageBinding) :
        MessageViewHolder<ItemReceivedMessageBinding>(binding) {
        override fun bind(item: Any) {
            binding.tvContent.text = (item as BluetoothMessage).message
            binding.tvSenderName.text = "${item.senderName}:"
        }
    }
}

abstract class MessageViewHolder<B : ViewBinding>(val binding: B) :
    RecyclerView.ViewHolder(binding.root) {
    abstract fun bind(item: Any)
}