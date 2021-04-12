package br.com.zup.edu.config.handler.handlers

import br.com.zup.edu.config.handler.ExceptionHandler
import br.com.zup.edu.config.handler.ExceptionHandler.StatusWithDetails
import io.grpc.Status
import javax.inject.Singleton

@Singleton
class DefaultExceptionHandler: ExceptionHandler<Exception> {

    override fun handle(e: Exception): StatusWithDetails {
        return StatusWithDetails(Status.UNKNOWN
            .withDescription(e.message)
            .withCause(e))
    }

    override fun supports(e: Exception): Boolean {
        return e is UnknownError
    }

}