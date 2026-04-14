/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
// TaskListUiState.kt - Clase para representar el estado de la UI
package com.ibc.procrastinapp.ui.tasklist

import com.ibc.procrastinapp.data.model.Task

/**
 * Clase que representa el estado de la UI de la lista de tareas
 *
 * Ventajas de este enfoque:
 * - Centraliza todo_ el estado en un solo lugar
 * - Facilita testing al tener un estado inmutable
 * - Simplifica la lógica del ViewModel
 */
data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
//    val isLoading: Boolean = false,
    val selectedTaskIds: Set<Long> = emptySet(),
    val result: TaskListResult? = null,
    val isActionsDialogVisible: Boolean = false
) {
    val hasSelection: Boolean get() = selectedTaskIds.isNotEmpty()
    val selectionCount: Int get() = selectedTaskIds.size
}

sealed class TaskListResult {
    object Completed : TaskListResult()
    data class CompleteFailed(val message: String?) : TaskListResult()
    object Deleted : TaskListResult()
    data class DeleteFailed(val message: String?) : TaskListResult()
}
