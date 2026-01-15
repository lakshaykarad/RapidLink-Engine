package com.example.systemmonitor.di

import com.example.systemmonitor.data.NominatimApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // provideRetrofit
    // provideNominatimApi
    // https://nominatim.openstreetmap.org/

    @Provides
    @Singleton
    fun provideOkhttpClient() : OkHttpClient{
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()

                val newRequest = originalRequest.newBuilder()
                    .header("User-Agent","RapidLink-Student-Project")
                    .build()
                chain.proceed(newRequest)
            }
            .build()
    }


    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient) : Retrofit{
        return Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideNominatimApi(retrofit: Retrofit) : NominatimApi{
        return retrofit.create(NominatimApi::class.java)
    }





}