package br.com.zup.edu.chavepix.remove

import br.com.zup.edu.config.validacao.ValidUUID
import io.micronaut.core.annotation.Introspected
import io.micronaut.validation.Validated
import javax.validation.constraints.NotBlank

@Validated
@Introspected
class RemoveChavePix(
    @field:ValidUUID @field:NotBlank val pixId: String?,
    @field:ValidUUID @field:NotBlank val clienteId: String?
)