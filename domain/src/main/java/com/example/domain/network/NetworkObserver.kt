package com.example.domain.network

import kotlinx.coroutines.flow.Flow


interface NetworkObserver {
    val isNetworkAvailable: Flow<Boolean>
    fun isCurrentlyAvailable(): Boolean

}
