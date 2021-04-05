package br.com.zup.edu.chavepix.registra

import br.com.zup.edu.chavepix.ChavePix
import br.com.zup.edu.chavepix.TipoChave
import br.com.zup.edu.chavepix.TipoConta
import br.com.zup.edu.chavepix.ContaAssociada
import br.com.zup.edu.config.validacao.ValidPixKey
import br.com.zup.edu.config.validacao.ValidUUID
import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@ValidPixKey
@Introspected
data class NovaChavePix(
    @field:ValidUUID
    @field:NotBlank val clienteId: String?,
    @field:NotNull val tipoChave: TipoChave?,
    @field:Size(max = 77) val valorChave: String?,
    @field:NotNull val tipoConta: TipoConta?
) {
    fun toModel(conta: ContaAssociada): ChavePix {
        return ChavePix(
            clienteId = UUID.fromString(this.clienteId),
            tipoChave = TipoChave.valueOf(this.tipoChave!!.name),
            valorChave = if (this.tipoChave == TipoChave.ALEATORIA) UUID.randomUUID().toString() else this.valorChave!!,
            tipoConta = TipoConta.valueOf(this.tipoConta!!.name),
            conta = conta
        )
    }
}
