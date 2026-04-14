/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements.tasksview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.data.service.AssistantResponse
import androidx.compose.ui.res.stringResource
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.ai.AIServiceError
import com.ibc.procrastinapp.data.service.AssistantResponseParserError
import com.ibc.procrastinapp.data.service.SaveMessagesException
import com.ibc.procrastinapp.ui.assistant.AssistantState
import com.ibc.procrastinapp.ui.assistant.AssistantState.AssistantEvent
import com.ibc.procrastinapp.ui.assistant.AssistantState.LoadTaskError
import com.ibc.procrastinapp.ui.assistant.AssistantState.ViewModelInfo
import com.ibc.procrastinapp.ui.assistant.QuoteViewModel
import com.ibc.procrastinapp.ui.assistant.elements.StatusCard
import com.ibc.procrastinapp.utils.Logger

private const val logTag = "IBC-TaskView"

/**
 * Vista principal para el modo de tareas
 */
@Composable
fun AssistantTasksView(
    uiState: AssistantState,
    lastResponse: AssistantResponse?,
    isEditMode: Boolean,
    agendaButtonClicked: Boolean,
    showSaveButton: Boolean,
    onClearViewModelInfo: () -> Unit,
    onAgendaButtonClick: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
    //tasksIdsString: String,
    quoteViewModel: QuoteViewModel

) {

    Logger.d(logTag, "AssistantTasksView(): entrando")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Mostrar error si existe
        uiState.chatAIServiceError?.let { error ->
            val errorMessage = when (error) {
                is AIServiceError.EmptyResponse ->
                    stringResource(R.string.error_ai_empty_response)

                is AIServiceError.Http ->
                    stringResource(R.string.error_ai_http, error.code, error.body ?: "")

                is AIServiceError.Communication ->
                    stringResource(R.string.error_ai_communication, error.detail ?: "desconocido")

                is SaveMessagesException ->
                    stringResource(R.string.error_save_messages, error.cause?.message ?: "")

                is AssistantResponseParserError.MalformedJson ->
                    stringResource(R.string.error_parser_malformed_json)

                is AssistantResponseParserError.MultipleJsonObjectsFound ->
                    stringResource(R.string.error_parser_multiple_json)

                else ->
                    stringResource(R.string.error_unexpected, error.message ?: "")
            }

            StatusCard(
                viewModelInfo = ViewModelInfo.Error(errorMessage),
                onDismiss = onClearViewModelInfo,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Convertir evento tipado a ViewModelInfo con strings localizadas
        val eventViewModelInfo: ViewModelInfo? = when (val e = uiState.event) {
            is AssistantEvent.CommitDone -> when {
                e.failed == 0 && e.updated == 0 ->
                    ViewModelInfo.Success(
                        if (e.saved == 1) stringResource(R.string.commit_one_saved)
                        else stringResource(R.string.commit_n_saved, e.saved)
                    )
                e.failed == 0 && e.saved == 0 ->
                    ViewModelInfo.Success(
                        if (e.updated == 1) stringResource(R.string.commit_one_updated)
                        else stringResource(R.string.commit_n_updated, e.updated)
                    )
                e.failed == 0 ->
                    ViewModelInfo.Success(stringResource(R.string.commit_saved_and_updated, e.saved, e.updated))
                e.saved + e.updated > 0 ->
                    ViewModelInfo.Warning(stringResource(R.string.commit_partial_failure, e.saved, e.updated, e.failed))
                else ->
                    ViewModelInfo.Error(stringResource(R.string.commit_nothing_saved))
            }
            is AssistantEvent.LoadFailed -> {
                val lines = e.errors.map { err ->
                    when (err) {
                        is LoadTaskError.NotFound -> stringResource(R.string.load_task_not_found, err.id)
                        is LoadTaskError.TaskException -> stringResource(R.string.load_task_exception, err.id, err.message ?: "")
                    }
                }
                ViewModelInfo.Error(lines.joinToString("\n"))
            }
            null -> null
        }
        eventViewModelInfo?.let { info ->
            StatusCard(
                modifier = Modifier.fillMaxWidth(),
                viewModelInfo = info,
                onDismiss = onClearViewModelInfo
            )
        }

        // Mostrar comentario y texto del asistente (si hay lastResponse y algún valor)
        lastResponse?.let { response ->
            if (response.hasCommentary || response.hasText) {
                CommentsCard(
                    lastComment = response.commentary,
                    lastAssistantText = response.text,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Lista de tareas (ocupa el espacio restante)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AssistantTasksViewList(
                tasks = uiState.tasks,
                modifier = Modifier.fillMaxSize(),
                quoteViewModel = quoteViewModel
            )
        }

        // Indicador de "escribiendo..." cuando está cargando
        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        }

        // Botón "To Agenda?" solo si hay tareas
        if (uiState.hasTasks) {

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Si estamos en modo edición, añadir un botón de cancelar
                if (isEditMode) {
                    TextButton(
                        onClick = onCancelEdit,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(id = R.string.assistant_cancel))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (showSaveButton) {
                    AssistantTasksViewSaveToAgendaButton(
                        onClick = onAgendaButtonClick,
                        isClicked = agendaButtonClicked,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

    }
}

////////////////////////////////////////////////////////////////////////////

