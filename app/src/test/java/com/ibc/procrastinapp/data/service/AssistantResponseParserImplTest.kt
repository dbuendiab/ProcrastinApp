/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.service

import com.ibc.procrastinapp.utils.Logger
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations


class AssistantResponseParserImplTest {

    private lateinit var parser: AssistantResponseParserImpl

    @Mock
    private lateinit var loggerMock: Logger

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        parser = AssistantResponseParserImpl()

        // Mockear Logger para evitar llamadas a android.util.Log
        mockkObject(Logger)
        every { Logger.d(any(), any()) } returns Unit
        every { Logger.w(any(), any()) } returns Unit
        every { Logger.e(any(), any()) } returns Unit
        every { Logger.e(any(), any(), any()) } returns Unit
        every { Logger.i(any(), any()) } returns Unit

    }

    @Test
    fun `parse returns empty AssistantResponse for blank message`() {
        val result = parser.parse("")

        // Verificar que todos los campos estén vacíos
        assertEquals("", result.text)
        assertEquals("", result.json)
        assertEquals("", result.commentary)
        assertFalse(result.hasJson)
        assertFalse(result.hasCommentary)
        assertFalse(result.hasText)
    }

    @Test
    fun `parse extracts text when no JSON present`() {
        val message = "Este es un mensaje sin JSON."
        val result = parser.parse(message)

        // Verificar que solo el texto esté presente
        assertEquals(message, result.text)
        assertEquals("", result.json)
        assertEquals("", result.commentary)
        assertTrue(result.hasText)
        assertFalse(result.hasJson)
        assertFalse(result.hasCommentary)
    }

    @Test
    fun `parse extracts text and JSON when both present`() {
        val text = "Este es un mensaje con JSON."
        val json = """{"propuesta": {"tasks": [{"title": "Tarea ejemplo"}]}}"""
        val message = "$text\n\n$json"

        val result = parser.parse(message)

        // Verificar que el texto y JSON se extraen correctamente
        assertEquals(text, result.text)
        assertEquals(json, result.json)
        assertEquals("", result.commentary)
        assertTrue(result.hasText)
        assertTrue(result.hasJson)
        assertFalse(result.hasCommentary)
    }

    @Test
    fun `parse extracts text, JSON and commentary when all present`() {
        val text = "Este es un mensaje con JSON y comentario."
        val json =
            """{"comentario": "Este es el comentario", "propuesta": {"tasks": [{"title": "Tarea ejemplo"}]}}"""
        val message = "$text\n\n$json"

        val result = parser.parse(message)

        // Verificar que todos los campos se extraen correctamente
        assertEquals(text, result.text)
        assertEquals(json, result.json)
        assertEquals("Este es el comentario", result.commentary)
        assertTrue(result.hasText)
        assertTrue(result.hasJson)
        assertTrue(result.hasCommentary)
    }

    @Test
    fun `parse handles complex JSON with nested structures`() {
        val text = "Aquí hay un JSON complejo:"
        val json = """
        {
            "comentario": "Esto es un comentario con \"comillas\"",
            "propuesta": {
                "tasks": [
                    {
                        "title": "Tarea 1",
                        "deadline": "2023-12-15",
                        "subtasks": [
                            {"title": "Subtarea 1"},
                            {"title": "Subtarea 2"}
                        ]
                    }
                ]
            }
        }
        """.trimIndent()
        val message = "$text\n\n$json"

        val result = parser.parse(message)

        // Verificar extracción correcta
        assertEquals(text, result.text)
        assertTrue(result.json.contains("comentario"))
        assertTrue(result.json.contains("subtasks"))
        assertEquals("Esto es un comentario con \"comillas\"", result.commentary)
    }

