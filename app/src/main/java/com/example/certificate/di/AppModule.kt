package com.example.certificate.di

import android.content.Context
import com.example.data.repository.CertificateRepositoryImpl
import com.example.data.repository.NetworkCheckerImpl
import com.example.data.repository.ResourceProviderImpl
import com.example.domain.repository.ICertificateRepository
import com.example.domain.repository.NetworkChecker
import com.example.domain.repository.ResourceProvider
import com.example.domain.usecase.GetSSLCertificateUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCertificateRepository(): ICertificateRepository {
        return CertificateRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideGetSSLCertificateUseCase(repository: ICertificateRepository): GetSSLCertificateUseCase {
        return GetSSLCertificateUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideResourceProvider(@ApplicationContext context: Context): ResourceProvider {
        return ResourceProviderImpl(context)
    }

    @Provides
    @Singleton
    fun provideNetworkChecker(@ApplicationContext context: Context): NetworkChecker {
        return NetworkCheckerImpl(context)
    }



}