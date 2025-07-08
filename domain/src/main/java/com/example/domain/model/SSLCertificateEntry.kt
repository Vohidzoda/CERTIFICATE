package com.example.domain.model

data class SSLCertificateEntry(
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validTo: String,
    val sha256: String
)
