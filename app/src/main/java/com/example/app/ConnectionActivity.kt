package com.example.app

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import java.net.URL

class ConnectionActivity : AppCompatActivity() {

    private lateinit var label: TextView
    private lateinit var userData: EditText
    private lateinit var button: Button
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        initViews()
        setupPreferences()
        loadSavedHost()
        setupClickListeners()
    }

    private fun initViews() {
        label = findViewById(R.id.main_label)
        userData = findViewById(R.id.user_data)
        button = findViewById(R.id.button)
    }

    private fun setupPreferences() {
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    private fun loadSavedHost() {
        prefs.getString("saved_host", null)?.let { savedHost ->
            userData.setText(savedHost)
            label.text = "Сохранённый IP и порт: $savedHost"
        }
    }

    private fun setupClickListeners() {
        button.setOnClickListener {
            handleUserInput()
        }
    }

    private fun handleUserInput() {
        var url = userData.text.toString().trim()

        // если протокола нет, добавляем http://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }

        if (!AppUtils.isUrlCorrect(url)) {
            Toast.makeText(this, "URL введён некорректно!", Toast.LENGTH_LONG).show()
            label.text = "Некорректный URL: $url"
            return
        }

        // сохраняем только IP и порт
        AppUtils.extractHost(url)?.let { host ->
            val port = URL(url).port.takeIf { it != -1 } ?: 80
            saveHost("$host:$port")
        }

        performGetRequest(url)
    }

    private fun createRetrofit(): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://localhost/") // фиктивный baseUrl
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
                    label.text = when {
                        body.isEmpty() -> "Пустой ответ"
                        body.length > 1000 -> "Ответ слишком длинный:\n${body.take(1000)}..."
                        else -> body
                    }
                    Toast.makeText(this@ConnectionActivity, "GET запрос успешен!", Toast.LENGTH_SHORT).show()
                } else {
                    label.text = "Ошибка: ${response.code()}"
                }
            } catch (e: IOException) {
                label.text = "Сетевая ошибка: ${e.message}"
            } catch (e: Exception) {
                label.text = "Ошибка: ${e.message}"
            }
        }
    }

    private fun saveHost(hostWithPort: String) {
        prefs.edit { putString("saved_host", hostWithPort) }
        label.text = "Сохранили IP и порт: $hostWithPort"
    }
}
