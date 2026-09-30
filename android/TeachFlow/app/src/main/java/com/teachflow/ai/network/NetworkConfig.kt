package com.teachflow.ai.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object NetworkConfig {
    /**
     * Default backend base URL.
     * Note: 10.0.2.2 points to host machine's localhost from Android Emulator.
     */
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8000"

    private val _baseUrl = MutableStateFlow(DEFAULT_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl

    private val _useMockBackend = MutableStateFlow(false)
    val useMockBackend: StateFlow<Boolean> = _useMockBackend

    fun setBaseUrl(url: String) {
        val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "http://$url"
        } else {
            url
        }
        _baseUrl.value = formatted.trimEnd('/')
    }

    fun setUseMockBackend(enabled: Boolean) {
        _useMockBackend.value = enabled
    }
}
