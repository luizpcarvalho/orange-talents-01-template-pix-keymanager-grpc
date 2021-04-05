package br.com.zup.edu.config.handler

import br.com.zup.edu.config.handler.handlers.DefaultExceptionHandler
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExceptionHandlerResolver(@Inject private val handlers: List<ExceptionHandler<Exception>>) {

    private val defaultHandler: ExceptionHandler<Exception> = DefaultExceptionHandler()

    fun resolve(e: Exception): ExceptionHandler<Exception> {
        val foundHandlers = handlers.filter { h -> h.supports(e) }
        return foundHandlers.firstOrNull() ?: defaultHandler
    }

}
