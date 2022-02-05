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
import com.google.android.material.tabs.TabLayout
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

    private val counters: Map<Tabs, AtomicInteger> = Tabs.values().associateWith { AtomicInteger() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        changePrevButtonState()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {

                changePrevButtonState()

                val tabCode: Tabs = getTab(tab!!.text!!)
                val counter = counters[tabCode]!!.get()

                getCachedPost(tabCode, counter)?.let {
                    showPost(it)
                } ?: showNextPost()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })

        nextButton.setOnClickListener {
            getCurrentCounter().incrementAndGet()
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
        val editor: SharedPreferences.Editor = getSharedPreferences(cacheName, Context.MODE_PRIVATE).edit()
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
        val currentTab: Tabs = getCurrentTab()
        val counterValue = counters[currentTab]!!.get()
        doAsync {
            val key = "${currentTab}_$counterValue"

            val post = getCachedPost(currentTab, counterValue) ?: when (currentTab) {
                Tabs.RANDOM -> developersLifeRestClient.getRandomPost()
                Tabs.LATEST -> developersLifeRestClient.getLatest(counterValue)
                Tabs.HOT -> developersLifeRestClient.getHot(counterValue)
                Tabs.TOP -> developersLifeRestClient.getTop(counterValue)
            }?.also {
                writeString(key, objectMapper.writeValueAsString(it))
            }

            showPost(post)
        }
    }

    private fun showPreviousPost() {
        if (getCurrentCounter().get() > 0) {
            val newValue = getCurrentCounter().decrementAndGet()
            val post = getCachedPost(getCurrentTab(), newValue)

            showPost(post)
        }
    }

    private fun getCachedPost(tab: Tabs, value: Int): Post? {

        val key = "${tab}_$value"
        val string = readString(key)

        return if (string != null) {
            objectMapper.readValue(string, Post::class.java)
        } else {
            null
        }
    }

    private fun showPost(post: Post?) {
        if (post != null) {
            val (description, gifURL) = post
            runOnUiThread {
                Glide.with(this)
                    .load(gifURL)
                    .placeholder(R.drawable.loading)
                    .error(R.drawable.error)
                    .fitCenter()
                    .into(pictureImageView)

                descriptionTextView.text = description
            }
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
        previousButton.isEnabled = getCurrentCounter().get() != 0
    }

    private fun getCurrentTab(): Tabs {
        return getTab(tabLayout.getTabAt(tabLayout.selectedTabPosition)!!.text!!)
    }

    private fun getTab(text: CharSequence): Tabs {
        return Tabs.values().first { it.text == text }
    }

    private fun getCurrentCounter(): AtomicInteger {
        return counters[getCurrentTab()]!!
    }

    enum class Tabs(val text: String) {
        RANDOM("Случайные"), LATEST("Последние"), HOT("Горячие"), TOP("Лучшие")
    }
}