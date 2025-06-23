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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.certificate.R
import com.example.certificate.presentation.viewModel.CertificateViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

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

            if (getButton.text == getString(R.string.button_retry)) {
                if (isInternetAvailable()) {
                    viewModel.retryLastRequest(requireContext())
                    getButton.text = getString(R.string.button_get)
                } else {
                    Toast.makeText(requireContext(),
                        getString(R.string.error_no_internet),
                        Toast.LENGTH_SHORT).show()
                }
            } else {
                viewModel.fetchCertificate(input, requireContext())
            }
        }

        shareButton.setOnClickListener {
            viewModel.certificateInfo.value?.let { cert ->
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

        lifecycleScope.launchWhenStarted {
            viewModel.hideKeyboardEvent.collect {
                hideKeyboard()
            }
        }

        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.onOkButtonClicked()
                true
            } else false
        }

        lifecycleScope.launchWhenStarted {
            viewModel.certificateInfo.combine(viewModel.signatureFingerprint) { cert, fingerprint ->
                Pair(cert, fingerprint)
            }.collectLatest { (cert, fingerprint) ->
                if (cert != null) {
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

                    appendLabelValue(resources.getString(R.string.cert_subject), cert.subject)
                    appendLabelValue(resources.getString(R.string.cert_issuer), cert.issuer)
                    appendLabelValue(resources.getString(R.string.cert_serial_number), cert.serialNumber)
                    appendLabelValue(resources.getString(R.string.cert_version), cert.version.toString())
                    appendLabelValue(resources.getString(R.string.cert_signature_algorithm), cert.signatureAlgorithm)
                    appendLabelValue(resources.getString(R.string.cert_fingerprint), fingerprint ?: "—")
                    appendLabelValue(resources.getString(R.string.cert_validity_period), "")
                    appendLabelValue(resources.getString(R.string.cert_valid_from), viewModel.formatDate(cert.validFrom))
                    appendLabelValue(resources.getString(R.string.cert_valid_to), viewModel.formatDate(cert.validTo))

                    textView.text = builder
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { error ->
                if (error != null) {
                    textView.text = error
                    getButton.text = getString(R.string.button_retry)
                } else {
                    getButton.text = getString(R.string.button_get)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.loading.collectLatest { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        observeNetwork(getButton)
    }

    private fun observeNetwork(getButton: Button) {
        connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
            }

            override fun onLost(network: Network) {
                requireActivity().runOnUiThread {
                    getButton.text = getString(R.string.button_retry)
                }
            }
        }

        val request = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    private fun isInternetAvailable(): Boolean {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::connectivityManager.isInitialized && ::networkCallback.isInitialized) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus ?: View(requireContext())
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
