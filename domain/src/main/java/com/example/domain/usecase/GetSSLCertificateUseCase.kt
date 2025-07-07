package com.example.domain.usecase

import com.example.domain.model.SSLCertificateInfo
import com.example.domain.repository.ICertificateRepository

class GetSSLCertificateUseCase(private val repository: ICertificateRepository) {
    suspend operator fun invoke(domain: String): SSLCertificateInfo {
        return repository.getSSLCertificate(domain)
    }
}
