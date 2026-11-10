package br.com.saluris.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.saluris.domain.enums.StatusValidade;

class ValidadeCalculatorTest {

    private final LocalDate hoje = LocalDate.of(2026, 10, 18);

    @Test
    void deveClassificarStatusDeValidadeCorretamente() {
        assertThat(ValidadeCalculator.classificar(hoje.plusDays(31), hoje)).isEqualTo(StatusValidade.EM_DIA);
        assertThat(ValidadeCalculator.classificar(hoje.plusDays(30), hoje)).isEqualTo(StatusValidade.PROXIMO_VENCIMENTO);
        assertThat(ValidadeCalculator.classificar(hoje, hoje)).isEqualTo(StatusValidade.PROXIMO_VENCIMENTO);
        assertThat(ValidadeCalculator.classificar(hoje.minusDays(1), hoje)).isEqualTo(StatusValidade.VENCIDO);
    }

    @Test
    void deveRetornarMarcosDeAlertaExatos30_15_7EUrgenciaSemanal() {
        assertThat(ValidadeCalculator.marcoAlertaHoje(hoje.plusDays(30), hoje)).isEqualTo(30);
        assertThat(ValidadeCalculator.marcoAlertaHoje(hoje.plusDays(15), hoje)).isEqualTo(15);
        assertThat(ValidadeCalculator.marcoAlertaHoje(hoje.plusDays(7), hoje)).isEqualTo(7);
        assertThat(ValidadeCalculator.marcoAlertaHoje(hoje.plusDays(10), hoje)).isNull();
        assertThat(ValidadeCalculator.marcoAlertaHoje(hoje, hoje)).isNull();

        assertThat(ValidadeCalculator.marcoAlertaHoje(hoje.minusDays(1), hoje)).isEqualTo(0);
        assertThat(ValidadeCalculator.marcoAlertaHoje(hoje.minusDays(2), hoje)).isNull();
        assertThat(ValidadeCalculator.marcoAlertaHoje(hoje.minusDays(8), hoje)).isEqualTo(0);
    }
}
