package br.com.zup.edu.config.handler.handlers

import br.com.zup.edu.config.handler.ExceptionHandler
import br.com.zup.edu.config.handler.ExceptionHandler.StatusWithDetails
import br.com.zup.edu.config.handler.exception.ChavePixInexistenteException
import io.grpc.Status
import java.lang.IllegalStateException
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

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