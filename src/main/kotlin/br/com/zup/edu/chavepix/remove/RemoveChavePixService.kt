package br.com.zup.edu.chavepix.remove

import br.com.zup.edu.bcb.BancoCentralClient
import br.com.zup.edu.bcb.DeletePixKeyRequest
import br.com.zup.edu.chavepix.ChavePixRepository
import br.com.zup.edu.chavepix.registra.RegistraChaveEndpoint
import br.com.zup.edu.config.handler.exception.ChavePixInexistenteException
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class RemoveChavePixService(@Inject val repository: ChavePixRepository,
                            @Inject val bcbClient: BancoCentralClient) {

    private val logger = LoggerFactory.getLogger(RegistraChaveEndpoint::class.java)

    @Transactional
    fun remove(@Valid removeChavePix: RemoveChavePix): String {

        logger.info("Removendo chave pix ${removeChavePix.pixId}")

        val pixId = UUID.fromString(removeChavePix.pixId)
        val clienteId = UUID.fromString(removeChavePix.clienteId)

        val chave = repository.findByIdAndClienteId(pixId, clienteId)
            .orElseThrow {
                ChavePixInexistenteException("Chave pix ${removeChavePix.pixId} inexistente ou não pertence ao cliente.")
            }

        repository.deleteById(pixId)

        val request = DeletePixKeyRequest(chave.valorChave)

        val bcbResponse = bcbClient.delete(key = chave.valorChave, request = request)
        if(bcbResponse.status != HttpStatus.OK) {
            throw IllegalStateException("Erro ao remover chave Pix no Banco Central do Brasil (BCB)")
        }

        return "Chave Pix ${chave.valorChave} removida com sucesso!"

    }

}
