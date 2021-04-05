package br.com.zup.edu.chavepix

import javax.persistence.Embeddable

@Embeddable
data class ContaAssociada(
    val instituicao: String,
    val nomeDoTitular: String,
    val cpfDoTitular: String,
    val agencia: String,
    val numeroDaConta: String
) {
    companion object {
        val ITAU_UNIBANCO_ISPB: String = "60701190"
    }
}
