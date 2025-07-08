package com.example.certificate.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.certificate.R
import com.example.certificate.presentation.state.CertificateUiState
import com.example.certificate.presentation.util.CertificateUtils
import com.example.domain.repository.NetworkChecker
import com.example.domain.repository.ResourceProvider
import com.example.domain.usecase.GetSSLCertificateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
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
        val domain = CertificateUtils.normalizeDomain(input)

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

                val formattedCert = cert.copy(
                    certificates = cert.certificates.map { entry ->
                        entry.copy(
                            validFrom = CertificateUtils.formatDate(entry.validFrom),
                            validTo = CertificateUtils.formatDate(entry.validTo)
                        )
                    }
                )

                _uiState.value = CertificateUiState.Success(formattedCert)
            } catch (e: Exception) {
                val msg = e.message?.ifBlank { null }
                _uiState.value = CertificateUiState.Error(
                    resourceProvider.getString(
                        R.string.error_invalid_domain,
                        msg ?: resourceProvider.getString(R.string.error_unknown)
                    )
                )
            }
        }
    }

    fun onOkButtonClicked() {
        viewModelScope.launch {
            _hideKeyboardEvent.emit(Unit)
        }
    }
}
