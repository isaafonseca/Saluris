package br.com.saluris.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentosBrTest {

    @Test
    void cpfValidoComESemMascara() {
        assertThat(DocumentosBr.cpfValido("52998224725")).isTrue();
        assertThat(DocumentosBr.cpfValido("529.982.247-25")).isTrue();
    }

    @Test
    void cpfInvalido() {
        assertThat(DocumentosBr.cpfValido("52998224726")).isFalse();   // dígito verificador errado
        assertThat(DocumentosBr.cpfValido("11111111111")).isFalse();   // todos iguais
        assertThat(DocumentosBr.cpfValido("123")).isFalse();
        assertThat(DocumentosBr.cpfValido(null)).isFalse();
    }

    @Test
    void cnpjValidoComESemMascara() {
        assertThat(DocumentosBr.cnpjValido("11222333000181")).isTrue();
        assertThat(DocumentosBr.cnpjValido("11.222.333/0001-81")).isTrue();
    }

    @Test
    void cnpjInvalido() {
        assertThat(DocumentosBr.cnpjValido("11222333000182")).isFalse();
        assertThat(DocumentosBr.cnpjValido("00000000000000")).isFalse();
        assertThat(DocumentosBr.cnpjValido("123")).isFalse();
        assertThat(DocumentosBr.cnpjValido(null)).isFalse();
    }

    @Test
    void somenteDigitosRemoveMascara() {
        assertThat(DocumentosBr.somenteDigitos("11.222.333/0001-81")).isEqualTo("11222333000181");
    }
}
