package br.com.zup.edu.chavepix.lista

import br.com.zup.edu.config.validacao.ValidUUID
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank

@Introspected
data class ListaChaveRequest(
    @field:NotBlank @field:ValidUUID val clienteId: String
)