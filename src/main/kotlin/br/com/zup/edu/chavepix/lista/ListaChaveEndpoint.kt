package br.com.zup.edu.chavepix.lista

import br.com.zup.edu.KeyManagerListaServiceGrpc
import br.com.zup.edu.ListaChavePixRequest
import br.com.zup.edu.ListaChavePixResponse
import br.com.zup.edu.chavepix.ChavePixRepository
import br.com.zup.edu.config.handler.ErrorHandler
import br.com.zup.edu.util.toModel
import io.grpc.stub.StreamObserver
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.Validator

@ErrorHandler
@Singleton
class ListaChaveEndpoint(@Inject val repository: ChavePixRepository,
                         @Inject val validator: Validator)
    : KeyManagerListaServiceGrpc.KeyManagerListaServiceImplBase() {

    override fun lista(request: ListaChavePixRequest,
                       responseObserver: StreamObserver<ListaChavePixResponse>) {

        val clienteId = request.toModel(validator).clienteId.let {
            UUID.fromString(it)
        }

        val lista = repository.findAllByClienteId(clienteId)

        responseObserver.onNext(ListaChavePixResponseConverter().convert(lista, clienteId = clienteId.toString()))
        responseObserver.onCompleted()

    }

}
