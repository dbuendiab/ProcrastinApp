package com.ibc.procrastinapp.data.service

/**
 * Representa los errores específicos que pueden ocurrir durante el parseo
 * de la respuesta del asistente.
 */
sealed class AssistantResponseParserError(cause: Throwable? = null) : Exception(cause) {

    /**
     * Se encontraron múltiples bloques JSON en la respuesta, cuando solo se esperaba uno.
     * @property count El número de bloques JSON encontrados.
     */
    data class MultipleJsonObjectsFound(val count: Int) : AssistantResponseParserError()

    /**
     * El bloque JSON de la respuesta está malformado y no se pudo parsear.
     * @param cause La excepción original que provocó el fallo de parseo.
     */
    data class MalformedJson(override val cause: Throwable) : AssistantResponseParserError(cause)
}
