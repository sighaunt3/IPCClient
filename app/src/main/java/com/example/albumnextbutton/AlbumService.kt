package com.example.albumnextbutton

import retrofit2.Response
import retrofit2.http.GET

interface AlbumService {
    @GET("/")
    suspend fun getFact() : Response<catfact>
}