package com.example.certificate.presentation.util

import java.text.SimpleDateFormat
import java.util.Locale

object CertificateUtils {

    fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            outputFormat.format(inputFormat.parse(dateString) ?: return dateString)
        } catch (e: Exception) {
            dateString
        }
    }

    fun normalizeDomain(input: String): String {
        val clean = input
            .replace("https://", "", ignoreCase = true)
            .replace("http://", "", ignoreCase = true)
            .trim()
            .split("/")[0]

        val host = clean.split(":")[0]

        val regex = Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        return if (regex.matches(host)) clean else ""
    }
}
