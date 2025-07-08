package com.example.data.repository

import android.util.Base64
import com.example.domain.model.SSLCertificateEntry
import com.example.domain.model.SSLCertificateInfo
import com.example.domain.repository.ICertificateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class CertificateRepositoryImpl : ICertificateRepository {

    override suspend fun getSSLCertificate(domain: String, port: Int): SSLCertificateInfo {
        return withContext(Dispatchers.IO) {
            val cleanHost = domain
                .replace("https://", "")
                .replace("http://", "")
                .trim()
                .split("/")[0]

            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val socket = factory.createSocket() as SSLSocket
            socket.connect(InetSocketAddress(cleanHost, port), 5000)
            socket.startHandshake()

            val certs = socket.session.peerCertificates
                .filterIsInstance<X509Certificate>()

            val entries = certs.map { cert ->
                SSLCertificateEntry(
                    subject = cert.subjectDN.name,
                    issuer = cert.issuerDN.name,
                    validFrom = cert.notBefore.toString(),
                    validTo = cert.notAfter.toString(),
                    sha256 = getCertificatePin(cert)
                )
            }

            socket.close()

            SSLCertificateInfo(
                domain = cleanHost,
                certificates = entries
            )
        }
    }



//    private fun parseHostAndPort(input: String): Pair<String, Int> {
//        val parts = input.split(":")
//        return if (parts.size == 2) {
//            val host = parts[0]
//            val port = parts[1].toIntOrNull() ?: 443
//            host to port
//        } else {
//            input to 443
//        }
//    }


    private fun getCertificatePin(cert: X509Certificate): String {
        val md = MessageDigest.getInstance("SHA-256")
        val publicKey = cert.publicKey.encoded
        val sha256 = md.digest(publicKey)
        return Base64.encodeToString(sha256, Base64.NO_WRAP)
    }

}
