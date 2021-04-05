package br.com.zup.edu.chavepix.consulta

import br.com.zup.edu.ConsultaChavePixRequest
import br.com.zup.edu.ConsultaChavePixResponse
import br.com.zup.edu.KeyManagerConsultaServiceGrpc
import br.com.zup.edu.bcb.BancoCentralClient
import br.com.zup.edu.chavepix.ChavePixRepository
import br.com.zup.edu.config.handler.ErrorHandler
import br.com.zup.edu.util.toModel
import io.grpc.stub.StreamObserver
import io.micronaut.validation.Validated
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.Validator

@Validated
@ErrorHandler
@Singleton
class ConsultaChaveEndpoint(@Inject private val repository: ChavePixRepository,
                            @Inject private val bcbClient: BancoCentralClient,
                            @Inject private val validator: Validator)
    : KeyManagerConsultaServiceGrpc.KeyManagerConsultaServiceImplBase() {

    override fun consulta(
        request: ConsultaChavePixRequest,
        responseObserver: StreamObserver<ConsultaChavePixResponse>
    ) {

        val filtro = request.toModel(validator)
        val chaveInfo = filtro.filtra(repository = repository, bcbClient = bcbClient)

        responseObserver.onNext(ConsultaChavePixResponseConverter().convert(chaveInfo))
        responseObserver.onCompleted()

    }

}