/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.ui.assistant.QuoteViewModel
import com.ibc.procrastinapp.ui.assistant.QuoteViewModel.QuoteError



@Composable
fun AquiNoHayNadaBox(
    modifier: Modifier = Modifier,
    quoteViewModel: QuoteViewModel? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp) // para dar algo de aire
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top, // 👉 alineado arriba
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 👉 Mostrar intro solo si QuoteViewModel existe y lo permite
            val showIntro = quoteViewModel?.shouldShowIntroText() ?: true
            if (showIntro) {

                Text(
                    text = stringResource(id = R.string.assistant_intro_text),
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // 👉 espacio flexible para empujar la cita hacia el centro
            Spacer(modifier = Modifier.weight(1f))

            // 👉 Mostrar frase motivadora o error si existe
            quoteViewModel?.let {
                val quote by it.currentQuote.collectAsStateWithLifecycle()
                val quoteError by it.quoteError.collectAsStateWithLifecycle()
                val displayText = when (quoteError) {
                    is QuoteError.LoadFailed -> stringResource(R.string.quote_error_load)
                    is QuoteError.FetchFailed -> stringResource(R.string.quote_error_fetch)
                    null -> quote
                }
                displayText?.let { phrase ->
                    Text(
                        text = phrase,
                        style = MaterialTheme.typography.headlineSmall,
                        color = primaryColor.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }
            }
        }
    }
}
