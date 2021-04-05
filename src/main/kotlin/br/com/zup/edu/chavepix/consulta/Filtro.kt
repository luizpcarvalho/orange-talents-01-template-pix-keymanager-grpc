package br.com.zup.edu.chavepix.consulta

import br.com.zup.edu.bcb.BancoCentralClient
import br.com.zup.edu.chavepix.ChavePixRepository
import br.com.zup.edu.config.handler.exception.ChavePixInexistenteException
import br.com.zup.edu.config.validacao.ValidUUID
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpStatus
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Introspected
sealed class Filtro {

    val logger = LoggerFactory.getLogger(this::class.java)
    /**
     * Deve retornar chave encontrada ou lançar uma exceção de erro de chave não encontrada
     * */
    abstract fun filtra(repository: ChavePixRepository, bcbClient: BancoCentralClient): ChavePixInfo

    @Introspected
    data class PorPixId(
        @field:NotBlank @field:ValidUUID val clienteId: String,
        @field:NotBlank @field:ValidUUID val pixId: String
    ): Filtro() {

        fun pixIdAsUuid() = UUID.fromString(pixId)
        fun clienteIdAsUuid() = UUID.fromString(clienteId)

        override fun filtra(repository: ChavePixRepository, bcbClient: BancoCentralClient): ChavePixInfo {
            logger.info("Consultando chave Pix '${pixId}' no sistema interno")

            return repository.findById(pixIdAsUuid())
                .filter { it.pertenceAo(clienteIdAsUuid()) }
                .map(ChavePixInfo::of)
                .orElseThrow { ChavePixInexistenteException("Chave Pix não encontrada") }
        }
    }

    @Introspected
    data class PorChave(@field:NotBlank @field:Size(max = 77) val chave: String): Filtro() {

        override fun filtra(repository: ChavePixRepository, bcbClient: BancoCentralClient): ChavePixInfo {
            logger.info("Consultando chave Pix '${chave}' no sistema interno")

            return repository.findByValorChave(chave)
                .map(ChavePixInfo::of)
                .orElseGet{
                    logger.info("Consultando chave Pix '$chave' no Banco Central do Brasil (BCB)")

                    val response = bcbClient.findByKey(chave)
                    when (response.status) {
                        HttpStatus.OK -> response.body()?.toModel()
                        else -> throw ChavePixInexistenteException("Chave Pix não encontrada")
                    }
                }
        }

    }

    @Introspected
    class Invalido(): Filtro() {

        override fun filtra(repository: ChavePixRepository, bcbClient: BancoCentralClient): ChavePixInfo {
            throw IllegalArgumentException("Chave Pix inválida ou não informada")
        }

    }

}