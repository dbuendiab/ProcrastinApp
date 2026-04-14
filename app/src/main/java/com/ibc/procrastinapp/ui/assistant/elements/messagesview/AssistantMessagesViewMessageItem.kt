/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements.messagesview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.ai.Message
import com.ibc.procrastinapp.ui.common.clipAutomaticText

/**
 * Componente que representa un mensaje individual en la conversación
 * @param message el mensaje que se va a mostrar
 */
@Composable
fun AssistantMessagesViewMessageItem(message: Message) {
    val isUser = message.isUser

    // Se trata de eliminar textos que se introduce de modo
    // repetitivo en cada mensaje, como "IMPORTANTE...
    // para condicionar la respuesta de ChatGPT o como
    // parte de su respuesta que es innecesario visualizar
    val messageClipped = clipAutomaticText(message.content)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Avatar para el asistente
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.avatar_assistant),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f, false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Burbuja de mensaje
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 12.dp else 0.dp,
                    topEnd = if (isUser) 0.dp else 12.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                ),
                color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = messageClipped,
                    modifier = Modifier.padding(12.dp),
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // Avatar para el usuario
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.avatar_user),
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

//--------------------------------------------------------------------
@Preview
@Composable
fun PreviewMessageItem() {
    Column {
        AssistantMessagesViewMessageItem(
            message = Message.Companion.assistantMessage(content = "Hola, ¿en qué puedo ayudarte?")
        )
        AssistantMessagesViewMessageItem(
            message = Message.Companion.userMessage(content = "Necesito planificar una tarea.")
        )
        AssistantMessagesViewMessageItem(
            message = Message.Companion.assistantMessage(content = "Te he entendido")
        )
    }
}