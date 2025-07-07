package com.example.domain.model


data class SSLCertificateInfo(
    val domain: String,
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validTo: String,
    val sha256: List<String>
)
