package me.bibliarij.developerslifeapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import me.bibliarij.developerslifeapplication.client.DevelopersLifeRestClient
import org.jetbrains.anko.doAsync

class MainActivity : AppCompatActivity() {

    private lateinit var developersLifeRestClient: DevelopersLifeRestClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        developersLifeRestClient = DevelopersLifeRestClient(this)

        doAsync {
            val randomPost = developersLifeRestClient.getRandomPost()

            runOnUiThread {
                Glide.with(this@MainActivity)
                    .load(randomPost.gifURL)
                    .into(pictureImageView)
            }
        }
    }
}