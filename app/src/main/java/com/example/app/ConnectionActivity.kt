package com.example.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.app.repository.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.net.URL

class ConnectionActivity : AppCompatActivity() {

    private lateinit var userData: EditText
    private lateinit var button: Button
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        userData = findViewById(R.id.user_data)
        button = findViewById(R.id.button)
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        loadSavedHost()
        setupClickListeners()

        val autoLoad = intent.getBooleanExtra("autoLoad", true)
        if (autoLoad) {
            prefs.getString("saved_host", null)?.let { host ->
                if (host.isNotEmpty()) {
                    val url = "http://$host/"
                    performGetRequest(url)
                }
            }
        }
    }

    private fun loadSavedHost() {
        prefs.getString("saved_host", null)?.let { host ->
            userData.setText(host)
        }
    }

    private fun setupClickListeners() {
        button.setOnClickListener {
            handleUserInput()
        }
    }

    private fun handleUserInput() {
        var url = userData.text.toString().trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }

        try {
            val host = URL(url).host
            val port = if (URL(url).port != -1) URL(url).port else 80
            saveHost("$host:$port")
        } catch (_: Exception) {
            Toast.makeText(this, "Некорректный URL", Toast.LENGTH_LONG).show()
            return
        }

        performGetRequest(url)
    }

    private fun createRetrofit(): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://localhost/")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
        return retrofit.create(ApiService::class.java)
    }

    private fun performGetRequest(fullUrl: String) {
        lifecycleScope.launch {
            val api = createRetrofit()
            try {
                val response: Response<String> = withContext(Dispatchers.IO) {
                    api.getRequest(fullUrl)
                }
                if (response.isSuccessful) {
                    val body = response.body().orEmpty()
                    val intent = Intent(this@ConnectionActivity, HtmlViewerActivity::class.java)
                    intent.putExtra("html", body)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@ConnectionActivity, "Ошибка: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                Toast.makeText(this@ConnectionActivity, "Сетевая ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@ConnectionActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveHost(hostWithPort: String) {
        prefs.edit { putString("saved_host", hostWithPort) }
    }
}