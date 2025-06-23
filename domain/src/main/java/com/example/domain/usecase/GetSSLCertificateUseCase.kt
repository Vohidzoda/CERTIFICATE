package com.example.domain.usecase

import com.example.domain.model.CertificateInfo
import com.example.domain.repository.ICertificateRepository

class GetSSLCertificateUseCase(
    private val repository: ICertificateRepository
) {

    suspend operator fun invoke (domain: String): CertificateInfo {
        return  repository.getCertificateInfo(domain)
    }
}