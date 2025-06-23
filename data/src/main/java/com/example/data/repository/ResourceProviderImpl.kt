package com.example.data.repository

import android.content.Context
import com.example.domain.repository.ResourceProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ResourceProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
): ResourceProvider {

    override fun getString(id: Int): String = context.getString(id)

    override fun getString(id: Int, vararg args: Any): String = context.getString(id, *args)

}