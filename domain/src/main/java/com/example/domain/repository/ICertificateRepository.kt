package com.example.domain.repository

import com.example.domain.model.SSLCertificateInfo

interface ICertificateRepository {
    suspend fun getSSLCertificate(domain: String, port: Int): SSLCertificateInfo
}
