/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.model.TaskPriority
import com.ibc.procrastinapp.R


@Composable
fun ShowUrgentRow(
    task: Task,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
    ) {
    // Parte derecha: ID, botón expandir y prioridad
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // Esto asegura que los elementos de la derecha se mantengan juntos
        horizontalArrangement = Arrangement.End,
        // Aplicamos el modificador recibido Y fillMaxWidth para que
        // los elementos se alineen a la derecha
        modifier = modifier.fillMaxWidth()

    ) {
        // DEBUG PURPOSES, mantener el ID
        Text(
            // El caso (new) es para la visualización del asistente (tareas nuevas)
            // La quito porque no quiero trabajar con task.id en el asistente
            text = if (task.id == 0L) stringResource(R.string.task_new_label)
                   else stringResource(R.string.task_id_suffix, task.id),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Icono para expandir/contraer subtareas
        if (task.subtasks.isNotEmpty()) {
            IconButton(
                onClick = onExpandClick,
                modifier = Modifier.size(32.dp), // o lo que necesites
                //contentPadding = PaddingValues(0.dp)

            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.tasklist_collapse_subtasks_cd) else stringResource(R.string.tasklist_expand_subtasks_cd),
                            modifier = Modifier.size(16.dp) // tamaño del icono
                )
            }
        }

        // Indicador de prioridad
        val priority = TaskPriority.fromLevel(task.priority)
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = priority.color(MaterialTheme.colorScheme),
            //contentColor = contentColorFor(priority.color(MaterialTheme.colorScheme)), // Determina automáticamente el mejor color para el contenido
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Text(
                text = stringResource(id = priority.labelResId).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
