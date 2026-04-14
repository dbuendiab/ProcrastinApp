/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
// TaskListContent.kt - Contenido principal de la pantalla
package com.ibc.procrastinapp.ui.tasklist.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.tasklist.TaskListResult
import com.ibc.procrastinapp.ui.tasklist.TaskListUiState

/**
 * Contenido principal de la pantalla de lista de tareas
 *
 * @param uiState Estado actual de la UI
 * @param onTaskClick Acción al pulsar una tarea
 * @param onTaskLongClick Acción al mantener pulsada una tarea
 * @param onCompleteTasksClick Acción al completar tareas seleccionadas
 * @param onDeleteTasksClick Acción al eliminar tareas seleccionadas
 * @param onDismissDialog Acción al cerrar el diálogo de acciones
 */
@Composable
fun TaskListContent(
    uiState: TaskListUiState,
    onTaskClick: (Long) -> Unit,
    onTaskLongClick: (Long) -> Unit,
    onCompleteTasksClick: () -> Unit,
    onEditTasksClick: () -> Unit,
    onDeleteTasksClick: () -> Unit,
    onDismissDialog: () -> Unit,
    onActionsClick: () -> Unit,
    onCompleteSingleTask: (Task) -> Unit,
    onDeleteSingleTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Traducir resultado tipado a mensaje localizado
        when (val r = uiState.result) {
            is TaskListResult.Completed ->
                ErrorMessage(message = stringResource(R.string.tasklist_complete_success), isError = false)
            is TaskListResult.CompleteFailed ->
                ErrorMessage(message = stringResource(R.string.tasklist_complete_failed, r.message ?: ""))
            is TaskListResult.Deleted ->
                ErrorMessage(message = stringResource(R.string.tasklist_delete_success), isError = false)
            is TaskListResult.DeleteFailed ->
                ErrorMessage(message = stringResource(R.string.tasklist_delete_failed, r.message ?: ""))
            null -> Unit
        }

        // Mostrar barra de selección si hay tareas seleccionadas
        if (uiState.hasSelection) {
            SelectionToolbar(
                selectionCount = uiState.selectionCount,
                onActionsClick = onActionsClick
            )
        }

        // Lista de tareas
        TaskList(
            tasks = uiState.tasks,
            selectedTaskIds = uiState.selectedTaskIds,
            onTaskClick = onTaskClick,
            onTaskLongClick = onTaskLongClick,
            showInlineActionsForId = uiState.selectedTaskIds.singleOrNull(), // ✅
            onInlineComplete = { onCompleteSingleTask(it) }, // ← nueva lambda
            onInlineDelete = { onDeleteSingleTask(it) }
        )

        // Diálogo de acciones si está visible
        if (uiState.isActionsDialogVisible) {
            TaskSelectionActionsDialog(
                selectedCount = uiState.selectionCount,
                onComplete = onCompleteTasksClick,
                onEdit = onEditTasksClick,
                onDelete = onDeleteTasksClick,
                onDismiss = onDismissDialog
            )
        }
    }
}

