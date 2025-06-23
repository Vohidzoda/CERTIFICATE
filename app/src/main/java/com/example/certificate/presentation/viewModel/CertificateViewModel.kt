package com.example.certificate.presentation.viewModel

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.certificate.R
import com.example.domain.model.CertificateInfo
import com.example.domain.repository.ResourceProvider
import com.example.domain.usecase.GetSSLCertificateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CertificateViewModel @Inject constructor(
    private val getSSLCertificateUseCase: GetSSLCertificateUseCase,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _hideKeyboardEvent = MutableSharedFlow<Unit>()
    val hideKeyboardEvent = _hideKeyboardEvent.asSharedFlow()

    private val _certificateInfo = MutableStateFlow<CertificateInfo?>(null)
    val certificateInfo: StateFlow<CertificateInfo?> = _certificateInfo

    private val _signatureFingerprint = MutableStateFlow<String?>(null)
    val signatureFingerprint: StateFlow<String?> = _signatureFingerprint

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var lastDomainInput: String? = null

    fun fetchCertificate(input: String, context: Context) {
        val domain = normalizeDomain(input)
        lastDomainInput = input

        if (domain.isEmpty()) {
            _error.value = resourceProvider.getString(R.string.error_invalid_domain)
            return
        }

        if (!isInternetAvailable(context)) {
            _error.value = resourceProvider.getString(R.string.error_no_internet)
            return
        }

        viewModelScope.launch {
            _loading.value = true
            try {
                val info = getSSLCertificateUseCase(domain)
                _certificateInfo.value = info
                _error.value = null

                _signatureFingerprint.value = getSignatureSha256Fingerprint(info.certificate)

            } catch (e: Exception) {
                val errorMsg = e.message?.ifBlank { null }
                _error.value = resourceProvider.getString(
                    R.string.error_with_message,
                    errorMsg ?: resourceProvider.getString(R.string.error_unknown)
                )
            } finally {
                _loading.value = false
            }
        }
    }

    fun retryLastRequest(context: Context) {
        lastDomainInput?.let {
            fetchCertificate(it, context)
        }
    }

    private fun normalizeDomain(input: String): String {
        val clean = input
            .replace("https://", "", ignoreCase = true)
            .replace("http://", "", ignoreCase = true)
            .trim()
            .split("/")[0]

        val regex = Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        return if (regex.matches(clean)) clean else ""
    }

    fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru"))
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: return dateString)
        } catch (e: Exception) {
            dateString
        }
    }

    fun getSignatureSha256Fingerprint(certificate: X509Certificate): String {
        return try {
            val signatureBytes = certificate.signature
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(signatureBytes)
            hashBytes.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            resourceProvider.getString(R.string.error_fingerprint)
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun onOkButtonClicked() {
        viewModelScope.launch {
            _hideKeyboardEvent.emit(Unit)
        }
    }
}
