/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.service

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.utils.Logger

data class Propuesta(val tasks: List<Task>?)
data class ResultadoAI(val comentario: String?, val propuesta: Propuesta?)

class TaskJsonExtractor(
    private val gson: Gson = Gson()
) {
    /**
     * Recibe el JSON limpio extraído por AssistantResponseParser y devuelve la lista de tareas.
     */
    fun extractTasks(json: String): List<Task> {
        if (json.isBlank()) return emptyList()
        return try {
            val result = gson.fromJson(json, ResultadoAI::class.java)
            Logger.d("IBC-TaskJsonExtractor", "Resultado fromJson: $result")
            val rawTasks = result.propuesta?.tasks ?: return emptyList()
            val tasks = rawTasks.map { it.withNonNullStrings() }
            if (tasks.any { !isValid(it) }) return emptyList()
            else tasks
        } catch (e: JsonSyntaxException) {
            Logger.e("IBC-TaskJsonExtractor", "Error al analizar JSON: ${e.message}")
            emptyList()
        }
    }

    private fun isValid(task: Task): Boolean {
        if (task.title.isBlank()) return false
        return task.subtasks.all { isValid(it) }
    }
}
