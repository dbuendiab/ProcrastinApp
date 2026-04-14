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
import com.ibc.procrastinapp.data.service.ChatAIService
import com.ibc.procrastinapp.data.service.TaskJsonExtractor
import com.ibc.procrastinapp.utils.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantRepositoryTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: CoroutineScope

    private lateinit var chatAIService: ChatAIService
    private lateinit var parser: AssistantResponseParser
    private lateinit var taskExtractor: TaskJsonExtractor
    private lateinit var repository: AssistantRepository

    private val messagesFlow = MutableStateFlow<List<Message>>(emptyList())
    private val isLoadingFlow = MutableStateFlow(false)
    private val serviceErrorFlow = MutableStateFlow<Throwable?>(null)

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        testScope = CoroutineScope(testDispatcher)

        mockkObject(Logger)
        every { Logger.d(any(), any()) } returns Unit
        every { Logger.w(any(), any()) } returns Unit
        every { Logger.e(any(), any()) } returns Unit
        every { Logger.e(any(), any(), any()) } returns Unit
        every { Logger.i(any(), any()) } returns Unit

        chatAIService = mockk(relaxed = true)
        parser = mockk(relaxed = true)
        taskExtractor = mockk(relaxed = true)

        every { chatAIService.messages } returns messagesFlow as StateFlow<List<Message>>
        every { chatAIService.isLoading } returns isLoadingFlow as StateFlow<Boolean>
        every { chatAIService.error } returns serviceErrorFlow as StateFlow<Throwable?>

        repository = AssistantRepository(
            chatAIService = chatAIService,
            coroutineScope = testScope,
            parser = parser,
            taskExtractor = taskExtractor
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.cancel()
    }

    @Test
    fun `cuando llega un mensaje del asistente con JSON valido extrae tareas`() = runTest {
        val json = """{"comentario":"OK","propuesta":{"tasks":[]}}"""
        val assistantMsg = Message.assistantMessage(json)
        val parsedResponse = AssistantResponse(text = "", json = json, commentary = "OK")
        val expectedTasks = listOf(Task(title = "Tarea"))

        every { parser.parse(json) } returns parsedResponse
        every { taskExtractor.extractTasks(json) } returns expectedTasks

        messagesFlow.value = listOf(assistantMsg)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(parsedResponse, repository.lastResponse.value)
        assertEquals(expectedTasks, repository.tasks.value)
        assertNull(repository.error.value)
    }

    @Test
    fun `cuando el mensaje del asistente no tiene JSON las tareas quedan vacias`() = runTest {
        val assistantMsg = Message.assistantMessage("Solo texto, sin JSON.")
        val parsedResponse = AssistantResponse(text = "Solo texto, sin JSON.")

        every { parser.parse(any()) } returns parsedResponse

        messagesFlow.value = listOf(assistantMsg)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(parsedResponse, repository.lastResponse.value)
        assertTrue(repository.tasks.value.isEmpty())
    }

    @Test
    fun `cuando el parser lanza MultipleJsonObjectsFound el error se expone y las tareas quedan vacias`() = runTest {
        val assistantMsg = Message.assistantMessage("dos JSONs aquí")
        val parserError = AssistantResponseParserError.MultipleJsonObjectsFound(2)

        every { parser.parse(any()) } throws parserError

        messagesFlow.value = listOf(assistantMsg)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(repository.lastResponse.value)
        assertTrue(repository.tasks.value.isEmpty())
        val error = repository.error.value
        assertNotNull(error)
        assertTrue(error is AssistantResponseParserError.MultipleJsonObjectsFound)
    }

    @Test
    fun `cuando el parser lanza MalformedJson el error se expone`() = runTest {
        val assistantMsg = Message.assistantMessage("{json roto")
        val parserError = AssistantResponseParserError.MalformedJson(RuntimeException("bad json"))

        every { parser.parse(any()) } throws parserError

        messagesFlow.value = listOf(assistantMsg)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.error.value is AssistantResponseParserError.MalformedJson)
    }

    @Test
    fun `los mensajes de usuario no se parsean`() = runTest {
        val userMsg = Message.userMessage("Este es el usuario hablando")

        messagesFlow.value = listOf(userMsg)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(repository.lastResponse.value)
        assertTrue(repository.tasks.value.isEmpty())
    }

    @Test
    fun `clearTasks limpia tareas y lastResponse`() = runTest {
        val json = """{"comentario":"OK","propuesta":{"tasks":[]}}"""
        val assistantMsg = Message.assistantMessage(json)
        val parsedResponse = AssistantResponse(text = "", json = json, commentary = "OK")

        every { parser.parse(json) } returns parsedResponse
        every { taskExtractor.extractTasks(json) } returns listOf(Task(title = "Tarea"))

        messagesFlow.value = listOf(assistantMsg)
        testDispatcher.scheduler.advanceUntilIdle()

        repository.clearTasks()

        assertNull(repository.lastResponse.value)
        assertTrue(repository.tasks.value.isEmpty())
    }

    @Test
    fun `initSession limpia el estado y delega en ChatAIService`() = runTest {
        val json = """{"comentario":"OK","propuesta":{"tasks":[]}}"""
        every { parser.parse(json) } returns AssistantResponse(json = json)
        every { taskExtractor.extractTasks(json) } returns listOf(Task(title = "Tarea"))
        messagesFlow.value = listOf(Message.assistantMessage(json))
        testDispatcher.scheduler.advanceUntilIdle()

        repository.initSession()

        verify { chatAIService.initSession() }
        assertNull(repository.lastResponse.value)
        assertTrue(repository.tasks.value.isEmpty())
        assertNull(repository.error.value)
    }

    @Test
    fun `el error del servicio de IA se propaga a traves del flow de error`() = runTest {
        val networkError = RuntimeException("Sin conexión")
        serviceErrorFlow.value = networkError
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(networkError, repository.error.value)
    }

    @Test
    fun `el error del parser tiene prioridad sobre el error del servicio`() = runTest {
        val assistantMsg = Message.assistantMessage("{dos}")
        val parserError = AssistantResponseParserError.MultipleJsonObjectsFound(2)
        every { parser.parse(any()) } throws parserError

        serviceErrorFlow.value = RuntimeException("Error de red")
        messagesFlow.value = listOf(assistantMsg)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.error.value is AssistantResponseParserError)
    }
}
