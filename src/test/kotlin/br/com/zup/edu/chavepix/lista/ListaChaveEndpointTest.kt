package br.com.zup.edu.chavepix.lista

import br.com.zup.edu.KeyManagerListaServiceGrpc
import br.com.zup.edu.ListaChavePixRequest
import br.com.zup.edu.chavepix.*
import br.com.zup.edu.chavepix.consulta.ConsultaChaveEndpointTest
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

@MicronautTest(transactional = false)
internal class ListaChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerListaServiceGrpc.KeyManagerListaServiceBlockingStub
) {

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    /**
     * TIP: por padrão roda numa transação isolada
     * */
    @BeforeEach
    fun setup() {
        repository.save(chave(tipoChave = TipoChave.EMAIL, valorChave = "luiz@gmail.com", clienteId = CLIENTE_ID))
        repository.save(chave(tipoChave = TipoChave.ALEATORIA, valorChave = "randomkey-2", clienteId = UUID.randomUUID()))
        repository.save(chave(tipoChave = TipoChave.ALEATORIA, valorChave = "randomkey-3", clienteId = CLIENTE_ID))
    }

    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve listar todas as chaves do cliente`() {
        // cenário
        val clienteId = CLIENTE_ID.toString()

        // ação
        val response = grpcClient.lista(ListaChavePixRequest.newBuilder()
            .setClienteId(clienteId)
            .build())

        // validação
        with(response) {
            assertEquals(clienteId, response.clienteId)
            assertThat(response.chavesList, hasSize(2))
            assertThat(
                response.chavesList.map { Pair(it.tipoChave, it.valorChave) }.toList(),
                containsInAnyOrder(
                    Pair(br.com.zup.edu.TipoChave.ALEATORIA, "randomkey-3"),
                    Pair(br.com.zup.edu.TipoChave.EMAIL, "luiz@gmail.com")
                )
            )
        }
    }

    @Test
    fun `nao deve listar as chaves do cliente quando cliente nao possuir chaves`() {
        // cenário
        val clienteSemChaves = UUID.randomUUID().toString()

        // ação
        val response = grpcClient.lista(ListaChavePixRequest.newBuilder()
            .setClienteId(clienteSemChaves)
            .build())

        // validação
        assertEquals(0, response.chavesCount)
    }

    @Test
    fun `nao deve listar todas as chaves do cliente quando clienteId for invalido`() {
        // cenário
        val clienteIdInvalido = ""

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.lista(ListaChavePixRequest.newBuilder()
                .setClienteId(clienteIdInvalido)
                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
            assertNotNull(this.status.description)
            assertThat(violations(this), containsString("Não é um formato válido de UUID"))
            assertThat(violations(this), containsString("não deve estar em branco"))
        }
    }

    @Factory
    class CLients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel)
        : KeyManagerListaServiceGrpc.KeyManagerListaServiceBlockingStub? {
            return KeyManagerListaServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun chave(
        tipoChave: TipoChave,
        valorChave: String = UUID.randomUUID().toString(),
        clienteId: UUID = UUID.randomUUID()
    ): ChavePix {
        return ChavePix(
            clienteId = clienteId,
            tipoChave = tipoChave,
            valorChave = valorChave,
            tipoConta = TipoConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                instituicao = ContaAssociada.ITAU_UNIBANCO_ISPB,
                nomeDoTitular = "Luiz",
                cpfDoTitular = "54003212088",
                agencia = "1218",
                numeroDaConta = "291900"
            )
        )
    }

    // extrai os detalhes de dentro do erro
    fun violations(e: StatusRuntimeException): String? {
        val details = StatusProto.fromThrowable(e)?.allFields?.values
        return details?.toString()
    }

}