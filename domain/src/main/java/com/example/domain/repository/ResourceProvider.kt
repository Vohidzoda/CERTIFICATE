package com.example.domain.repository

import androidx.annotation.StringRes

interface ResourceProvider {

    fun getString(@StringRes id : Int): String
    fun getString(@StringRes id : Int, vararg args : Any): String
}