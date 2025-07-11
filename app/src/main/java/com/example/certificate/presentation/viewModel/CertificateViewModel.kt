package com.example.certificate.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.certificate.R
import com.example.certificate.presentation.state.CertificateUiState
import com.example.certificate.presentation.util.CertificateUtils
import com.example.domain.model.SSLCertificateInfo
import com.example.domain.network.NetworkObserver
import com.example.domain.repository.ResourceProvider
import com.example.domain.usecase.GetSSLCertificateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okio.IOException
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class CertificateViewModel @Inject constructor(
    private val getSSLCertificateUseCase: GetSSLCertificateUseCase,
    private val resourceProvider: ResourceProvider,
    private val networkObserver: NetworkObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow<CertificateUiState>(CertificateUiState.Idle)
    val uiState: StateFlow<CertificateUiState> = _uiState
    private var hasInternet = true
    private var isFirstCheck = true
    private var lastConnectionState: Boolean? = null


    init {
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            networkObserver.isNetworkAvailable.collect { isConnected ->
                hasInternet = isConnected

                if (isFirstCheck) {
                    isFirstCheck = false
                    lastConnectionState = isConnected
                    if (!isConnected) {
                        postError(R.string.error_no_internet)
                    }
                } else {
                    val wasConnected = lastConnectionState
                    lastConnectionState = isConnected

                    if (!isConnected) {
                        postError(R.string.error_no_internet)
                    } else if (wasConnected == false && isConnected) {
                        postInfo(R.string.info_internet_restored)
                    }
                }
            }
        }
    }


    fun fetchCertificate(input: String, port: Int) {
        if (!networkObserver.isCurrentlyAvailable()) {
            postError(R.string.error_no_internet)
            return
        }

        val domain = validateAndNormalizeDomain(input) ?: return
        loadCertificate(domain, port)
    }


    private fun validateAndNormalizeDomain(input: String): String? {
        val domain = CertificateUtils.normalizeDomain(input)
        if (domain.isEmpty()) {
            postError(R.string.error_invalid_domain)
            return null
        }
        return domain
    }

    private fun loadCertificate(domain: String, port: Int) {
        viewModelScope.launch {
            _uiState.value = CertificateUiState.Loading
            try {
                val cert = getSSLCertificateUseCase(domain, port)
                val formattedCert = formatCertificateDates(cert)
                _uiState.value = CertificateUiState.Success(formattedCert)
            } catch (e: Throwable) {
                handleError(e)
            }
        }
    }

    private fun formatCertificateDates(cert: SSLCertificateInfo): SSLCertificateInfo {
        return cert.copy(
            certificates = cert.certificates.map { entry ->
                entry.copy(
                    validFrom = CertificateUtils.formatDate(entry.validFrom),
                    validTo = CertificateUtils.formatDate(entry.validTo)
                )
            }
        )
    }

    private fun handleError(e: Throwable) {
        val errorMessageRes = when (e) {
            is java.net.UnknownHostException -> R.string.error_invalid_domain
            is java.net.ConnectException ,
            is java.net.SocketTimeoutException -> {
                if (!hasInternet) {
                    R.string.error_no_internet
                } else {
                    R.string.error_connection_failed
                }
            }
            is IOException -> R.string.error_unknown
            is HttpException -> when (e.code()) {
                400 -> R.string.error_domain_not_found
                500 -> R.string.error_server_error
                else -> R.string.error_unknown
            }
            is TimeoutCancellationException -> R.string.error_timeout
            is IllegalArgumentException -> R.string.error_invalid_domain
            else -> R.string.error_unknown
        }
        postError(errorMessageRes)
    }

    private fun postInfo(messageRes: Int) {
        _uiState.value = CertificateUiState.Info(resourceProvider.getString(messageRes))
    }

    private fun postError(messageRes: Int) {
        _uiState.value = CertificateUiState.Error(resourceProvider.getString(messageRes))
    }
}
