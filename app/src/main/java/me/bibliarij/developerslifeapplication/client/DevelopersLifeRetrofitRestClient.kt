package me.bibliarij.developerslifeapplication.client

import retrofit2.Call
import retrofit2.http.GET

interface DevelopersLifeRetrofitRestClient {

    @GET("random?json=true")
    fun getRandomPost(): Call<Post>
}