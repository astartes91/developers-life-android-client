package me.bibliarij.developerslifeapplication.client

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface DevelopersLifeRetrofitRestClient {

    @GET("random?json=true")
    fun getRandomPost(): Call<Post>

    @GET("latest/{pageSize}?json=true")
    fun getLatestPosts(@Path("pageSize") pageSize: Int): Call<Page>

    @GET("hot/{pageSize}?json=true")
    fun getHotPosts(@Path("pageSize") pageSize: Int): Call<Page>

    @GET("top/{pageSize}?json=true")
    fun getTopPosts(@Path("pageSize") pageSize: Int): Call<Page>
}