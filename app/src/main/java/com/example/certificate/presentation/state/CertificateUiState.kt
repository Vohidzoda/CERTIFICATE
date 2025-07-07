package com.example.certificate.presentation.state

import com.example.domain.model.SSLCertificateInfo

sealed class CertificateUiState {
    object Idle : CertificateUiState()
    object Loading : CertificateUiState()
    data class Success(val data: SSLCertificateInfo) : CertificateUiState()
    data class Error(val message: String) : CertificateUiState()
}
