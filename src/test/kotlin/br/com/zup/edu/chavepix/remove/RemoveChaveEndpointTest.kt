package br.com.zup.edu.chavepix.remove

import br.com.zup.edu.KeyManagerRemoveServiceGrpc
import br.com.zup.edu.RemoveChavePixRequest
import br.com.zup.edu.bcb.BancoCentralClient
import br.com.zup.edu.bcb.DeletePixKeyRequest
import br.com.zup.edu.bcb.DeletePixKeyResponse
import br.com.zup.edu.chavepix.*
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.*
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class RemoveChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerRemoveServiceGrpc.KeyManagerRemoveServiceBlockingStub
) {

    lateinit var CHAVE_EXISTENTE: ChavePix

    @Inject
    lateinit var bcbClient: BancoCentralClient

    @BeforeEach
    fun setup() {
        CHAVE_EXISTENTE = repository.save(ChavePix(
            tipoChave = TipoChave.EMAIL,
            valorChave = "luiz@gmail.com",
            clienteId = UUID.randomUUID(),
            tipoConta = TipoConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                ContaAssociada.ITAU_UNIBANCO_ISPB,
                "Luiz Carvalho",
                "54003212088",
                "1218",
                "291900"
            )
        ))
    }

    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve remover chave pix existente`() {
        // cenário
        `when`(bcbClient.delete(key = CHAVE_EXISTENTE.valorChave, request = DeletePixKeyRequest(CHAVE_EXISTENTE.valorChave)))
            .thenReturn(HttpResponse.ok(DeletePixKeyResponse(
                key = "luiz@gmail.com",
                participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
                deletedAt = LocalDateTime.now()
            )))

        // ação
        val response = grpcClient.remove(RemoveChavePixRequest.newBuilder()
            .setPixId(CHAVE_EXISTENTE.id.toString())
            .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
            .build())

        // validação
        with(response) {
            assertEquals("Chave Pix ${CHAVE_EXISTENTE.valorChave} removida com sucesso!", this.resultado)
        }
    }

    @Test
    fun `nao deve remover chave pix quando chave inexistente`() {
        // cenário
        val pixIdNaoExistente = UUID.randomUUID().toString()

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.remove(RemoveChavePixRequest.newBuilder()
                .setPixId(pixIdNaoExistente)
                .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, this.status.code)
            assertEquals("Chave pix $pixIdNaoExistente inexistente ou não pertence ao cliente.", this.status.description)
        }
    }

    @Test
    fun `nao deve remover chave pix quando chave existente mas pertence a outro cliente`() {
        // cenário
        val outroClienteId = UUID.randomUUID().toString()

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.remove(RemoveChavePixRequest.newBuilder()
                .setPixId(CHAVE_EXISTENTE.id.toString())
                .setClienteId(outroClienteId)
                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, this.status.code)
            assertEquals("Chave pix ${CHAVE_EXISTENTE.id.toString()} inexistente ou não pertence ao cliente.",
                this.status.description)
        }
    }

    @Test
    fun `nao deve remover chave pix quando falhar em remover do Banco Central`() {
        // cenário
        `when`(bcbClient.delete(key = CHAVE_EXISTENTE.valorChave, request = DeletePixKeyRequest(CHAVE_EXISTENTE.valorChave)))
            .thenReturn(HttpResponse.badRequest())

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.remove(RemoveChavePixRequest.newBuilder()
                .setPixId(CHAVE_EXISTENTE.id.toString())
                .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.FAILED_PRECONDITION.code, this.status.code)
            assertEquals("Erro ao remover chave Pix no Banco Central do Brasil (BCB)", this.status.description)
        }
    }

    @MockBean(BancoCentralClient::class)
    fun bcbClient(): BancoCentralClient? {
        return mock(BancoCentralClient::class.java)
    }

    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel)
        : KeyManagerRemoveServiceGrpc.KeyManagerRemoveServiceBlockingStub {
            return KeyManagerRemoveServiceGrpc.newBlockingStub(channel)
        }
    }

}