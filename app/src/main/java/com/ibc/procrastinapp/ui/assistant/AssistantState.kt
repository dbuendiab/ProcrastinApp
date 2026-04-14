/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant

import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.model.Task

/**
 * Clase que representa el estado de la UI de la pantalla de asistente IA
 *
 * Ventajas de este enfoque:
 * - Centraliza todo_ el estado en un solo lugar
 * - Facilita testing al tener un estado inmutable
 * - Simplifica la lógica del ViewModel
 * - Evita estados inconsistentes
 */
data class AssistantState(
    val messages: List<Message> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val chatAIServiceError: Throwable? = null,
    val event: AssistantEvent? = null,
) {
    val hasTasks: Boolean get() = tasks.isNotEmpty()

    // Tipo de mensaje de presentación para StatusCard (creado en la capa UI)
    sealed class ViewModelInfo(val text: String) {
        class Success(text: String) : ViewModelInfo(text)
        class Warning(text: String) : ViewModelInfo(text)
        class Error(text: String) : ViewModelInfo(text)
    }

    // Eventos tipados que emite el ViewModel (sin strings hardcoded)
    sealed class AssistantEvent {
        data class CommitDone(val saved: Int, val updated: Int, val failed: Int) : AssistantEvent()
        data class LoadFailed(val errors: List<LoadTaskError>) : AssistantEvent()
    }

    sealed class LoadTaskError {
        data class NotFound(val id: Long) : LoadTaskError()
        data class TaskException(val id: Long, val message: String?) : LoadTaskError()
    }

    // Metodo toString() personalizado para debugging
    override fun toString(): String {
        val sb = StringBuilder()
        sb.appendLine("AssistantState {")
        sb.appendLine("  messages: ${messages.size} items")
        sb.appendLine("  tasks: ${tasks.size} items")
        sb.appendLine("  isLoading: $isLoading")
        sb.appendLine("  chatAIServiceError: ${chatAIServiceError ?: "null"}")
        sb.appendLine("  event: ${event ?: "null"}")
        sb.appendLine("}")
        return sb.toString()
    }
}