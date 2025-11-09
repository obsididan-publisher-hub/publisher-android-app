package com.example.app

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import android.content.SharedPreferences
import io.ktor.client.call.body
import io.ktor.client.request.get

class ConnectionActivity : AppCompatActivity() {

    internal val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) { json() }
        }
    }

    internal lateinit var label: TextView
    internal lateinit var userData: EditText
    internal lateinit var button: Button
    internal lateinit var prefs: SharedPreferences
    internal lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        initViews()
        setupPreferences()
        settings = Settings(this)
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
        val savedHost = prefs.getString("saved_host", null)
        savedHost?.let {
            label.text = "Сохранённый хост: $it"
        }
    }

    private fun setupClickListeners() {
        button.setOnClickListener {
            handleUserInput()
        }
    }
    private fun handleUserInput() {
        val text = userData.text.toString().trim()

        if (AppUtils.isUrlCorrect(text)) {
            val host = AppUtils.extractHost(text)
            host?.let { saveHost(it) }
            performGetRequest(text)
        } else {
            Toast.makeText(this, "URL введён некорректно!", Toast.LENGTH_LONG).show()
            label.text = "Некорректный URL: $text"
        }
    }

    internal fun performGetRequest(url: String) {
        lifecycleScope.launch {
            try {
                val response: String = client.get(url).body()
                label.text = response
                Toast.makeText(this@ConnectionActivity, "GET запрос успешен!", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                label.text = "Ошибка GET запроса: ${e.message}"
                Toast.makeText(this@ConnectionActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveHost(host: String) {
        prefs.edit().putString("saved_host", host).apply()
        label.text = "Сохранили хост: $host"
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
    }
}