package br.com.zup.edu.chavepix.registra

import br.com.zup.edu.bcb.BancoCentralClient
import br.com.zup.edu.bcb.CreatePixKeyRequest
import br.com.zup.edu.chavepix.ChavePix
import br.com.zup.edu.chavepix.ChavePixRepository
import br.com.zup.edu.config.handler.exception.ChavePixExistenteException
import br.com.zup.edu.itau.ERPItauClient
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class NovaChavePixService(@Inject val repository: ChavePixRepository,
                          @Inject val itauClient: ERPItauClient,
                          @Inject val bcbClient: BancoCentralClient) {

    private val logger = LoggerFactory.getLogger(RegistraChaveEndpoint::class.java)

    @Transactional
    fun registra(@Valid novaChave: NovaChavePix): ChavePix {

        logger.info("Registrando chave pix para requisição: $novaChave")

        // verifica se a chave já existe no sistema
        if(repository.existsByValorChave(novaChave.valorChave!!)) {
            throw ChavePixExistenteException("Chave Pix ${novaChave.valorChave} existente")
        }

        // busca dados da conta no ERP do Itau
        val response = itauClient.buscaContaPorTipoConta(novaChave.clienteId!!, novaChave.tipoConta!!.name)
        val conta = response.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no Itau")

        // grava no banco de dados
        val chave = novaChave.toModel(conta)
        repository.save(chave)

        // registra chave no BCB
        val bcbRequest = CreatePixKeyRequest.of(chave).also {
            logger.info("Registrando chave Pix no Banco Central do Brasil (BCB): $it")
        }
        val bcbResponse = bcbClient.create(bcbRequest)
        if(bcbResponse.status != HttpStatus.CREATED) {
            throw IllegalStateException("Erro ao registrar chave Pix no Banco Central do Brasil (BCB)")
        }

        // atualiza chave do domínio com chave gerado pelo BCB
        chave.atualiza(bcbResponse.body()!!.key)

        return chave
    }

}
