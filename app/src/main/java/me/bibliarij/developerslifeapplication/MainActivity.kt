package me.bibliarij.developerslifeapplication

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.android.synthetic.main.activity_main.*
import me.bibliarij.developerslifeapplication.client.DevelopersLifeRestClient
import me.bibliarij.developerslifeapplication.client.Post
import org.jetbrains.anko.doAsync
import java.util.concurrent.atomic.AtomicInteger


class MainActivity : AppCompatActivity() {

    private val cacheName = "DEVELOPERS_LIFE_CLIENT_CACHE"

    private lateinit var developersLifeRestClient: DevelopersLifeRestClient

    private val objectMapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(KotlinModule())

    private var counter: AtomicInteger = AtomicInteger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        changePrevButtonState()

        nextButton.setOnClickListener {
            counter.incrementAndGet()
            showNextPost()
            changePrevButtonState()
        }

        previousButton.setOnClickListener {

            showPreviousPost()
            changePrevButtonState()
        }

        developersLifeRestClient = DevelopersLifeRestClient(this, objectMapper)

        showNextPost()
    }

    override fun onDestroy() {
        super.onDestroy()
        val editor: SharedPreferences.Editor =
            getSharedPreferences(cacheName, Context.MODE_PRIVATE).edit()
        editor.clear()
        editor.commit()
    }

    fun hasNetwork(): Boolean {
        val connectivityManager: ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        val result: Boolean = activeNetwork != null && activeNetwork.isConnected

        if (!result) {
            runOnUiThread {

                Glide.with(this)
                    .load(R.drawable.error)
                    .placeholder(R.drawable.loading)
                    .error(R.drawable.error)
                    .into(pictureImageView)

                descriptionTextView.text = "Произошла ошибка при загрузке данных. Проверьте подключение к сети"
            }
        }

        return result
    }

    private fun showNextPost() {
        val counterValue = counter.get()
        doAsync {
            val post =
                if (readString(counterValue.toString()) != null) {
                    getPost(counterValue)
                } else {
                    val randomPost = developersLifeRestClient.getRandomPost()
                    if (randomPost != null) {
                        writeString(counterValue.toString(), objectMapper.writeValueAsString(randomPost))
                    }
                    randomPost
                }

            if (post != null) {
                showPost(post)
            }
        }
    }

    private fun showPreviousPost() {
        if (counter.get() > 0) {
            val newValue = counter.decrementAndGet()
            val post = getPost(newValue)

            if (post != null) {
                showPost(post)
            }
        }
    }

    private fun getPost(value: Int): Post? {
        val string = readString(value.toString())

        return if (string != null) {
            objectMapper.readValue(string, Post::class.java)
        } else {
            null
        }
    }

    private fun showPost(randomPost: Post) {
        runOnUiThread {
            Glide.with(this)
                .load(randomPost.gifURL)
                .placeholder(R.drawable.loading)
                .error(R.drawable.error)
                .into(pictureImageView)

            descriptionTextView.text = randomPost.description
        }
    }

    private fun writeString(key: String, property: String) {
        val editor: SharedPreferences.Editor = getSharedPreferences(cacheName, Context.MODE_PRIVATE).edit()
        editor.putString(key, property)
        editor.commit()
    }

    private fun readString(key: String): String? {
        return getSharedPreferences(cacheName, Context.MODE_PRIVATE).getString(key, null)
    }

    private fun changePrevButtonState(): Unit {
        previousButton.isEnabled = counter.get() != 0
    }
}