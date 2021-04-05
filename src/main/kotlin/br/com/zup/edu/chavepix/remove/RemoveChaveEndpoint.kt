package br.com.zup.edu.chavepix.remove

import br.com.zup.edu.KeyManagerRemoveServiceGrpc
import br.com.zup.edu.RemoveChavePixRequest
import br.com.zup.edu.RemoveChavePixResponse
import br.com.zup.edu.config.handler.ErrorHandler
import br.com.zup.edu.util.toModel
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RemoveChaveEndpoint(@Inject val service: RemoveChavePixService)
    : KeyManagerRemoveServiceGrpc.KeyManagerRemoveServiceImplBase() {

    override fun remove(request: RemoveChavePixRequest, responseObserver: StreamObserver<RemoveChavePixResponse>) {

        val removeChavePix = request.toModel()
        val resposta = service.remove(removeChavePix)

        responseObserver.onNext(RemoveChavePixResponse.newBuilder()
            .setResultado(resposta).build())
        responseObserver.onCompleted()

    }

}