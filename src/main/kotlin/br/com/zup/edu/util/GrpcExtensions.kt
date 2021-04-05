package br.com.zup.edu.util

import br.com.zup.edu.ConsultaChavePixRequest
import br.com.zup.edu.ConsultaChavePixRequest.FiltroCase.*
import br.com.zup.edu.ListaChavePixRequest
import br.com.zup.edu.RegistrarChavePixRequest
import br.com.zup.edu.RemoveChavePixRequest
import br.com.zup.edu.TipoChave.CHAVE_DESCONHECIDA
import br.com.zup.edu.TipoConta.CONTA_DESCONHECIDA
import br.com.zup.edu.chavepix.TipoChave
import br.com.zup.edu.chavepix.TipoConta
import br.com.zup.edu.chavepix.consulta.Filtro
import br.com.zup.edu.chavepix.lista.ListaChaveRequest
import br.com.zup.edu.chavepix.registra.NovaChavePix
import br.com.zup.edu.chavepix.remove.RemoveChavePix
import javax.validation.ConstraintViolationException
import javax.validation.Validator

fun RegistrarChavePixRequest.toModel(): NovaChavePix {
    return NovaChavePix(
        clienteId = clienteId,
        tipoChave = when (tipoChave) {
            CHAVE_DESCONHECIDA -> null
            else -> TipoChave.valueOf(tipoChave.name)
        },
        valorChave = valorChave,
        tipoConta = when (tipoConta) {
            CONTA_DESCONHECIDA -> null
            else -> TipoConta.valueOf(tipoConta.name)
        }
    )
}

fun RemoveChavePixRequest.toModel(): RemoveChavePix {
    return RemoveChavePix(
        pixId = pixId,
        clienteId = clienteId
    )
}

fun ConsultaChavePixRequest.toModel(validator: Validator): Filtro {
    // filtrocase é um campo interno de ConsultaChavePixRequest
    val filtro = when(filtroCase) {
        PIXID -> pixId.let {
            Filtro.PorPixId(clienteId = it.clienteId, pixId = it.pixId)
        }
        CHAVE -> Filtro.PorChave(chave)
        FILTRO_NOT_SET -> Filtro.Invalido()
    }

    val violations = validator.validate(filtro)
    if(violations.isNotEmpty()) {
        throw ConstraintViolationException(violations)
    }

    return filtro
}

fun ListaChavePixRequest.toModel(validator: Validator): ListaChaveRequest {
    val request = ListaChaveRequest(clienteId)
    val violations = validator.validate(request)
    if(violations.isNotEmpty()) {
        throw ConstraintViolationException(violations)
    }
    return request
}