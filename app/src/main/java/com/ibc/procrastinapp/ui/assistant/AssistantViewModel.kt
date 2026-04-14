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
import com.ibc.procrastinapp.ui.assistant.AssistantState.AssistantEvent
import com.ibc.procrastinapp.ui.assistant.AssistantState.LoadTaskError
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

    // Evento tipado que la UI traduce a string localizada
    private val _event = MutableStateFlow<AssistantEvent?>(null)

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
        _event
    ) { messages, tasks, isLoading, error, event ->
        AssistantState(
            messages = messages,
            tasks = tasks,
            isLoading = isLoading,
            chatAIServiceError = error,
            event = event
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
            _event.value = null
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

            _event.value = AssistantEvent.CommitDone(saved = saved, updated = 0, failed = failed)
            Logger.d("IBC13-AssistantViewModel", "commitTasksFromAssistant(): saved=$saved, failed=$failed")
            _commitFinished.emit(Unit)
            Logger.d("IBC13-AssistantViewModel", "commitFinished.emit: guardado finalizado")
            delay(3000)
            _event.value = null
            originalTaskIds.clear()
            assistantRepository.clearTasks()
        }
    }

    fun clearEvent() {
        _event.value = null
    }

    fun loadTasksForEditing(taskIds: List<Long>) {
        viewModelScope.launch {
            val tasks = mutableListOf<Task>()
            val loadErrors = mutableListOf<LoadTaskError>()
            taskIds.forEach { id ->
                try {
                    taskRepository.getTask(id)?.let { tasks.add(it) }
                        ?: loadErrors.add(LoadTaskError.NotFound(id))
                } catch (e: Exception) {
                    loadErrors.add(LoadTaskError.TaskException(id, e.message))
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
                _event.value = AssistantEvent.LoadFailed(loadErrors)
            }
        }
    }

    fun cancelEdit() {
        assistantRepository.clearTasks()
        originalTaskIds.clear()
    }

    private fun convertListTaskToJSON(tasks: List<Task>): String {
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()
        return gson.toJson(mapOf("propuesta" to mapOf("tasks" to tasks)))
    }
}
