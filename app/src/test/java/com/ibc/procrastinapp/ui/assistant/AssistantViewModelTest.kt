/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant

import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.repository.AssistantRepository
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.data.service.AssistantResponse
import com.ibc.procrastinapp.utils.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var viewModel: AssistantViewModel
    private lateinit var taskRepository: TaskRepository
    private lateinit var assistantRepository: AssistantRepository

    private val messagesFlow = MutableStateFlow<List<Message>>(emptyList())
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private val isLoadingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<Throwable?>(null)
    private val lastResponseFlow = MutableStateFlow<AssistantResponse?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkObject(Logger)
        every { Logger.d(any(), any()) } returns Unit
        every { Logger.w(any(), any()) } returns Unit
        every { Logger.e(any(), any()) } returns Unit
        every { Logger.e(any(), any(), any()) } returns Unit
        every { Logger.i(any(), any()) } returns Unit

        taskRepository = mockk(relaxed = true)
        assistantRepository = mockk(relaxed = true)

        every { assistantRepository.messages } returns messagesFlow as StateFlow<List<Message>>
        every { assistantRepository.tasks } returns tasksFlow as StateFlow<List<Task>>
        every { assistantRepository.isLoading } returns isLoadingFlow as StateFlow<Boolean>
        every { assistantRepository.error } returns errorFlow as StateFlow<Throwable?>
        every { assistantRepository.lastResponse } returns lastResponseFlow as StateFlow<AssistantResponse?>

        viewModel = AssistantViewModel(assistantRepository, taskRepository)
        testScope.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should delete old tasks and insert new ones on commit`() = testScope.runTest {
        val oldTaskId = 1L
        val oldTask = Task(id = oldTaskId, title = "Vieja tarea")
        val newTask = Task(title = "Nueva propuesta")

        coEvery { taskRepository.getTask(oldTaskId) } returns oldTask
        coEvery { taskRepository.saveTask(newTask) } returns 100L

        // 1. Cargar tareas en modo edición
        viewModel.loadTasksForEditing(listOf(oldTaskId))
        advanceUntilIdle()

        // 2. Verificar que originalTaskIds contiene [1]
        val idsField = AssistantViewModel::class.java.getDeclaredField("originalTaskIds")
        idsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        assertEquals(listOf(oldTaskId), idsField.get(viewModel) as List<Long>)

        // 3. El repositorio tiene la nueva tarea propuesta
        tasksFlow.value = listOf(newTask)

        // 4. Ejecutar commit
        viewModel.commitTasksFromAssistant()
        advanceUntilIdle()

        // 5. Verificaciones
        coVerify { taskRepository.deleteTasks(listOf(oldTaskId)) }
        coVerify { taskRepository.saveTask(newTask) }
    }

    @Test
    fun `should insert tasks without deleting if not in edit mode`() = testScope.runTest {
        val newTask = Task(title = "Tarea sin edición")
        coEvery { taskRepository.saveTask(newTask) } returns 200L

        tasksFlow.value = listOf(newTask)

        viewModel.commitTasksFromAssistant()
        advanceUntilIdle()

        coVerify { taskRepository.saveTask(newTask) }
        coVerify(exactly = 0) { taskRepository.deleteTasks(any()) }
    }

    @Test
    fun `should clear taskIds and tasks on cancelEdit`() = testScope.runTest {
        val oldTaskId = 42L
        coEvery { taskRepository.getTask(oldTaskId) } returns Task(id = oldTaskId, title = "Tarea")

        viewModel.loadTasksForEditing(listOf(oldTaskId))
        advanceUntilIdle()

        viewModel.cancelEdit()

        verify { assistantRepository.clearTasks() }

        val originalIdsField = AssistantViewModel::class.java.getDeclaredField("originalTaskIds")
        originalIdsField.isAccessible = true
        val ids = originalIdsField.get(viewModel) as List<*>
        assertTrue(ids.isEmpty())
    }
}
