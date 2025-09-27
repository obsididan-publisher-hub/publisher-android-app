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
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import com.example.app.Settings
private lateinit var settings: Settings

class ConnectionActivity : AppCompatActivity() {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        settings = Settings(this)
        val label = findViewById<TextView>(R.id.main_label)
        val userData: EditText = findViewById(R.id.user_data)
        val button: Button = findViewById(R.id.button)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val savedHost = prefs.getString("saved_host", null)
        if (savedHost != null) {
            label.text = "Сохранённый хост: $savedHost"
        }

        button.setOnClickListener {
            val text = userData.text.toString().trim()

            if (AppUtils.isUrlCorrect(text)) {
                val host = AppUtils.extractHost(text)
                if (host != null) {
                    prefs.edit().putString("saved_host", host).apply()
                    label.text = "Сохранили хост: $host"
                }
                AppUtils.tryToOpenStartNote(this, text)
            } else {
                label.text = text
            }

            lifecycleScope.launch {
                try {
                    val response: String = client.get("https://jsonplaceholder.typicode.com/users/100").body()
                    label.text = response
                } catch (e: Exception) {
                    Toast.makeText(this@ConnectionActivity, "Ошибка запроса: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
    }
}