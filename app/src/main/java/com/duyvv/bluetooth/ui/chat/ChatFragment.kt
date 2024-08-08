package com.duyvv.bluetooth.ui.chat

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Message
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.duyvv.bluetooth.R
import com.duyvv.bluetooth.base.BaseFragment
import com.duyvv.bluetooth.databinding.FragmentChatBinding
import com.duyvv.bluetooth.ui.BluetoothViewModel


class ChatFragment : BaseFragment<FragmentChatBinding>() {

    private val messageAdapter: MessageAdapter by lazy {
        MessageAdapter()
    }

    private lateinit var viewmodel: BluetoothViewModel

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentChatBinding.inflate(inflater, container, false)

    override fun init() {
        viewmodel = ViewModelProvider(requireActivity())[BluetoothViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setup()
    }

    @SuppressLint("SetTextI18n")
    private fun setup() {

        binding.rcvMessage.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.btnSend.setOnClickListener {
            val message = binding.edtInput.text.toString()
            if (message.isEmpty()) return@setOnClickListener
            viewmodel.sendMessage(message)
            binding.edtInput.setText("")
        }

        binding.btnDisconnect.setOnClickListener {
            viewmodel.disconnectFromDevice()
            findNavController().popBackStack()
        }

        collectLifecycleFlow(viewmodel.messages) {
            messageAdapter.setMessages(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewmodel.disconnectFromDevice()
    }
}