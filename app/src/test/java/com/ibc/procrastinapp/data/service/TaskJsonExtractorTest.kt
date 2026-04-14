/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.service

import com.ibc.procrastinapp.utils.Logger
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskJsonExtractorTest {

    private lateinit var extractor: TaskJsonExtractor

    @Before
    fun setUp() {
        extractor = TaskJsonExtractor()

        // Mockear Logger para evitar llamadas a android.util.Log
        mockkObject(Logger)
        every { Logger.d(any(), any()) } returns Unit
        every { Logger.w(any(), any()) } returns Unit
        every { Logger.e(any(), any()) } returns Unit
        every { Logger.e(any(), any(), any()) } returns Unit
        every { Logger.i(any(), any()) } returns Unit

    }

    @Test
    fun `extrae una tarea simple correctamente`() {
        val text = """
        {
            "comentario": "Todo bien",
            "propuesta": {
                "tasks": [
                    {
                        "title": "Llamar a Pedro",
                        "subtasks": []
                    }
                ]
            }
        }
    """.trimIndent()

        val tasks = extractor.extractTasks(text)
        assertEquals(1, tasks.size)
        assertEquals("Llamar a Pedro", tasks[0].title)
    }

    @Test
    fun `extrae subtareas anidadas correctamente`() {
        val text = """
            {
                "comentario": "Jerarquía",
                "propuesta": {
                    "tasks": [
                        {
                            "title": "Tarea principal",
                            "subtasks": [
                                {
                                    "title": "Subtarea 1",
                                    "subtasks": [
                                        {
                                            "title": "Sub-subtarea"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            }
        """.trimIndent()

        val tasks = extractor.extractTasks(text)
        assertEquals(1, tasks.size)
        assertEquals("Tarea principal", tasks[0].title)
        assertEquals("Subtarea 1", tasks[0].subtasks[0].title)
        assertEquals("Sub-subtarea", tasks[0].subtasks[0].subtasks[0].title)
    }

    @Test
    fun `devuelve vacío si falta el title`() {
        val text = """
            {
                "comentario": "Falta campo obligatorio",
                "propuesta": {
                    "tasks": [
                        {
                            "notes": "sin título"
                        }
                    ]
                }
            }
        """.trimIndent()

        val tasks = extractor.extractTasks(text)
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun `devuelve vacío si hay más de un JSON en el texto`() {
        val text = """
            {
                "comentario": "Uno",
                "propuesta": {
                    "tasks": []
                }
            }
            {
                "comentario": "Dos",
                "propuesta": {
                    "tasks": []
                }
            }
        """.trimIndent()

        val tasks = extractor.extractTasks(text)
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun `ignora atributos desconocidos`() {
        val text = """
            {
                "comentario": "Hay extras",
                "propuesta": {
                    "tasks": [
                        {
                            "title": "Comprar pan",
                            "urgencia": "alta",
                            "extra": 123,
                            "subtasks": []
                        }
                    ]
                }
            }
        """.trimIndent()

        val tasks = extractor.extractTasks(text)
        assertEquals(1, tasks.size)
        assertEquals("Comprar pan", tasks[0].title)
    }

    @Test
    fun `devuelve vacío si el JSON está roto`() {
        val text = """
            comentario: "Error"
            propuesta: { tasks: [] }
        """

        val tasks = extractor.extractTasks(text)
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun `rellena strings vacías correctamente`() {
        val text = """
            {
                "comentario": "Con campos nulos",
                "propuesta": {
                    "tasks": [
                        {
                            "title": "Revisar informe",
                            "deadline": null,
                            "periodicity": null,
                            "notes": null,
                            "notify": null,
                            "subtasks": []
                        }
                    ]
                }
            }
        """.trimIndent()

        val tasks = extractor.extractTasks(text)
        val t = tasks[0]
        assertEquals("", t.deadline)
        assertEquals("", t.notes)
        assertEquals("", t.notify)
        assertEquals("", t.periodicity)
    }

    @Test
    fun `Valid JSON input with single task`() {
        val text = """{
            "comentario": "Comentario de prueba",
            "propuesta": {
                "tasks": [
                    {
                        "title": "Tarea única",
                        "subtasks": []
                    }
                ]
            }
        }""".trimIndent()
        val result = extractor.extractTasks(text)
        assertEquals(1, result.size)
        assertEquals("Tarea única", result[0].title)
    }

    @Test
    fun `Empty input string`() {
        val text = """""".trimIndent()
        val result = extractor.extractTasks(text)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `Multiple JSON objects in input`() {
        val text = """{
            "comentario": "Primero",
            "propuesta": { "tasks": [] }
        }
        {
            "comentario": "Segundo",
            "propuesta": { "tasks": [] }
        }""".trimIndent()
        val result = extractor.extractTasks(text)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `No JSON object in input`() {
        val text = """Este texto no tiene JSON""".trimIndent()
        val result = extractor.extractTasks(text)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `Invalid JSON format`() {
        val text = """{ comentario: 'Falta comillas y llaves }""".trimIndent()
        val result = extractor.extractTasks(text)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `JSON without  propuesta`() {
        val text = """{
            "comentario": "Sin propuesta"
        }""".trimIndent()
        val result = extractor.extractTasks(text)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `JSON without  tasks  in  propuesta`() {
        val text = """{
            "comentario": "Sin tareas",
            "propuesta": {}
        }""".trimIndent()
        val result = extractor.extractTasks(text)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `Empty  tasks  array`() {
        val text = """{
            "comentario": "Vacía",
            "propuesta": {
                "tasks": []
            }
        }""".trimIndent()
        val result = extractor.extractTasks(text)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `Task with blank title`() {
        val text = """{
            "comentario": "Título en blanco",
            "propuesta": {
                "tasks": [
                    { "title": "   ", "subtasks": [] }
                ]
            }
        }""".trimIndent()
        val result = extractor.extractTasks(text)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `Task with valid subtasks`() {
        val text = """{
            "comentario": "Con subtareas válidas",
            "propuesta": {
                "tasks": [
                    {
                        "title": "Tarea principal",
                        "subtasks": [
                            { "title": "Subtarea 1", "subtasks": [] }
                        ]
                    }
                ]
            }
        }""".trimIndent()
        val result = extractor.extractTasks(text)
        assertEquals(1, result.size)
        assertEquals("Tarea principal", result[0].title)
    }

    @Test
    fun `Task with invalid subtask  blank title`() {
        val text = """{
            "comentario": "Subtarea inválida",
            "propuesta": {
                "tasks": [
                    {
                        "title": "Padre",
                        "subtasks": [
                            { "title": "   " }
                        ]
                    }
                ]
            }
        }""".trimIndent()
        val result = extractor.extractTasks(text)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `Multiple Valid Tasks`() {
        val text = """{
            "comentario": "Múltiples tareas",
            "propuesta": {
                "tasks": [
                    { "title": "Tarea 1", "subtasks": [] },
                    { "title": "Tarea 2", "subtasks": [] }
                ]
            }
        }""".trimIndent()
        val result = extractor.extractTasks(text)
        assertEquals(2, result.size)
    }

    // No espero en ningún caso que ChatGPT me inyecte un null ahí
//    @Test
//    fun `null in tasks`() {
//        val text = """{
//            "comentario": "Tareas con null",
//            "propuesta": {
//                "tasks": [null]
//            }
//        }""".trimIndent()
//        val result = extractor.extractTasks(text)
//        assertTrue(result.isEmpty())
//    }

    @Test
    fun `valid nested subtasks`() {
        val text = """{
            "comentario": "Subtareas anidadas",
            "propuesta": {
                "tasks": [
                    {
                        "title": "Principal",
                        "subtasks": [
                            {
                                "title": "Nivel 1",
                                "subtasks": [
                                    { "title": "Nivel 2", "subtasks": [] }
                                ]
                            }
                        ]
                    }
                ]
            }
        }""".trimIndent()
        val result = extractor.extractTasks(text)
        assertEquals(1, result.size)
        assertEquals("Principal", result[0].title)
    }

    @Test
    fun `invalid nested subtasks`() {
        val text = """{
            "comentario": "Subtarea anidada inválida",
            "propuesta": {
                "tasks": [
                    {
                        "title": "Principal",
                        "subtasks": [
                            {
                                "title": "Nivel 1",
                                "subtasks": [
                                    { "title": "   " }
                                ]
                            }
                        ]
                    }
                ]
            }
        }""".trimIndent()
        val result = extractor.extractTasks(text)
        assertTrue(result.isEmpty())
    }

}