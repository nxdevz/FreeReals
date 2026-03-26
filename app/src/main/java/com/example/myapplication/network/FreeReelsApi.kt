package com.example.myapplication.network

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface FreeReelsApi {
    @GET("api/freereels/foryou")
    suspend fun getForYou(): JsonElement

    @GET("api/freereels/homepage")
    suspend fun getHomepage(): JsonElement

    @GET("api/freereels/animepage")
    suspend fun getAnimePage(): JsonElement

    @GET("api/freereels/search")
    suspend fun search(@Query("query") query: String): JsonElement
}