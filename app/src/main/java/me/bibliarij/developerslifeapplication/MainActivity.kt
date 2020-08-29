package me.bibliarij.developerslifeapplication

import android.content.Context
import android.content.SharedPreferences
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
            getSharedPreferences("DEVELOPERS_LIFE_CLIENT_CACHE", Context.MODE_PRIVATE).edit()
        editor.clear()
        editor.commit()
    }

    private fun showNextPost() {
        doAsync {

            val counterValue = counter.get()
            val post = if(readString(counterValue.toString()) != null){
                getPost(counterValue)
            } else {
                val randomPost = developersLifeRestClient.getRandomPost()
                writeString(counterValue.toString(), objectMapper.writeValueAsString(randomPost))
                randomPost
            }

            showPost(post)
        }
    }

    private fun showPreviousPost() {
        if (counter.get() > 0) {
            val newValue = counter.decrementAndGet()
            val post = getPost(newValue)

            showPost(post)
        }
    }

    private fun getPost(value: Int): Post {
        val string = readString(value.toString())

        return objectMapper.readValue(string, Post::class.java)
    }

    private fun showPost(randomPost: Post) {
        runOnUiThread {
            Glide.with(this)
                .load(randomPost.gifURL)
                .into(pictureImageView)

            descriptionTextView.text = randomPost.description
        }
    }

    private fun writeString(key: String, property: String) {
        val editor: SharedPreferences.Editor =
            getSharedPreferences("DEVELOPERS_LIFE_CLIENT_CACHE", Context.MODE_PRIVATE).edit()
        editor.putString(key, property)
        editor.commit()
    }

    private fun readString(key: String): String? {
        return getSharedPreferences("DEVELOPERS_LIFE_CLIENT_CACHE", Context.MODE_PRIVATE)
            .getString(key, null)
    }

    private fun changePrevButtonState(): Unit {
        previousButton.isEnabled = counter.get() != 0
    }
}