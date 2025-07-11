package com.example.certificate.presentation.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
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
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CertificateDetailAdapter
    private lateinit var getButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var editText: TextInputEditText
    private lateinit var editTextPort: TextInputEditText
    private lateinit var shareButton: MaterialButton


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews(view)
        setupRecyclerView()
        setupButtons()
        observeUiState()
    }

    private fun initViews(view: View){
        recyclerView = view.findViewById(R.id.certificateRecyclerView)
        getButton = view.findViewById(R.id.getButton)
        progressBar = view.findViewById(R.id.progressBar)
        editText = view.findViewById(R.id.editTextDomain)
        editTextPort = view.findViewById(R.id.editTextPort)
        shareButton = view.findViewById(R.id.shareButton)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CertificateDetailAdapter { sha256 ->
            copyToClipboard(sha256)
        }
        recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        getButton.setOnClickListener {
            val domain = editText.text?.toString()?.trim() ?: ""
            val portText = editTextPort.text?.toString()?.trim() ?: "443"
            val port = portText.toIntOrNull() ?: 443

            viewModel.fetchCertificate(domain, port)

        }

        shareButton.setOnClickListener {
            shareCertificate()
        }
    }


    private fun observeUiState() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CertificateUiState.Loading -> {
                        progressBar.isVisible = true
                        recyclerView.isVisible = false
                        getButton.isEnabled = false
                    }

                    is CertificateUiState.Success -> {
                        progressBar.isVisible = false
                        recyclerView.isVisible = true
                        getButton.isEnabled = true
                        getButton.text = getString(R.string.button_get)
                        adapter.updateData(state.data.certificates)
                    }

                    is CertificateUiState.Error -> {
                        progressBar.isVisible = false
                        recyclerView.isVisible = false
                        getButton.isEnabled = true
                        getButton.text = getString(R.string.button_retry)
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }

                    is CertificateUiState.Info -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }

                    is CertificateUiState.Idle -> {
                        progressBar.isVisible = false
                        recyclerView.isVisible = false
                        getButton.isEnabled = true
                        getButton.text = getString(R.string.button_get)
                    }
                }
            }
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

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.label_clipboard_title), text)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(requireContext(), getString(R.string.copied_toast, text), Toast.LENGTH_SHORT).show()
    }
}
