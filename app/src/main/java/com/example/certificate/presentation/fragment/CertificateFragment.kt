package com.example.certificate.presentation.fragment

import android.content.Context
import android.content.Intent
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
import com.example.certificate.presentation.viewModel.CertificateViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class CertificateFragment : Fragment(R.layout.fragment_certificate) {

    private val viewModel: CertificateViewModel by viewModels()

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CertificateDetailAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.certificateRecyclerView)
        setupRecyclerView()

        setupButtons(view)

        observeUiState(view)
        observeKeyboardAndNetwork(view)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CertificateDetailAdapter(emptyList())
        recyclerView.adapter = adapter
    }

    private fun setupButtons(view: View) {
        val editText = view.findViewById<TextInputEditText>(R.id.editTextDomain)
        val getButton = view.findViewById<Button>(R.id.getButton)
        val shareButton = view.findViewById<MaterialButton>(R.id.shareButton)

        getButton.setOnClickListener {
            val input = editText.text?.toString() ?: ""
            viewModel.fetchCertificate(input)
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

    private fun observeUiState(view: View) {
        val getButton = view.findViewById<Button>(R.id.getButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CertificateUiState.Loading -> {
                        progressBar.isVisible = true
                        recyclerView.isVisible = false
                        getButton.text = getString(R.string.button_get)
                    }
                    is CertificateUiState.Success -> {
                        progressBar.isVisible = false
                        recyclerView.isVisible = true

                        val cert = state.data

                        val displayList = listOf(
                            getString(R.string.cert_subject) to cert.subject,
                            getString(R.string.cert_issuer) to cert.issuer,
                            getString(R.string.cert_valid_from) to viewModel.formatDate(cert.validFrom),
                            getString(R.string.cert_valid_to) to viewModel.formatDate(cert.validTo),
                            getString(R.string.cert_sha256_pin) to cert.sha256Pin
                        )

                        adapter.updateData(displayList)
                        getButton.text = getString(R.string.button_get)
                    }
                    is CertificateUiState.Error -> {
                        progressBar.isVisible = false
                        recyclerView.isVisible = false
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        getButton.text = getString(R.string.button_retry)
                    }
                    is CertificateUiState.Idle -> {
                        progressBar.isVisible = false
                        recyclerView.isVisible = false
                        getButton.text = getString(R.string.button_get)
                    }
                }
            }
        }
    }


    private fun observeKeyboardAndNetwork(view: View) {
        val getButton = view.findViewById<Button>(R.id.getButton)

        lifecycleScope.launchWhenStarted {
            viewModel.hideKeyboardEvent.collect {
                hideKeyboard()
            }
        }

        observeNetwork(getButton)
    }

    private fun shareCertificate() {
        val state = viewModel.uiState.value
        if (state is CertificateUiState.Success) {
            val cert = state.data
            val shareText = getString(
                R.string.share_certificate_text,
                cert.subject,
                cert.issuer,
                viewModel.formatDate(cert.validFrom),
                viewModel.formatDate(cert.validTo),
                cert.sha256Pin
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_certificate_title)))
        } else {
            Toast.makeText(requireContext(), "Нет данных для шаринга", Toast.LENGTH_SHORT).show()
        }
    }


    private fun observeNetwork(getButton: Button) {
        connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                requireActivity().runOnUiThread {
                    getButton.text = getString(R.string.button_retry)
                }
            }
        }

        val request = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::connectivityManager.isInitialized && ::networkCallback.isInitialized) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        val view = requireActivity().currentFocus ?: View(requireContext())
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

