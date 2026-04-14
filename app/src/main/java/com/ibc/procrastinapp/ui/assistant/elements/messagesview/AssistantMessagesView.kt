/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements.messagesview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.ai.AIServiceError
import com.ibc.procrastinapp.data.service.AssistantResponseParserError
import com.ibc.procrastinapp.data.service.SaveMessagesException
import com.ibc.procrastinapp.ui.assistant.AssistantState
import com.ibc.procrastinapp.ui.assistant.AssistantState.ViewModelInfo
import com.ibc.procrastinapp.ui.assistant.QuoteViewModel
import com.ibc.procrastinapp.ui.assistant.elements.StatusCard
import com.ibc.procrastinapp.ui.assistant.elements.TypingIndicator
import com.ibc.procrastinapp.ui.common.AquiNoHayNadaBox
import org.koin.androidx.compose.koinViewModel

/**
 * Vista principal para el modo de mensajes completos
 */
@Composable
fun AssistantMessagesView(
    uiState: AssistantState,
    onClearViewModelInfo: () -> Unit,
    modifier: Modifier = Modifier,
    quoteViewModel: QuoteViewModel
) {
    // Estado local para el LazyColumn
    val lazyListState = rememberLazyListState()

    // Efecto para desplazarse al último mensaje cuando se añade uno nuevo
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
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

        // Mostrar mensaje de guardado si existe
        uiState.viewModelInfo?.let { saveMessage ->
            StatusCard(
                viewModelInfo = saveMessage,
                onDismiss = onClearViewModelInfo,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.messages.isEmpty()) {

                AquiNoHayNadaBox(modifier, quoteViewModel)

            } else {
                // Lista de mensajes
                LazyColumn(
                    modifier = Modifier
                        //.weight(1f)
                        .fillMaxWidth(),
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages) { message ->
                        AssistantMessagesViewMessageItem(message)
                    }

                    // Indicador de "escribiendo..." cuando está cargando
                    if (uiState.isLoading) {
                        item {
                            TypingIndicator()
                        }
                    }
                }

            }
        }

        Spacer(modifier = Modifier.height(8.dp))

    }
}

//////////////////////////////////////////////////////////////////////////////////////////////

@Preview
@Composable
fun MessagesViewInfoPreview() {
    val viewModelInfo = ViewModelInfo.Success("Tarea guardada bien")
    val uiState = AssistantState(
        chatAIServiceError = AIServiceError.Communication("No se pudo salvar la tarea", null),
        viewModelInfo = viewModelInfo
    )
    AssistantMessagesView(
        uiState = uiState,
        onClearViewModelInfo = {},
        quoteViewModel = koinViewModel()
    )
}
