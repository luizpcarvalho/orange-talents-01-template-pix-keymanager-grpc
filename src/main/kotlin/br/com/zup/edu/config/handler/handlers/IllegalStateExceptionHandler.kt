package br.com.zup.edu.config.handler.handlers

import br.com.zup.edu.config.handler.ExceptionHandler
import br.com.zup.edu.config.handler.ExceptionHandler.StatusWithDetails
import io.grpc.Status
import javax.inject.Singleton

@Singleton
class IllegalStateExceptionHandler: ExceptionHandler<IllegalStateException> {

    override fun handle(e: IllegalStateException): StatusWithDetails {
        return StatusWithDetails(Status.FAILED_PRECONDITION
            .withDescription(e.message)
            .withCause(e))
    }

    override fun supports(e: Exception): Boolean {
        return e is IllegalStateException
    }

}