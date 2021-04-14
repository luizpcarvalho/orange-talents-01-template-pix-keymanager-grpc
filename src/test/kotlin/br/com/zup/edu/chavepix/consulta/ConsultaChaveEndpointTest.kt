package br.com.zup.edu.chavepix.consulta

import br.com.zup.edu.ConsultaChavePixRequest
import br.com.zup.edu.KeyManagerConsultaServiceGrpc
import br.com.zup.edu.bcb.*
import br.com.zup.edu.chavepix.*
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.contains
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class ConsultaChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerConsultaServiceGrpc.KeyManagerConsultaServiceBlockingStub
) {

    @Inject
    lateinit var bcbClient: BancoCentralClient

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    /**
     * TIP: por padrão roda numa transação isolada
     * */
    @BeforeEach
    fun setup() {
        repository.save(chave(tipoChave = TipoChave.EMAIL, valorChave = "luiz@gmail.com", clienteId = CLIENTE_ID))
        repository.save(chave(tipoChave = TipoChave.CPF, valorChave = "54003212088", clienteId = UUID.randomUUID()))
        repository.save(chave(tipoChave = TipoChave.ALEATORIA, valorChave = "randomkey-3", clienteId = CLIENTE_ID))
        repository.save(chave(tipoChave = TipoChave.CELULAR, valorChave = "+5511999998888", clienteId = CLIENTE_ID))
    }

    /**
     * TIP: por padrão roda numa transação isolada
     * */
    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve consultar chave por pixId e clienteId`() {
        // cenário
        val chaveExistente = repository.findByValorChave("+5511999998888").get()

        // ação
        val response = grpcClient.consulta(ConsultaChavePixRequest.newBuilder()
            .setPixId(ConsultaChavePixRequest.FiltroPorPixId.newBuilder()
                .setPixId(chaveExistente.id.toString())
                .setClienteId(chaveExistente.clienteId.toString())
                .build())
            .build()
        )

        // validação
        with(response) {
            assertEquals(chaveExistente.id.toString(), this.pixId)
            assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipoChave.name, this.chave.tipo.name)
            assertEquals(chaveExistente.valorChave, this.chave.chave)
        }
    }

    @Test
    fun `nao deve consultar chave por pixId e clienteId quando filtro invalido`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(ConsultaChavePixRequest.newBuilder()
                .setPixId(ConsultaChavePixRequest.FiltroPorPixId.newBuilder()
                    .setPixId("")
                    .setClienteId("")
                    .build())
                .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
            assertNotNull(this.status.description)
            assertThat(violations(this), containsString("não deve estar em branco"))
            assertThat(violations(this), containsString("Não é um formato válido de UUID"))
        }
    }

    @Test
    fun `nao deve consultar chave por pixId e clienteId quando registro nao existir`() {
        // cenário
        val pixIdNaoExistente = UUID.randomUUID().toString()
        val clienteIdNaoExistente = UUID.randomUUID().toString()

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(ConsultaChavePixRequest.newBuilder()
                .setPixId(ConsultaChavePixRequest.FiltroPorPixId.newBuilder()
                    .setPixId(pixIdNaoExistente)
                    .setClienteId(clienteIdNaoExistente)
                    .build())
                .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, this.status.code)
            assertEquals("Chave Pix não encontrada", this.status.description)
        }
    }

    @Test
    fun `deve consultar chave por valor da chave quando registro existir localmente`() {
        // cenário
        val chaveExistente = repository.findByValorChave("luiz@gmail.com").get()

        // ação
        val response = grpcClient.consulta(ConsultaChavePixRequest.newBuilder()
            .setChave("luiz@gmail.com")
            .build())

        // validação
        with(response) {
            assertEquals(chaveExistente.id.toString(), this.pixId)
            assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipoChave.name, this.chave.tipo.name)
            assertEquals(chaveExistente.valorChave, this.chave.chave)
        }
    }

    @Test
    fun `deve consultar chave por valor da chave quando registro nao existir localmente mas existir no BCB`() {
        // cenário
        val bcbResponse = pixKeyDetailsResponse()
        `when`(bcbClient.findByKey(key = "user.from.another.bank@santander.com.br"))
            .thenReturn(HttpResponse.ok(pixKeyDetailsResponse()))

        // ação
        val response = grpcClient.consulta(ConsultaChavePixRequest.newBuilder()
            .setChave("user.from.another.bank@santander.com.br")
            .build())

        // validação
        with(response) {
            assertEquals("", this.pixId)
            assertEquals("", this.clienteId)
            assertEquals(bcbResponse.keyType.name, this.chave.tipo.name)
            assertEquals(bcbResponse.key, this.chave.chave)
        }
    }

    @Test
    fun `nao deve consultar chave por valor da chave quando registro nao existir localmente nem no BCB`() {
        // cenário
        `when`(bcbClient.findByKey(key = "not.existing.user@santander.com.br"))
            .thenReturn(HttpResponse.notFound())

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(ConsultaChavePixRequest.newBuilder()
                .setChave("not.existing.user@santander.com.br")
                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, this.status.code)
            assertEquals("Chave Pix não encontrada", this.status.description)
        }
    }

    @Test
    fun `nao deve consultar chave por valor da chave quando filtro invalido`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(ConsultaChavePixRequest.newBuilder().setChave("").build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
            assertNotNull(this.status.description)
            assertThat(violations(this), containsString("não deve estar em branco"))
        }
    }

    @Test
    fun `nao deve consultar chave quando filtro invalido`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(ConsultaChavePixRequest.newBuilder().build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
            assertEquals("Chave Pix inválida ou não informada", this.status.description)
        }
    }

    @MockBean(BancoCentralClient::class)
    fun bcbClient(): BancoCentralClient? {
        return Mockito.mock(BancoCentralClient::class.java)
    }

    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel)
        : KeyManagerConsultaServiceGrpc.KeyManagerConsultaServiceBlockingStub? {
            return KeyManagerConsultaServiceGrpc.newBlockingStub(channel)
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

    private fun pixKeyDetailsResponse(): PixKeyDetailsResponse {
        return PixKeyDetailsResponse(
            keyType = PixKeyType.EMAIL,
            key = "user.from.another.bank@santander.com.br",
            bankAccount = BankAccount(
                participant = "SANTANDER",
                branch = "1234",
                accountNumber = "654321",
                accountType = BankAccount.AccountType.CACC
            ),
            owner = Owner(
                type = Owner.OwnerType.NATURAL_PERSON,
                name = "John Smith",
                taxIdNumber = "45826781068"
            ),
            createdAt = LocalDateTime.now()
        )
    }

    // extrai os detalhes de dentro do erro
    fun violations(e: StatusRuntimeException): String? {
        val details = StatusProto.fromThrowable(e)?.allFields?.values
        return details?.toString()
    }

}