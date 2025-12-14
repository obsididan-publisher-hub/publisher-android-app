package com.example.app.repository

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private var retrofit: Retrofit? = null

    fun getApi(host: String): ApiService {
        val baseUrl = if (!host.endsWith("/")) "http://$host/" else "http://$host/"

        if (retrofit == null || retrofit!!.baseUrl().toString() != baseUrl) {
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }
}