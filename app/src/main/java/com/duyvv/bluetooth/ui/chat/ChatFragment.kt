package com.duyvv.bluetooth.ui.chat

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.duyvv.bluetooth.MainActivity
import com.duyvv.bluetooth.R
import com.duyvv.bluetooth.base.BaseFragment
import com.duyvv.bluetooth.databinding.FragmentChatBinding
import com.duyvv.bluetooth.ui.BluetoothViewModel


class ChatFragment : BaseFragment<FragmentChatBinding>() {

    private val messageAdapter: MessageAdapter by lazy {
        MessageAdapter()
    }

    private val activity: MainActivity by lazy {
        requireActivity() as MainActivity
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
            activity.hideKeyboard()
            val navController = Navigation.findNavController(
                requireActivity(),
                R.id.nav_host_fragment
            )
            activity.finish()
        }

        collectLifecycleFlow(viewmodel.messages) {
            messageAdapter.setMessages(it)
            val lastPosition = if (it.size - 1 < 0) 0 else it.size - 1
            binding.rcvMessage.smoothScrollToPosition(lastPosition)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewmodel.disconnectFromDevice()
    }
}