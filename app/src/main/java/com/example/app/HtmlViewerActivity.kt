package com.example.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.Response
import org.jsoup.Jsoup

class HtmlViewerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_html_viewer)

        webView = findViewById(R.id.webView)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false
                return when {
                    url.startsWith("http://") || url.startsWith("https://") -> {
                        view?.loadUrl(url)
                        true
                    }
                    else -> {
                        // внешние схемы через браузер
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        true
                    }
                }
            }
        }

        val savedHost = getSavedHost() ?: return

        val mainUrl = "http://$savedHost/"
        fetchHtml(mainUrl) { html ->
            webView.loadDataWithBaseURL(mainUrl, html, "text/html", "utf-8", null)
            // парсим заметки и заполняем шторку
            val notes = parseNotes(html)
            populateDrawer(notes, savedHost)
        }
    }

    private fun getSavedHost(): String? {
        return getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("saved_host", null)
    }

    private fun fetchHtml(url: String, callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = createRetrofit()
                val response: Response<String> = api.getRequest(url)
                if (response.isSuccessful) {
                    val body = response.body().orEmpty()
                    runOnUiThread { callback(body) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createRetrofit(): ApiService {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://localhost/") // фиктивный baseUrl
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
        return retrofit.create(ApiService::class.java)
    }

    private fun parseNotes(html: String): List<Pair<String, String>> {
        val notes = mutableListOf<Pair<String, String>>()
        val doc = Jsoup.parse(html)
        val links = doc.select("a[href^=/notes/]")
        for (link in links) {
            val title = link.text()
            val href = link.attr("href")
            notes.add(Pair(title, href))
        }
        return notes
    }

    private fun populateDrawer(notes: List<Pair<String, String>>, host: String) {
        val menu = navView.menu
        menu.clear()

        notes.forEachIndexed { index, pair ->
            val item = menu.add(0, index, 0, pair.first)
            item.setOnMenuItemClickListener {
                val fullUrl = "http://$host${pair.second}"
                webView.loadUrl(fullUrl)
                drawerLayout.closeDrawers()
                true
            }
        }
    }
}
