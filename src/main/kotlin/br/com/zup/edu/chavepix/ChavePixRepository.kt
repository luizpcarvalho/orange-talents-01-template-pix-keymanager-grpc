package br.com.zup.edu.chavepix

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ChavePixRepository: JpaRepository<ChavePix, UUID> {
    fun existsByValorChave(chave: String): Boolean
    fun findByIdAndClienteId(pixId: UUID, clienteId: UUID): Optional<ChavePix>
    fun findByValorChave(chave: String): Optional<ChavePix>
    fun findAllByClienteId(clienteId: UUID): List<ChavePix>
}
