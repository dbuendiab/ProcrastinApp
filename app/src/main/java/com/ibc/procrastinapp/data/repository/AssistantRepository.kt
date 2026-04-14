/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.repository

import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.service.AssistantResponse
import com.ibc.procrastinapp.data.service.AssistantResponseParser
import com.ibc.procrastinapp.data.service.AssistantResponseParserError
import com.ibc.procrastinapp.data.service.AssistantResponseParserImpl
import com.ibc.procrastinapp.data.service.ChatAIService
import com.ibc.procrastinapp.data.service.TaskJsonExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Capa intermedia entre ChatAIService y el ViewModel.
 * Responsabilidades:
 * - Observar el flujo de mensajes del servicio de IA
 * - Parsear los mensajes del asistente (texto → AssistantResponse)
 * - Extraer las tareas del JSON resultante (AssistantResponse → List<Task>)
 * - Exponer el estado procesado listo para consumir desde el ViewModel
 */
class AssistantRepository(
    private val chatAIService: ChatAIService,
    coroutineScope: CoroutineScope
) {
    private val parser: AssistantResponseParser = AssistantResponseParserImpl()
    private val taskExtractor: TaskJsonExtractor = TaskJsonExtractor()

    val messages: StateFlow<List<Message>> = chatAIService.messages
    val isLoading: StateFlow<Boolean> = chatAIService.isLoading

    private val _lastResponse = MutableStateFlow<AssistantResponse?>(null)
    val lastResponse: StateFlow<AssistantResponse?> = _lastResponse.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _parserError = MutableStateFlow<Throwable?>(null)

    // Unifica errores del servicio de IA y del parser en un único flujo
    val error: StateFlow<Throwable?> = combine(chatAIService.error, _parserError) { serviceError, parserError ->
        parserError ?: serviceError
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        coroutineScope.launch {
            chatAIService.messages.collectLatest { messages ->
                val lastAssistantMessage = messages.lastOrNull { it.isAssistant }
                if (lastAssistantMessage != null) {
                    try {
                        val response = parser.parse(lastAssistantMessage.content)
                        _lastResponse.value = response
                        _tasks.value = if (response.hasJson)
                            taskExtractor.extractTasks(response.json)
                        else emptyList()
                        _parserError.value = null
                    } catch (e: AssistantResponseParserError) {
                        _lastResponse.value = null
                        _tasks.value = emptyList()
                        _parserError.value = e
                    }
                } else {
                    _lastResponse.value = null
                    _tasks.value = emptyList()
                }
            }
        }
    }

    fun clearTasks() {
        _tasks.value = emptyList()
        _lastResponse.value = null
    }

    suspend fun sendMessage(userInput: String) = chatAIService.sendMessage(userInput)

    fun initSession() {
        chatAIService.initSession()
        _lastResponse.value = null
        _tasks.value = emptyList()
        _parserError.value = null
    }

    fun addUserAndAssistantMessage(userMsg: String, taskJson: String) =
        chatAIService.addUserAndAssistantMessage(userMsg, taskJson)
}
