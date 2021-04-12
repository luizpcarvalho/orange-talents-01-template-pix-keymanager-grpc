package br.com.zup.edu.chavepix.registra

import br.com.zup.edu.KeyManagerRegistraServiceGrpc
import br.com.zup.edu.RegistrarChavePixRequest
import br.com.zup.edu.TipoChave
import br.com.zup.edu.TipoConta
import br.com.zup.edu.bcb.*
import br.com.zup.edu.chavepix.ChavePix
import br.com.zup.edu.chavepix.ChavePixRepository
import br.com.zup.edu.chavepix.ContaAssociada
import br.com.zup.edu.itau.DadosDaContaResponse
import br.com.zup.edu.itau.ERPItauClient
import br.com.zup.edu.itau.InstituicaoResponse
import br.com.zup.edu.itau.TitularResponse
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
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

/**
 * TIP: Necessário desabilitar o controle transactional (transactional = false) pois o gRPC Server
 * roda numa thread separada, caso contrário não será possível preparar cenário dentro do método @Test
 * */

@MicronautTest(transactional = false)
internal class RegistraChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerRegistraServiceGrpc.KeyManagerRegistraServiceBlockingStub
) {

    @Inject
    lateinit var itauClient: ERPItauClient

    @Inject
    lateinit var bcbClient: BancoCentralClient

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `deve registrar nova chave pix`() {
        // cenário
        `when`(itauClient.buscaContaPorTipoConta(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.create(dadosCreatePixKeyRequest()))
            .thenReturn(HttpResponse.created(dadosCreatePixKeyResponse()))

        // ação
        val response = grpcClient.registra(RegistrarChavePixRequest.newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoChave(TipoChave.EMAIL)
            .setValorChave("luiz@gmail.com")
            .setTipoConta(TipoConta.CONTA_CORRENTE)
            .build())

        // validação
        with(response) {
            assertEquals(CLIENTE_ID.toString(), clienteId)
            assertNotNull(pixId)
        }
    }

    @Test
    fun `nao deve registrar chave pix quando chave existente`() {
        // cenário
        repository.save(ChavePix(
            tipoChave = br.com.zup.edu.chavepix.TipoChave.CPF,
            valorChave = "54003212088",
            clienteId = CLIENTE_ID,
            tipoConta = br.com.zup.edu.chavepix.TipoConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                ContaAssociada.ITAU_UNIBANCO_ISPB,
                "Luiz Carvalho",
                "54003212088",
                "1218",
                "291900"
            )
        ))

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.registra(RegistrarChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.CPF)
                .setValorChave("54003212088")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.ALREADY_EXISTS.code, this.status.code)
            assertEquals("Chave Pix 54003212088 existente", this.status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix quando nao encontrar dados da conta cliente`() {
        // cenário
        `when`(itauClient.buscaContaPorTipoConta(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.notFound())

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.registra(RegistrarChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.EMAIL)
                .setValorChave("luiz@gmail.com")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.FAILED_PRECONDITION.code, this.status.code)
            assertEquals("Cliente não encontrado no Itau", this.status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix quando parametros forem invalidos`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.registra(RegistrarChavePixRequest.newBuilder().build())
        }
        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
            assertNotNull(this.status.description)
            // extrair e validar os detalhes do erro (violations)
            val violations = violations(this)
            assertThat(violations, containsString("Não é um formato válido de UUID"))
            assertThat(violations, containsString("não deve estar em branco"))
            assertThat(violations, containsString("não deve ser nulo"))
            assertThat(violations, containsString("Chave Pix inválida."))
        }
    }

    @MockBean(ERPItauClient::class)
    fun itauClient(): ERPItauClient? {
        return mock(ERPItauClient::class.java)
    }

    @MockBean(BancoCentralClient::class)
    fun bcbClient(): BancoCentralClient? {
        return mock(BancoCentralClient::class.java)
    }

    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel)
        : KeyManagerRegistraServiceGrpc.KeyManagerRegistraServiceBlockingStub {
            return KeyManagerRegistraServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun dadosCreatePixKeyRequest(): CreatePixKeyRequest {
        return CreatePixKeyRequest(
            keyType = PixKeyType.EMAIL,
            key = "luiz@gmail.com",
            bankAccount = BankAccount(
                participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
                branch = "1218",
                accountNumber = "291900",
                accountType = BankAccount.AccountType.CACC
            ),
            owner = Owner(
                type = Owner.OwnerType.NATURAL_PERSON,
                name = "Luiz Carvalho",
                taxIdNumber = "54003212088"
            )
        )
    }

    private fun dadosCreatePixKeyResponse(): CreatePixKeyResponse {
        return CreatePixKeyResponse(
            keyType = PixKeyType.EMAIL,
            key = "luiz@gmail.com",
            bankAccount = BankAccount(
                participant = ContaAssociada.ITAU_UNIBANCO_ISPB,
                branch = "1218",
                accountNumber = "291900",
                accountType = BankAccount.AccountType.CACC
            ),
            owner = Owner(
                type = Owner.OwnerType.NATURAL_PERSON,
                name = "Luiz Carvalho",
                taxIdNumber = "54003212088"
            ),
            createdAt = LocalDateTime.now()
        )
    }

    private fun dadosDaContaResponse(): DadosDaContaResponse {
        return DadosDaContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = InstituicaoResponse("UNIBANCO ITAU SA", ContaAssociada.ITAU_UNIBANCO_ISPB),
            agencia = "1218",
            numero = "291900",
            titular = TitularResponse("Luiz Carvalho", "54003212088")
        )
    }

    // extrai os detalhes de dentro do erro
    fun violations(e: StatusRuntimeException): String? {
        val details = StatusProto.fromThrowable(e)?.allFields?.values
        return details?.toString()
    }

}