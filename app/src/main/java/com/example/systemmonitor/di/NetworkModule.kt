package com.example.systemmonitor.di

import com.example.systemmonitor.data.interfaces.NominatimApi
import com.example.systemmonitor.data.interfaces.OsrmApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okio.Timeout
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * OKHttpClient use to ensure security we send agent here and say yes it's me not fake people
     **/
    @Provides
    @Singleton
    fun provideOkhttpClient() : OkHttpClient{
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                // What user want?
                val originalRequest = chain.request()
                // fine but i send my agent here this is not fake request it ensure our security
                val newRequest = originalRequest.newBuilder()
                    .header("User-Agent","RapidLink-Student-Project")
                    .build()
                chain.proceed(newRequest)
            }
            .retryOnConnectionFailure(true) // retry for data
            .connectTimeout(10, TimeUnit.SECONDS) // connection time 10
            .writeTimeout(10, TimeUnit.SECONDS) // write time
            .readTimeout(15, TimeUnit.SECONDS) // read time
            .callTimeout(20, TimeUnit.SECONDS ) // wait for all if not get the result throw InterruptedIOException
            .followRedirects(true) // follow the https requests (301, 302, 307, 308)
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
    // This is normal coding for us retrofit not big deal
    @Provides
    @Singleton
    fun provideNominatimApi(retrofit: Retrofit) : NominatimApi{
        return retrofit.create(NominatimApi::class.java)
    }

    // Retrofit for Routing Osrm
    @Provides
    @Singleton
    @Named("Osrm") // Hilt is dum we want to tell him this is new and different retrofit
    fun provideOsrmRetrofit(client: OkHttpClient) : Retrofit{
        return Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Osrm Provider
    @Provides
    @Singleton
    fun provideOsrmApi(@Named("Osrm") retrofit: Retrofit) : OsrmApi{
        return retrofit.create(OsrmApi::class.java)
    }

}