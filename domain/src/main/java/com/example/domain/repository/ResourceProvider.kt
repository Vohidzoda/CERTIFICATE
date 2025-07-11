package com.example.domain.repository

import androidx.annotation.StringRes

interface ResourceProvider {

    fun getString(id : Int): String
    fun getString(id : Int, vararg args : Any): String
}