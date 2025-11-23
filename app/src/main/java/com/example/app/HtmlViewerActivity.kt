package com.example.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.addCallback
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
import androidx.core.net.toUri


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

        // кнопка гамбургер для открытия бокового меню
        val openDrawerBtn: ImageButton = findViewById(R.id.button_open_drawer)
        openDrawerBtn.setOnClickListener {
            drawerLayout.openDrawer(navView)
        }

        // footer "change host"
        val footer = layoutInflater.inflate(R.layout.drawer_footer, navView, false)
        navView.addView(footer)
        val changeHostBtn = footer.findViewById<Button>(R.id.button_change_host)
        changeHostBtn.setOnClickListener {
            val intent = Intent(this, ConnectionActivity::class.java)
            intent.putExtra("autoLoad", false)
            startActivity(intent)
        }
        val footerLayout = footer.findViewById<LinearLayout>(R.id.footer_layout)

        ViewCompat.setOnApplyWindowInsetsListener(navView) { _, insets ->
            val navHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            footerLayout.updatePadding(bottom = navHeight + 32)

            insets
        }
        ViewCompat.requestApplyInsets(navView)

        // webview
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false
                return if (url.startsWith("http://") || url.startsWith("https://")) {
                    view?.loadUrl(url)
                    true
                } else {
                    try { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } catch (_: Exception) {}
                    true
                }
            }
        }

        // кнопка назад
        onBackPressedDispatcher.addCallback(this) {
            when {
                drawerLayout.isDrawerOpen(navView) -> drawerLayout.closeDrawers()
                webView.canGoBack() -> webView.goBack()
                else -> finish()
            }
        }

        // загрузка главной страницы
        val savedHost = getSavedHost() ?: return
        val mainUrl = "http://$savedHost/"
        fetchHtml(mainUrl) { html ->
            webView.loadDataWithBaseURL(mainUrl, html, "text/html", "utf-8", null)
            val notes = parseNotes(html)
            populateDrawer(notes, savedHost)
        }
    }

    private fun getSavedHost(): String? =
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("saved_host", null)

    private fun fetchHtml(url: String, callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = createRetrofit()
                val response: Response<String> = api.getRequest(url)
                if (response.isSuccessful) runOnUiThread { callback(response.body().orEmpty()) }
            } catch (_: Exception) {}
        }
    }

    private fun createRetrofit(): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
        return Retrofit.Builder()
            .baseUrl("https://localhost/")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun parseNotes(html: String): List<Pair<String, String>> =
        Jsoup.parse(html)
            .select("a[href^=/notes/]")
            .map { it.text() to it.attr("href") }

    private fun populateDrawer(notes: List<Pair<String, String>>, host: String) {
        val menu = navView.menu
        menu.clear()
        notes.forEachIndexed { index, pair ->
            menu.add(0, index, 0, pair.first).setOnMenuItemClickListener {
                webView.loadUrl("http://$host${pair.second}")
                drawerLayout.closeDrawers()
                true
            }
        }
    }
}