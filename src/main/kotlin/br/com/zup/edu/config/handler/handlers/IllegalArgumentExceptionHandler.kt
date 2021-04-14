package br.com.zup.edu.config.handler.handlers

import br.com.zup.edu.config.handler.ExceptionHandler
import br.com.zup.edu.config.handler.ExceptionHandler.StatusWithDetails
import br.com.zup.edu.config.handler.exception.ChavePixInexistenteException
import io.grpc.Status
import java.lang.IllegalArgumentException
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
class IllegalArgumentExceptionHandler: ExceptionHandler<IllegalArgumentException> {

    override fun handle(e: IllegalArgumentException): StatusWithDetails {
        return StatusWithDetails(Status.INVALID_ARGUMENT
            .withDescription(e.message)
            .withCause(e))
    }

    override fun supports(e: Exception): Boolean {
        return e is IllegalArgumentException
    }

}