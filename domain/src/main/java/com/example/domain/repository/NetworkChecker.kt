package com.example.domain.repository

interface NetworkChecker {
    fun isConnected(): Boolean
}