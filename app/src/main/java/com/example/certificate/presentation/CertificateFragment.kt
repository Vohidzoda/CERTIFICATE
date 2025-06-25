package com.example.certificate.presentation

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.certificate.R
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val editText = view.findViewById<TextInputEditText>(R.id.editTextDomain)
        val getButton = view.findViewById<Button>(R.id.getButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val textView = view.findViewById<TextView>(R.id.jsonTextView)
        val shareButton = view.findViewById<MaterialButton>(R.id.shareButton)

        getButton.setOnClickListener {
            val input = editText.text?.toString() ?: ""
            viewModel.fetchCertificate(input)
        }

        shareButton.setOnClickListener {
            val state = viewModel.uiState.value
            if (state is CertificateUiState.Success) {
                val cert = state.data
                val shareText = getString(
                    R.string.share_certificate_text,
                    cert.subject,
                    cert.issuer,
                    cert.serialNumber,
                    cert.version,
                    cert.signatureAlgorithm,
                    viewModel.formatDate(cert.validFrom),
                    viewModel.formatDate(cert.validTo)
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share_certificate_title)))
            }
        }

        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.onOkButtonClicked()
                true
            } else false
        }

        lifecycleScope.launchWhenStarted {
            viewModel.hideKeyboardEvent.collect {
                hideKeyboard()
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is CertificateUiState.Loading -> {
                        progressBar.isVisible = true
                        textView.text = ""
                        getButton.text = getString(R.string.button_get)
                    }
                    is CertificateUiState.Success -> {
                        progressBar.isVisible = false
                        val cert = state.data
                        val fingerprint = state.fingerprint ?: "—"
                        val context = requireContext()
                        val redColor = ContextCompat.getColor(context, R.color.red)
                        val builder = SpannableStringBuilder()

                        fun appendLabelValue(label: String, value: String) {
                            val start = builder.length
                            builder.append(label)
                            builder.setSpan(
                                ForegroundColorSpan(redColor),
                                start,
                                start + label.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            builder.append(value)
                            builder.append("\n")
                        }

                        appendLabelValue(getString(R.string.cert_subject), cert.subject)
                        appendLabelValue(getString(R.string.cert_issuer), cert.issuer)
                        appendLabelValue(getString(R.string.cert_serial_number), cert.serialNumber)
                        appendLabelValue(getString(R.string.cert_version), cert.version.toString())
                        appendLabelValue(getString(R.string.cert_signature_algorithm), cert.signatureAlgorithm)
                        appendLabelValue(getString(R.string.cert_fingerprint), fingerprint)
                        appendLabelValue(getString(R.string.cert_validity_period), "")
                        appendLabelValue(getString(R.string.cert_valid_from), viewModel.formatDate(cert.validFrom))
                        appendLabelValue(getString(R.string.cert_valid_to), viewModel.formatDate(cert.validTo))

                        textView.text = builder
                        getButton.text = getString(R.string.button_get)
                    }
                    is CertificateUiState.Error -> {
                        progressBar.isVisible = false
                        textView.text = state.message
                        getButton.text = getString(R.string.button_retry)
                    }
                    is CertificateUiState.Idle -> {
                        progressBar.isVisible = false
                        textView.text = ""
                        getButton.text = getString(R.string.button_get)
                    }
                }
            }
        }

        observeNetwork(getButton)
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