package br.com.zup.edu.chavepix.lista

import br.com.zup.edu.ListaChavePixResponse
import br.com.zup.edu.TipoChave
import br.com.zup.edu.TipoConta
import br.com.zup.edu.chavepix.ChavePix
import com.google.protobuf.Timestamp
import java.time.ZoneId

class ListaChavePixResponseConverter {

    fun convert(lista: List<ChavePix>, clienteId: String): ListaChavePixResponse {
        val listaChaves = lista.map {
            ListaChavePixResponse.ChavePixDetails.newBuilder()
                .setPixId(it.id.toString())
                .setClienteId(it.clienteId.toString())
                .setTipoChave(TipoChave.valueOf(it.tipoChave.name))
                .setValorChave(it.valorChave)
                .setTipoConta(TipoConta.valueOf(it.tipoConta.name))
                .setCriadaEm(it.criadaEm.let {
                    val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                    Timestamp.newBuilder()
                        .setSeconds(createdAt.epochSecond)
                        .setNanos(createdAt.nano)
                        .build()
                })
                .build()
        }
        return ListaChavePixResponse.newBuilder()
            .setClienteId(clienteId)
            .addAllChaves(listaChaves)
            .build()
    }

}