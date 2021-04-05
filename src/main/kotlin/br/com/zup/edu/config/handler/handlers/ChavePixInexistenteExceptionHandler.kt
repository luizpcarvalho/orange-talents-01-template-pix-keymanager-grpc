package br.com.zup.edu.config.handler.handlers

import br.com.zup.edu.config.handler.ExceptionHandler
import br.com.zup.edu.config.handler.ExceptionHandler.StatusWithDetails
import br.com.zup.edu.config.handler.exception.ChavePixInexistenteException
import io.grpc.Status
import javax.inject.Singleton

@Singleton
class ChavePixInexistenteExceptionHandler: ExceptionHandler<ChavePixInexistenteException> {

    override fun handle(e: ChavePixInexistenteException): StatusWithDetails {
        return StatusWithDetails(Status.NOT_FOUND
            .withDescription(e.message)
            .withCause(e))
    }

    override fun supports(e: Exception): Boolean {
        return e is ChavePixInexistenteException
    }

}