/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements.tasksview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.ui.common.clipAutomaticText

/**
 * Card con los comentarios del asistente
 */
@Composable
fun CommentsCard(
    lastComment: String,
    lastAssistantText: String,
    modifier: Modifier = Modifier
) {

    // Estado para control de expansión de comentarios
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .padding(bottom = 8.dp)
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Fila superior con icono de expandir/contraer
            Row(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Título o indicador de contenido
                Text(
                    text = stringResource(R.string.comments_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                // Icono para expandir/contraer (solo visual)
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isExpanded) {
                // Si hay texto del asistente, mostrarlo primero
                if (lastAssistantText.isNotBlank() && lastAssistantText != lastComment) {
                    Text(
                        text = clipAutomaticText(lastAssistantText),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                // Si también hay comentario, añadir un separador
                if (lastComment.isNotBlank() && lastAssistantText.isNotBlank()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                            alpha = 0.2f
                        )
                    )
                }

                // Si hay comentario, mostrarlo
                if (lastComment.isNotBlank()) {
                    Text(
                        text = lastComment,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun CommentsCardPreview() {
    val lastComment = "Te paso una secuencia de tareas que te ayudará"
    val lastAssistantText = "Encantado de saludarte de nuevo"
    CommentsCard(lastComment, lastAssistantText)
}