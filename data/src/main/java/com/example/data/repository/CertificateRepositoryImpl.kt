package com.example.data.repository

import com.example.domain.model.CertificateInfo
import com.example.domain.repository.ICertificateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class CertificateRepositoryImpl: ICertificateRepository {

    override suspend fun getCertificateInfo(domain: String): CertificateInfo {
        return withContext(Dispatchers.IO) {
            val clean = domain
                .replace("https://", "")
                .replace("http://", "")
                .trim()
                .split("/")[0]

            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val socket = factory.createSocket() as SSLSocket
            socket.connect(InetSocketAddress(clean, 443), 5000)
            socket.startHandshake()

            val cert = socket.session.peerCertificates.first() as X509Certificate
            socket.close()

            CertificateInfo(
                subject = cert.subjectDN.name,
                issuer = cert.issuerDN.name,
                validFrom = cert.notBefore.toString(),
                validTo = cert.notAfter.toString(),
                signatureAlgorithm = cert.sigAlgName,
                serialNumber = cert.serialNumber.toString(16),
                version = cert.version,
                certificate = cert

            )
        }
    }
}