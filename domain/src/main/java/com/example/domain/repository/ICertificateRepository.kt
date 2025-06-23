package com.example.domain.repository

import com.example.domain.model.CertificateInfo

interface ICertificateRepository {

    suspend fun getCertificateInfo(domain: String): CertificateInfo
}