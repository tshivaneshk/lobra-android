package com.example.lobra.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NominatimApi {
    @GET("search")
    suspend fun searchLocation(
        @Header("User-Agent") userAgent: String = "LobraApp/1.0",
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("bounded") bounded: Int = 1,
        @Query("viewbox") viewbox: String,
        @Query("limit") limit: Int = 10
    ): List<LocationSuggestion>
}
