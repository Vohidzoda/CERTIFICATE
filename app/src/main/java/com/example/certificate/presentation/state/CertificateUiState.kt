package com.example.certificate.presentation.state

import com.example.domain.model.CertificateInfo

sealed class CertificateUiState {
    object Loading : CertificateUiState()
    data class Success(val data: CertificateInfo, val fingerprint: String?) : CertificateUiState()
    data class Error(val message: String) : CertificateUiState()
    object Idle : CertificateUiState()
}
