package br.com.zup.edu.chavepix

import br.com.zup.edu.ListaChavePixResponse
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
@Table(uniqueConstraints = [UniqueConstraint(
    name = "uk_chave_pix",
    columnNames = ["valorChave"]
)])
class ChavePix(
    @field:NotNull
    @Column(nullable = false)
    val clienteId: UUID,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipoChave: TipoChave,

    @field:NotBlank
    @Column(unique = true, nullable = false)
    var valorChave: String,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipoConta: TipoConta,

    @field:Valid
    @Embedded
    val conta: ContaAssociada
) {
    @Id
    @GeneratedValue
    val id: UUID? = null

    @Column(nullable = false)
    val criadaEm: LocalDateTime = LocalDateTime.now()

    override fun toString(): String {
        return "Chave Pix (clienteId=$clienteId, tipoChave=$tipoChave, valorChave=$valorChave, tipoConta=$tipoConta"
    }

    fun atualiza(key: String) {
        this.valorChave = key
    }

    fun pertenceAo(clienteIdAsUuid: UUID?) = this.clienteId == clienteIdAsUuid

}