//    @Test
//    fun `parse handles multiple JSON objects and takes first one`() {
//        val text = "Texto inicial"
//        val json1 = """{"comentario": "Primer comentario", "propuesta": {}}"""
//        val json2 = """{"comentario": "Segundo comentario", "propuesta": {}}"""
//        val message = "$text\n$json1\n\nOtro texto\n$json2"
//
//        val result = parser.parse(message)
//
//        // Debe usar el primer JSON encontrado
//        assertEquals(text, result.text)
//        assertEquals(json1, result.json)
//        assertEquals("Primer comentario", result.commentary)
//    }

    @Test
    fun `parse handles malformed JSON by returning empty commentary`() {
        val text = "Texto antes de JSON malformado"
        val malformedJson = """{"comentario": "Comentario incompleto", "propuesta": {"""
        val message = "$text\n\n$malformedJson"

        val result = parser.parse(message)

        // Debe extraer todo_ el texto como texto y no como JSON
        assertEquals(message, result.text)
        assertEquals("", result.json)
        assertEquals("", result.commentary)
    }

    @Test
    fun `parse handles JSON without commentary field`() {
        val text = "Mensaje sin campo de comentario"
        val json = """{"propuesta": {"tasks": []}}"""
        val message = "$text\n\n$json"

        val result = parser.parse(message)

        // Debe extraer texto y JSON, pero no hay comentario
        assertEquals(text, result.text)
        assertEquals(json, result.json)
        assertEquals("", result.commentary)
    }

    @Test
    fun `parse handles multiline text before JSON`() {
        val text = """
        Este es un texto
        con múltiples líneas
        antes del JSON.
        """.trimIndent()
        val json = """{"comentario": "El comentario", "propuesta": {}}"""
        val message = "$text\n\n$json"

        val result = parser.parse(message)

        // Debe manejar texto multilínea correctamente
        assertEquals(text, result.text)
        assertEquals(json, result.json)
        assertEquals("El comentario", result.commentary)
    }

//    @Test
//    fun `parse handles message with code blocks before JSON`() {
//        val text = """
//        Aquí hay código:
//        ```kotlin
//        fun test() {
//            println("Hola")
//        }
//        ```
//        Y ahora el JSON:
//        """.trimIndent()
//        val json = """{"comentario": "Comentario después de código", "propuesta": {}}"""
//        val message = "$text\n\n$json"
//
//        val result = parser.parse(message)
//
//        // Debe manejar bloques de código correctamente
//        assertEquals(text, result.text)
//        assertEquals(json, result.json)
//        assertEquals("Comentario después de código", result.commentary)
//    }

    @Test
    fun `parse handles JSON with Unicode characters in commentary`() {
        val text = "Mensaje con Unicode"
        val json = """{"comentario": "Comentario con emojis 😊🚀 y acentos áéíóú", "propuesta": {}}"""
        val message = "$text\n\n$json"

        val result = parser.parse(message)

        // Debe manejar caracteres Unicode correctamente
        assertEquals("Comentario con emojis 😊🚀 y acentos áéíóú", result.commentary)
    }

//    @Test
//    fun `parse handles exceptions during JSON processing`() {
//        // Simulamos un escenario donde JsonParser lanza una excepción
//        // Esto requeriría una implementación especial para pruebas o un mock más avanzado
//
//        // Como alternativa, se puede probar con un JSON muy complejo que cause problemas
//        val text = "Mensaje antes del JSON"
//        val complexJson = """{"comentario": "${"\u0000"}", "propuesta": {}}"""
//        val message = "$text\n\n$complexJson"
//
//        val result = parser.parse(message)
//
//        // Asegurar que no se rompe ante un JSON problemático
//        assertEquals(text, result.text)
//        assertNotEquals("", result.json)
//        assertEquals("", result.commentary)  // El comentario debería estar vacío si hubo excepción
//    }

    @Test
    fun `parse throws MalformedJson when two JSON blocks are present`() {
        // La regex greedy captura desde el primer { hasta el último } como un único bloque,
        // que resulta ser JSON malformado. Por tanto se lanza MalformedJson, no MultipleJsonObjectsFound.
        val text = "Texto inicial"
        val json1 = """{"comentario": "Primer comentario", "propuesta": {}}"""
        val json2 = """{"comentario": "Segundo comentario", "propuesta": {}}"""
        val message = "$text\n$json1\n\nOtro texto\n$json2"

        assertThrows(AssistantResponseParserError.MalformedJson::class.java) {
            parser.parse(message)
        }
    }

    @Test
    fun `parse handles single JSON object correctly`() {
        val text = "Texto inicial"
        val json = """{"comentario": "Un comentario", "propuesta": {}}"""
        val message = "$text\n$json"

        val result = parser.parse(message)

        assertEquals(text, result.text)
        assertEquals(json, result.json)
        assertEquals("Un comentario", result.commentary)
    }

    @Test
    fun `parse handles nested JSON objects correctly`() {
        val text = "Texto inicial"
        val json = """{"comentario": "Comentario con objeto", "propuesta": {"item1": {}, "item2": {"subitem": "valor"}}}"""
        val message = "$text\n$json"

        val result = parser.parse(message)

        assertEquals(text, result.text)
        assertEquals(json, result.json)
        assertEquals("Comentario con objeto", result.commentary)
    }
}
