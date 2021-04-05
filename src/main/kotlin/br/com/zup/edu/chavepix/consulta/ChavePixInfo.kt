package br.com.zup.edu.chavepix.consulta

import br.com.zup.edu.chavepix.ChavePix
import br.com.zup.edu.chavepix.ContaAssociada
import br.com.zup.edu.chavepix.TipoChave
import br.com.zup.edu.chavepix.TipoConta
import java.time.LocalDateTime
import java.util.*

data class ChavePixInfo(
    val pixId: UUID? = null,
    val clienteId: UUID? = null,
    val tipo: TipoChave,
    val chave: String,
    val tipoConta: TipoConta,
    val conta: ContaAssociada,
    val registradaEm: LocalDateTime = LocalDateTime.now()
) {

    companion object {
        fun of(chave: ChavePix): ChavePixInfo {
            return ChavePixInfo(
                pixId = chave.id,
                clienteId = chave.clienteId,
                tipo = chave.tipoChave,
                chave = chave.valorChave,
                tipoConta = chave.tipoConta,
                conta = chave.conta,
                registradaEm = chave.criadaEm
            )
        }
    }

}