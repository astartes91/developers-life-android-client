package me.bibliarij.developerslifeapplication.client

import com.fasterxml.jackson.databind.ObjectMapper
import me.bibliarij.developerslifeapplication.MainActivity
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File

class DevelopersLifeRestClient (private val context: MainActivity, private val objectMapper: ObjectMapper) {

    private val restRetrofitRestClient: DevelopersLifeRetrofitRestClient = buildRestClient()

    fun getRandomPost(): Post? {

        val body: Post? = restRetrofitRestClient.getRandomPost().execute().body()

        return body
    }

    fun getLatest(number: Int): Post? {

        val pageNumber = number / 5
        val page: Page? = restRetrofitRestClient.getLatestPosts(pageNumber).execute().body()
        return getPostFromPage(page, number)
    }

    fun getHot(number: Int): Post? {

        val pageNumber = number / 5
        val page: Page? = restRetrofitRestClient.getHotPosts(pageNumber).execute().body()

        return getPostFromPage(page, number)
    }

    fun getTop(number: Int): Post? {

        val pageNumber = number / 5
        val page: Page? = restRetrofitRestClient.getTopPosts(pageNumber).execute().body()

        return getPostFromPage(page, number)
    }

    private fun getPostFromPage(page: Page?, number: Int): Post?{
        return page?.let {
            val index = number % 5
            it.result.elementAtOrNull(index)
        }
    }

    private fun buildRestClient(): DevelopersLifeRetrofitRestClient {

        val cacheSize: Long = (5 * 1024 * 1024).toLong()
        val cacheDir: File = context.cacheDir
        val myCache: Cache = Cache(cacheDir, cacheSize)

        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .cache(myCache)
            .addInterceptor { chain ->

                var request: Request = chain.request()
                request = if (context.hasNetwork()){
                    /*
                     *  If there is Internet, get the cache that was stored 5 seconds ago.
                     *  If the cache is older than 5 seconds, then discard it,
                     *  and indicate an error in fetching the response.
                     *  The 'max-age' attribute is responsible for this behavior.
                     */
                    request.newBuilder().header("Cache-Control", "public, max-age=" + 5).build()
                } else {
                    /*
                     *  If there is no Internet, get the cache that was stored 7 days ago.
                     *  If the cache is older than 7 days, then discard it,
                     *  and indicate an error in fetching the response.
                     *  The 'max-stale' attribute is responsible for this behavior.
                     *  The 'only-if-cached' attribute indicates to not retrieve new data; fetch the cache only instead.
                     */
                    request.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7)
                        .build()
                }

                chain.proceed(request)
            }
            .build()

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://developerslife.ru")
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .client(okHttpClient)
            .build()

        return retrofit.create(DevelopersLifeRetrofitRestClient::class.java)
    }
}