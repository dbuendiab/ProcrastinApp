/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ibc.procrastinapp.ui.assistant.elements.AssistantTopBar
import com.ibc.procrastinapp.ui.assistant.elements.MessageInputField
import com.ibc.procrastinapp.ui.assistant.elements.messagesview.AssistantMessagesView
import com.ibc.procrastinapp.ui.assistant.elements.tasksview.AssistantTasksView
import com.ibc.procrastinapp.utils.Logger
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private const val logTag = "IBC11-AssistantScreen"

/**
 * Pantalla principal para la gestión de tareas con IA
 *
 * La pantalla se ha refactorizado siguiendo los siguientes principios:
 * 1. Separación de responsabilidades: Cada componente tiene una única responsabilidad
 * 2. Elevación de estado: El estado se gestiona en el ViewModel y se pasa a los componentes
 * 3. Componentización: La UI se divide en componentes reutilizables y con responsabilidades claras
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    assistantViewModel: AssistantViewModel = koinViewModel(),
    quoteViewModel: QuoteViewModel = koinViewModel(),
    taskIdsToEdit: List<Long> = emptyList(),
    initSession: Boolean = false,
) {

    // Recolección del estado unificado de la UI y el derived state lastResponse
    val uiState by assistantViewModel.uiState.collectAsStateWithLifecycle()
    val lastResponse by assistantViewModel.lastResponse.collectAsStateWithLifecycle()

    // Estados locales de la pantalla
    var agendaButtonClicked by remember { mutableStateOf(false) }
    var currentScreenMode by remember { mutableStateOf(AssistantScreenType.TASKS) }


    // Cuando la lista de tareas viene de TaskListVM vía ruta de navegación
    val isEditMode = taskIdsToEdit.isNotEmpty()


    Logger.d(logTag, "taskIdsToEdit: $taskIdsToEdit")
    LaunchedEffect(taskIdsToEdit) {
        if (taskIdsToEdit.isNotEmpty()) {
            assistantViewModel.loadTasksForEditing(taskIdsToEdit)
        }
    }

    val showSaveButton = uiState.tasks.isNotEmpty() && !uiState.isLoading
    //val tasksIdsString = uiState.tasks.map { it.id }.joinToString(", ")

    LaunchedEffect(Unit) {
        assistantViewModel.commitFinished.collect {
            Logger.d("IBC13", "commitFinished.collect: agendaButtonClicked = false")
            agendaButtonClicked = false
            // showSaveButton debería actualizarse si hay tareas y han cambiado
            // pero es una variable val, así que hay que confiar que se actualice
            // mediante el cambio de uiState.tasks
            //showSaveButton = false
            delay(2000)  // O un toast o snackbar informando

            //NO NAVEGO - TENGO QUE VER LO QUE EXPLICA ASSISTANT
            // Navegar solo después de que se complete el guardado
            navController.navigate("task_list")

        }
    }

    // Cuando se navega a esta pantalla vía FAB de TaskList, se inicia una nueva sesión
    LaunchedEffect(initSession) {
        if (initSession) {
            // Limpiar la lista de mensajes aquí
            assistantViewModel.startNewSession()
        }
    }

    // Timeout de seguridad para limpiar eventos no descartados manualmente
    LaunchedEffect(uiState.event) {
        if (uiState.event != null) {
            delay(10000)
            assistantViewModel.clearEvent()
        }
    }

    Scaffold(
        topBar = {
            AssistantTopBar(
                onGoToTaskList = { navController.navigate("task_list") },
                onDeleteAll = { assistantViewModel.startNewSession() },
                currentScreenMode = currentScreenMode,
                onCurrentScreenModeChange = { newMode ->
                    currentScreenMode = newMode
                },
            )
        }) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Parte superior que ocupa el espacio principal (pero no todo_)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column {

                        if (currentScreenMode == AssistantScreenType.MESSAGES) {
                            Logger.d(logTag, "JSON MODE")
                            // Vista de mensajes completos de ChatGPT, con texto de tareas en JSON
                            AssistantMessagesView(
                                uiState = uiState,
                                onClearViewModelInfo = { assistantViewModel.clearEvent() },
                                quoteViewModel = quoteViewModel,
                                // Quita el padding inferior para dejar espacio al input
                                modifier = Modifier.padding(bottom = 0.dp)
                            )
                        } else {  // modo TaskList
                            Logger.d(logTag, "TASK MODE")
                            // Vista de Tasks con comentarios (convertidas desde los mensajes JSON)
                            AssistantTasksView(
                                uiState = uiState,
                                lastResponse = lastResponse,
                                isEditMode = isEditMode,
                                agendaButtonClicked = agendaButtonClicked,
                                showSaveButton = showSaveButton,
                                onClearViewModelInfo = { assistantViewModel.clearEvent() },
                                onAgendaButtonClick = {
                                    agendaButtonClicked = true
                                    assistantViewModel.commitTasksFromAssistant()
                                    // El cambio de true a false en agendaButtonClicked es vía evento ViewModel
                                },
                                onCancelEdit = { assistantViewModel.cancelEdit() },
                                //tasksIdsString = tasksIdsString,
                                quoteViewModel = quoteViewModel,
                                // Quita el padding inferior para dejar espacio al input
                                modifier = Modifier.padding(bottom = 0.dp)
                            )
                        }
                    }
                }

                // Área de entrada de mensaje
                MessageInputField(
                    onSendMessage = { message ->
                        assistantViewModel.sendMessage(message)
                    },
                    isLoading = uiState.isLoading,
                    // Aquí incluimos TODOS los modificadores que queremos aplicar
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
