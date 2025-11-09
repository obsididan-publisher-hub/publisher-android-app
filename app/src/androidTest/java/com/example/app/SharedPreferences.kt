package com.example.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.Assert.*

class SettingsTest {

    /*Тест на сохранение и чтение хоста через Settings*/
    @Test
    fun `save and read host via Settings wrapper`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = Settings(context)

        settings.clear()

        val expectedHost = "example.com"
        settings["saved_host"] = expectedHost

        val savedHost = settings["saved_host"]

        assertEquals(expectedHost, savedHost)

        settings["saved_host"] = null
        assertNull(settings["saved_host"])
    }
}