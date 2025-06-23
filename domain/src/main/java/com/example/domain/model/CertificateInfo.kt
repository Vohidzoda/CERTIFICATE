package com.example.domain.model

import java.security.cert.X509Certificate

data class CertificateInfo(
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validTo: String,
    val certificate: X509Certificate,
    val signatureAlgorithm: String,
    val serialNumber: String,
    val version: Int
)
