package com.example.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.repository.ApiClient
import com.example.app.repository.ApiService
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import org.jsoup.Jsoup
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class HtmlViewerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var searchResults: RecyclerView
    private lateinit var adapter: NotesAdapter
    private var isSearchActive = true
    private var hasShownNoResultsToast = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_html_viewer)

        webView = findViewById(R.id.webView)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        searchView = findViewById(R.id.search_view)
        searchResults = findViewById(R.id.search_results)

        setupWebView()
        setupDrawerWithFooter()  // гамбургер + footer
        setupRecyclerView()
        setupSearch()
        loadMainPage()

        // кнопка назад
        onBackPressedDispatcher.addCallback(this) {
            when {
                drawerLayout.isDrawerOpen(navView) -> drawerLayout.closeDrawers()
                webView.canGoBack() -> webView.goBack()
                else -> finish()
            }
        }
    }

    private fun getSavedHost(): String? =
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("saved_host", null)

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val savedHost = getSavedHost() ?: return
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false
                return if (url.startsWith("http://$savedHost") || url.startsWith("https://$savedHost")) {
                    view?.loadUrl(url)
                    true
                } else {
                    try { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } catch (_: Exception) {}
                    true
                }
            }
        }
    }

    // drawer с кнопкой гамбургер и footer с кнопкой изменения хоста
    private fun setupDrawerWithFooter() {
        // гамбургер
        val openDrawerBtn: ImageButton = findViewById(R.id.button_open_drawer)
        openDrawerBtn.setOnClickListener { drawerLayout.openDrawer(navView) }

        // footer
        val footer = layoutInflater.inflate(R.layout.drawer_footer, navView, false)
        navView.addView(footer)
        val changeHostBtn = footer.findViewById<Button>(R.id.button_change_host)
        changeHostBtn.setOnClickListener {
            startActivity(Intent(this, ConnectionActivity::class.java).apply {
                putExtra("autoLoad", false)
            })
        }
        val footerLayout = footer.findViewById<LinearLayout>(R.id.footer_layout)
        ViewCompat.setOnApplyWindowInsetsListener(navView) { _, insets ->
            footerLayout.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom + 32)
            insets
        }
        ViewCompat.requestApplyInsets(navView)
    }

    private fun setupRecyclerView() {
        adapter = NotesAdapter(emptyList()) { noteId -> openNote(noteId) }
        searchResults.layoutManager = LinearLayoutManager(this)
        searchResults.adapter = adapter

        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(this, R.drawable.divider_line)?.let { divider.setDrawable(it) }
        searchResults.addItemDecoration(divider)
        searchResults.visibility = View.GONE
    }

    private fun openNote(noteId: String) {
        val savedHost = getSavedHost() ?: return
        webView.loadUrl("http://$savedHost/notes/$noteId")
        isSearchActive = false
        searchResults.visibility = View.GONE
        searchView.setQuery("", false)
        searchView.clearFocus()
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performServerSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    isSearchActive = true
                    hasShownNoResultsToast = false
                    if (it.isBlank()) searchResults.visibility = View.GONE
                    else performServerSearch(it)
                }
                return true
            }
        })
    }

    private fun performServerSearch(query: String) {
        if (!isSearchActive) return
        val savedHost = getSavedHost() ?: return
        val api = ApiClient.getApi(savedHost)

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { api.searchNotes(query) }
                if (!response.isSuccessful) return@launch

                val ids = response.body()?.noteIds ?: emptyList()
                val loadedTitles = mutableListOf<Pair<String, String>>()

                ids.map { id ->
                    async(Dispatchers.IO) {
                        try {
                            val noteResponse = api.getNoteById(id)
                            val rawJson = noteResponse.body()?.string()
                            if (!rawJson.isNullOrEmpty() && noteResponse.isSuccessful) {
                                val jsonObj = JSONObject(rawJson)
                                val title = jsonObj.getString("title")
                                title to id
                            } else null
                        } catch (_: Exception) { null }
                    }
                }.awaitAll().filterNotNull().forEach { loadedTitles.add(it) }

                adapter.updateNotes(loadedTitles)
                searchResults.visibility = if (loadedTitles.isNotEmpty()) View.VISIBLE else View.GONE

                if (loadedTitles.isEmpty() && !hasShownNoResultsToast) {
                    Toast.makeText(this@HtmlViewerActivity, "Заметки не найдены", Toast.LENGTH_SHORT).show()
                    hasShownNoResultsToast = true
                }
            } catch (_: Exception) {
                adapter.updateNotes(emptyList())
                searchResults.visibility = View.GONE
            }
        }
    }

    private fun loadMainPage() {
        val savedHost = getSavedHost() ?: return
        val mainUrl = "http://$savedHost/"
        fetchHtml(mainUrl) { html ->
            webView.loadDataWithBaseURL(mainUrl, html, "text/html", "utf-8", null)
            val notes = parseNotes(html)
            populateDrawer(notes, savedHost)
        }
    }

    private fun fetchHtml(url: String, callback: (String) -> Unit) {
        val host = getSavedHost() ?: return
        val api = createRetrofit(host)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: Response<String> = api.getRequest(url)
                if (response.isSuccessful) runOnUiThread { callback(response.body().orEmpty()) }
            } catch (_: Exception) {}
        }
    }

    private fun createRetrofit(host: String): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
        val baseUrl = if (!host.endsWith("/")) "http://$host/" else "http://$host/"
        return Retrofit.Builder()
            .baseUrl(baseUrl)
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
        menu.add(0, -1, 0, "Главная").setOnMenuItemClickListener {
            webView.loadUrl("http://$host/")
            drawerLayout.closeDrawers()
            true
        }
        notes.forEachIndexed { index, pair ->
            menu.add(0, index, index + 1, pair.first).setOnMenuItemClickListener {
                webView.loadUrl("http://$host${pair.second}")
                drawerLayout.closeDrawers()
                true
            }
        }
    }
}