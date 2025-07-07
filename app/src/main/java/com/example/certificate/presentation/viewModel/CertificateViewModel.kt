package com.example.certificate.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.certificate.R
import com.example.certificate.presentation.state.CertificateUiState
import com.example.domain.repository.NetworkChecker
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
    private val resourceProvider: ResourceProvider,
    private val networkChecker: NetworkChecker
) : ViewModel() {

    private val _hideKeyboardEvent = MutableSharedFlow<Unit>()
    val hideKeyboardEvent = _hideKeyboardEvent.asSharedFlow()

    private val _uiState = MutableStateFlow<CertificateUiState>(CertificateUiState.Idle)
    val uiState: StateFlow<CertificateUiState> = _uiState

    fun fetchCertificate(input: String) {
        val domain = normalizeDomain(input)

        if (domain.isEmpty()) {
            _uiState.value = CertificateUiState.Error(
                resourceProvider.getString(R.string.error_invalid_domain))
            return
        }

        if (!networkChecker.isConnected()) {
            _uiState.value = CertificateUiState.Error(
                resourceProvider.getString(R.string.error_no_internet))
            return
        }

        viewModelScope.launch {
            _uiState.value = CertificateUiState.Loading
            try {
                val cert = getSSLCertificateUseCase(domain)
                _uiState.value = CertificateUiState.Success(cert)
            } catch (e: Exception) {
                val msg = e.message?.ifBlank { null }

                _uiState.value = CertificateUiState.Error(
                    resourceProvider.getString(
                        R.string.error_with_message, msg ?: resourceProvider.getString(
                            R.string.error_unknown))
                )
            }
        }
    }

    fun onOkButtonClicked() {
        viewModelScope.launch {
            _hideKeyboardEvent.emit(Unit)
        }
    }

    fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru"))
            outputFormat.format(inputFormat.parse(dateString) ?: return dateString)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun normalizeDomain(input: String): String {
        val clean = input
            .replace("https://", "", ignoreCase = true)
            .replace("http://", "", ignoreCase = true)
            .trim()
            .split("/")[0]

        val host = clean.split(":")[0]

        val regex = Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        return if (regex.matches(host)) clean else ""
    }

}
