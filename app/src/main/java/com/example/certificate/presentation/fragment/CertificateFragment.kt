package com.example.certificate.presentation.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.certificate.R
import com.example.certificate.presentation.adapter.CertificateDetailAdapter
import com.example.certificate.presentation.state.CertificateUiState
import com.example.certificate.presentation.util.ShareHelper
import com.example.certificate.presentation.viewModel.CertificateViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class CertificateFragment : Fragment(R.layout.fragment_certificate) {

    @Inject
    lateinit var shareHelper: ShareHelper
    private val viewModel: CertificateViewModel by viewModels()
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CertificateDetailAdapter
    private lateinit var getButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var editText: TextInputEditText
    private lateinit var shareButton: MaterialButton

    private var isNetworkAvailable = true
        set(value) {
            field = value
            requireActivity().runOnUiThread {
                getButton.text = if (value) getString(R.string.button_get) else getString(R.string.button_retry)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.certificateRecyclerView)
        getButton = view.findViewById(R.id.getButton)
        progressBar = view.findViewById(R.id.progressBar)
        editText = view.findViewById(R.id.editTextDomain)
        shareButton = view.findViewById(R.id.shareButton)

        setupRecyclerView()
        setupButtons()
        observeUiState()
        observeKeyboard()
        observeNetwork()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CertificateDetailAdapter(emptyList()) { sha256 ->
            copyToClipboard(sha256)
        }
        recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        getButton.setOnClickListener {
            val input = editText.text?.toString() ?: ""

            if (isNetworkAvailable) {
                viewModel.fetchCertificate(input)
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_no_internet), Toast.LENGTH_SHORT).show()
            }
        }

        shareButton.setOnClickListener {
            shareCertificate()
        }

        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.onOkButtonClicked()
                hideKeyboard()
                true
            } else false
        }
    }

    private fun observeUiState() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CertificateUiState.Loading -> {
                        progressBar.isVisible = true
                        recyclerView.isVisible = false
                    }
                    is CertificateUiState.Success -> {
                        progressBar.isVisible = false
                        recyclerView.isVisible = true
                        adapter.updateData(state.data.certificates)
                    }
                    is CertificateUiState.Error -> {
                        progressBar.isVisible = false
                        recyclerView.isVisible = false
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    is CertificateUiState.Idle -> {
                        progressBar.isVisible = false
                        recyclerView.isVisible = false
                    }
                }
            }
        }
    }

    private fun observeKeyboard() {
        lifecycleScope.launchWhenStarted {
            viewModel.hideKeyboardEvent.collect {
                hideKeyboard()
            }
        }
    }

    private fun observeNetwork() {
        connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                isNetworkAvailable = false
            }

            override fun onAvailable(network: Network) {
                isNetworkAvailable = true
            }
        }

        isNetworkAvailable = connectivityManager.activeNetwork != null

        val request = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::connectivityManager.isInitialized && ::networkCallback.isInitialized) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    private fun shareCertificate() {
        val state = viewModel.uiState.value
        if (state is CertificateUiState.Success) {
            shareHelper.shareCertificates(state.data.certificates)
        } else {
            Toast.makeText(requireContext(), getString(R.string.no_data_to_share), Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        val view = requireActivity().currentFocus ?: View(requireContext())
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.label_clipboard_title), text)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(requireContext(), getString(R.string.copied_toast, text), Toast.LENGTH_SHORT).show()
    }
}
