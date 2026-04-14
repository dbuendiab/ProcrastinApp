/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibc.procrastinapp.data.service.quotes.QuoteAIService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuoteViewModel(
    private val quoteAIService: QuoteAIService
) : ViewModel() {

    private var showCount = 0

    fun shouldShowIntroText(): Boolean {
        showCount++
        return showCount <= 3
    }

    // Estado observable de la frase actual
    private val _currentQuote = MutableStateFlow<String?>(null)
    val currentQuote: StateFlow<String?> = _currentQuote.asStateFlow()

    // Error tipado (la UI lo traduce a string localizada)
    private val _quoteError = MutableStateFlow<QuoteError?>(null)
    val quoteError: StateFlow<QuoteError?> = _quoteError.asStateFlow()

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    sealed class QuoteError {
        object LoadFailed : QuoteError()
        object FetchFailed : QuoteError()
    }

    init {
        initializeQuotes()
    }

    private fun initializeQuotes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                quoteAIService.initialize()
                getNextQuote()
            } catch (e: Exception) {
                _quoteError.value = QuoteError.LoadFailed
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getNextQuote() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val quote = quoteAIService.getNextQuote()
                _currentQuote.value = quote
                _quoteError.value = null
            } catch (e: Exception) {
                _quoteError.value = QuoteError.FetchFailed
            } finally {
                _isLoading.value = false
            }
        }
    }
}
