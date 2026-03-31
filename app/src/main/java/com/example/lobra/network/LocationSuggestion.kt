package com.example.lobra.network

import com.google.gson.annotations.SerializedName

data class LocationSuggestion(
    @SerializedName("place_id") val placeId: Long,
    val lat: String,
    val lon: String,
    @SerializedName("display_name") val displayName: String,
    val name: String
)
