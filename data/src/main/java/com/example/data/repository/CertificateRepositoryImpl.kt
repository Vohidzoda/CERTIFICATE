package com.example.data.repository

import android.util.Base64
import com.example.domain.model.SSLCertificateInfo
import com.example.domain.repository.ICertificateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.URL
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class CertificateRepositoryImpl : ICertificateRepository {

    override suspend fun getSSLCertificate(domain: String): SSLCertificateInfo {
        return withContext(Dispatchers.IO) {
            val clean = domain
                .replace("https://", "")
                .replace("http://", "")
                .trim()
                .split("/")[0]

            val (host, port) = parseHostAndPort(clean)

            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val socket = factory.createSocket() as SSLSocket
            socket.connect(InetSocketAddress(host, port), 5000)
            socket.startHandshake()

            val certs = socket.session.peerCertificates
                .filterIsInstance<X509Certificate>()

            val pins = certs.map { getCertificatePin(it) }

            val firstCert = certs.first()
            socket.close()

            SSLCertificateInfo(
                domain = host,
                subject = firstCert.subjectDN.name,
                issuer = firstCert.issuerDN.name,
                validFrom = firstCert.notBefore.toString(),
                validTo = firstCert.notAfter.toString(),
                sha256 = pins
            )
        }
    }


    private fun parseHostAndPort(input: String): Pair<String, Int> {
        val parts = input.split(":")
        return if (parts.size == 2) {
            val host = parts[0]
            val port = parts[1].toIntOrNull() ?: 443
            host to port
        } else {
            input to 443
        }
    }


    private fun getCertificatePin(cert: X509Certificate): String {
        val md = MessageDigest.getInstance("SHA-256")
        val publicKey = cert.publicKey.encoded
        val sha256 = md.digest(publicKey)
        return Base64.encodeToString(sha256, Base64.NO_WRAP)
    }

    private fun normalizeDomain(input: String): String {
        return input
            .replace("https://", "", ignoreCase = true)
            .replace("http://", "", ignoreCase = true)
            .trim()
            .split("/")[0]
    }
}
