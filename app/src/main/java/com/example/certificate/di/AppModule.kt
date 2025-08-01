package com.example.certificate.di

import android.content.Context
import android.net.ConnectivityManager
import com.example.data.network.NetworkObserverImpl
import com.example.data.repository.CertificateRepositoryImpl
import com.example.data.repository.ResourceProviderImpl
import com.example.domain.network.NetworkObserver
import com.example.domain.repository.ICertificateRepository
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
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    }

    @Provides
    @Singleton
    fun provideNetworkObserver(connectivityManager: ConnectivityManager): NetworkObserver {
        return NetworkObserverImpl(connectivityManager)
    }

}