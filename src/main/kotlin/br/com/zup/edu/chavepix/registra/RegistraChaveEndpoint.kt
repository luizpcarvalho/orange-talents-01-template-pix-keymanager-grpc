package br.com.zup.edu.chavepix.registra

import br.com.zup.edu.KeyManagerRegistraServiceGrpc
import br.com.zup.edu.RegistrarChavePixRequest
import br.com.zup.edu.RegistrarChavePixResponse
import br.com.zup.edu.config.handler.ErrorHandler
import br.com.zup.edu.util.toModel
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RegistraChaveEndpoint(@Inject private val service: NovaChavePixService)
    : KeyManagerRegistraServiceGrpc.KeyManagerRegistraServiceImplBase() {

    override fun registra(request: RegistrarChavePixRequest,
                          responseObserver: StreamObserver<RegistrarChavePixResponse>) {

        val novaChave = request.toModel()
        val chaveCriada = service.registra(novaChave)

        val response = RegistrarChavePixResponse.newBuilder()
            .setClienteId(chaveCriada.clienteId.toString())
            .setPixId(chaveCriada.id.toString())
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()

    }

}