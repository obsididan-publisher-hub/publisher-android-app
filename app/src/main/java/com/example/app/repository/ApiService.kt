package com.example.app.repository

import com.example.app.model.SearchResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {

    @GET
    suspend fun getRequest(@Url url: String): Response<String>

    @GET("api/notes/search")
    suspend fun searchNotes(@Query("searchString") query: String): Response<SearchResponse>

    @GET("api/notes/{id}")
    suspend fun getNoteById(@Path("id") id: String): Response<ResponseBody>
}