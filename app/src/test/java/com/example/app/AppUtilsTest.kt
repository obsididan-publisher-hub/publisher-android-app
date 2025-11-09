package com.example.app

import org.junit.Test
import org.junit.Assert.*

class AppUtilsTest {

/*isUrlCorrect - true, если http + host*/
    @Test
    fun `isUrlCorrect returns true for valid HTTPS URLs`() {
        assertTrue(AppUtils.isUrlCorrect("https://google.com"))
        assertTrue(AppUtils.isUrlCorrect("https://example.com:8080"))
        assertTrue(AppUtils.isUrlCorrect("https://sub.domain.co.uk/path"))
        assertTrue(AppUtils.isUrlCorrect("http://localhost"))
        assertTrue(AppUtils.isUrlCorrect("http://192.168.1.1:3000"))
        assertTrue(AppUtils.isUrlCorrect("http://127.0.0.1/api"))
    }

    @Test
    fun `isUrlCorrect returns false for invalid URLs`() {
        assertFalse(AppUtils.isUrlCorrect("просто текст"))
        assertFalse(AppUtils.isUrlCorrect("http://"))
        assertFalse(AppUtils.isUrlCorrect("https://"))
        assertFalse(AppUtils.isUrlCorrect("://example.com"))
        assertFalse(AppUtils.isUrlCorrect(""))
        assertFalse(AppUtils.isUrlCorrect("   "))
    }

    @Test
    fun `isUrlCorrect returns false for URLs without scheme`() {
        assertFalse(AppUtils.isUrlCorrect("google.com"))
        assertFalse(AppUtils.isUrlCorrect("localhost:8080"))
        assertFalse(AppUtils.isUrlCorrect("192.168.1.1"))
    }

/*extractHost*/
    @Test
    fun `extractHost returns correct host for HTTPS URLs`() {
        assertEquals("google.com", AppUtils.extractHost("https://google.com"))
        assertEquals("example.com", AppUtils.extractHost("https://example.com:8080"))
        assertEquals("sub.domain.co.uk", AppUtils.extractHost("https://sub.domain.co.uk/path"))
        assertEquals("localhost", AppUtils.extractHost("http://localhost"))
        assertEquals("192.168.1.1", AppUtils.extractHost("http://192.168.1.1:3000"))
        assertEquals("127.0.0.1", AppUtils.extractHost("http://127.0.0.1/api"))
    }

    @Test
    fun `extractHost returns null for invalid URLs`() {
        assertNull(AppUtils.extractHost("просто текст"))
        assertNull(AppUtils.extractHost("http://"))
        assertNull(AppUtils.extractHost(""))
        assertNull(AppUtils.extractHost("google.com"))
    }

    @Test
    fun `extractHost handles URLs with special characters`() {
        assertEquals("example.com", AppUtils.extractHost("https://example.com/path?query=param"))
        assertEquals("site.com", AppUtils.extractHost("http://site.com/#section"))
    }
}