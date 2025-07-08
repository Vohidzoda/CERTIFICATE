package com.example.certificate.presentation.util

import android.content.Context
import android.content.Intent
import com.example.certificate.R
import com.example.domain.model.SSLCertificateEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ShareHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun shareCertificates(certificates: List<SSLCertificateEntry>) {
        val text = buildString {
            certificates.forEachIndexed { i, cert ->
                appendLine("Сертификат ${i + 1}")
                appendLine("Субъект: ${cert.subject}")
                appendLine("Издатель: ${cert.issuer}")
                appendLine("SHA-256: ${cert.sha256}")
                appendLine("С: ${cert.validFrom}")
                appendLine("До: ${cert.validTo}")
                appendLine()
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.share_certificate_title))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
