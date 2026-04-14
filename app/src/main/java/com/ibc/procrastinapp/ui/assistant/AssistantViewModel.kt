/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.AssistantRepository
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.data.service.AssistantResponse
import com.ibc.procrastinapp.ui.assistant.AssistantState.ViewModelInfo
import com.ibc.procrastinapp.utils.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val assistantRepository: AssistantRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    // Muestra mensajes de confirmación o error
    private val _viewModelInfo = MutableStateFlow<ViewModelInfo?>(null)

    // Señaliza el final de la tarea asíncrona de guardado de tareas
    private val _commitFinished = MutableSharedFlow<Unit>()
    val commitFinished = _commitFinished.asSharedFlow()

    // En el modo edición, la lista de tareas a editar (para borrarlas
    // en caso de que se acepten los cambios - se insertarán como nuevas)
    private val originalTaskIds = mutableListOf<Long>()

    // Estado derivado correspondiente al parsing del último mensaje
    val lastResponse: StateFlow<AssistantResponse?> = assistantRepository.lastResponse

    // Unifica los varios flows en un UiState común
    val uiState: StateFlow<AssistantState> = combine(
        assistantRepository.messages,
        assistantRepository.tasks,
        assistantRepository.isLoading,
        assistantRepository.error,
        _viewModelInfo
    ) { messages, tasks, isLoading, error, viewModelInfo ->
        AssistantState(
            messages = messages,
            tasks = tasks,
            isLoading = isLoading,
            chatAIServiceError = error,
            viewModelInfo = viewModelInfo
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AssistantState()
    )

    fun startNewSession() {
        viewModelScope.launch {
            assistantRepository.initSession()
            originalTaskIds.clear()
            _viewModelInfo.value = null
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch { assistantRepository.sendMessage(message) }
    }

    fun commitTasksFromAssistant() {
        val tasks = assistantRepository.tasks.value
        if (tasks.isEmpty()) return

        // Capturar originalTaskIds ANTES del launch para evitar race conditions
        val taskIdsToDelete = originalTaskIds.toList()

        viewModelScope.launch {
            val isEditMode = taskIdsToDelete.isNotEmpty()
            if (isEditMode) {
                try {
                    taskRepository.deleteTasks(taskIdsToDelete)
                } catch (_: Exception) {}
            }

            var saved = 0
            var failed = 0

            tasks.forEach { task ->
                try {
                    taskRepository.saveTask(task)
                    saved++
                } catch (_: Exception) {
                    failed++
                }
            }

            _viewModelInfo.value = getCommitResultInfo(saved, 0, failed)
            Logger.d("IBC13-AssistantViewModel", "getCommitResultInfo(): _viewModelInfo.value = ${_viewModelInfo.value}")
            _commitFinished.emit(Unit)
            Logger.d("IBC13-AssistantViewModel", "commitFinished.emit: guardado finalizado")
            delay(3000)
            _viewModelInfo.value = null
            originalTaskIds.clear()
            assistantRepository.clearTasks()
        }
    }

    fun clearViewModelInfo() {
        _viewModelInfo.value = null
    }

    fun loadTasksForEditing(taskIds: List<Long>) {
        viewModelScope.launch {
            val tasks = mutableListOf<Task>()
            val loadErrors = mutableListOf<String>()
            taskIds.forEach { id ->
                try {
                    taskRepository.getTask(id)?.let { tasks.add(it) }
                        ?: loadErrors.add("❌ No encontrada tarea ID $id")
                } catch (e: Exception) {
                    loadErrors.add("❌ Error tarea $id: ${e.message}")
                }
            }
            if (tasks.isNotEmpty()) {
                originalTaskIds.clear()
                originalTaskIds.addAll(taskIds)
                val userMessage = if (tasks.size == 1) "Tarea para editar"
                else "Tareas para edición múltiple"
                val json = convertListTaskToJSON(tasks)
                assistantRepository.addUserAndAssistantMessage(userMessage, json)
            }

            if (loadErrors.isNotEmpty()) {
                _viewModelInfo.value = ViewModelInfo.Error(loadErrors.joinToString("\n"))
            }
        }
    }

    fun cancelEdit() {
        assistantRepository.clearTasks()
        originalTaskIds.clear()
    }

    @Suppress("SameParameterValue")
    private fun getCommitResultInfo(saved: Int, updated: Int, failed: Int): ViewModelInfo {
        return when {
            failed == 0 && updated == 0 -> {
                val msg = if (saved == 1) "✓ Una tarea guardada"
                else "✓ $saved tareas guardadas"
                ViewModelInfo.Success(msg)
            }
            failed == 0 && saved == 0 -> {
                val msg = if (updated == 1) "Una tarea actualizada"
                else "✓ $updated tareas actualizadas"
                ViewModelInfo.Success(msg)
            }
            failed == 0 -> {
                val savedMsg = if (saved == 1) "$saved nueva" else "$saved nuevas"
                val updatedMsg = if (updated == 1) "$updated actualizada" else "$updated actualizadas"
                ViewModelInfo.Success("✓ $savedMsg + $updatedMsg")
            }
            saved + updated > 0 -> ViewModelInfo.Warning("⚠️ nuevas: $saved, actualizadas + $updated; fallidas: $failed")
            else -> ViewModelInfo.Error("❌ Ninguna tarea guardada")
        }
    }

    private fun convertListTaskToJSON(tasks: List<Task>): String {
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()
        return gson.toJson(mapOf("propuesta" to mapOf("tasks" to tasks)))
    }
}
