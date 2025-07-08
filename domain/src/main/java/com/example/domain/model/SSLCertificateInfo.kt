package com.example.domain.model


data class SSLCertificateInfo(
    val domain: String,
    val certificates: List<SSLCertificateEntry>
)
