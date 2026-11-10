package br.com.saluris.shared.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import br.com.saluris.domain.enums.StatusValidade;

/**
 * Cálculo de dias restantes, classificação de status de validade (RN08 / RF30) e marcos de alerta (30, 15, 7 e 0).
 */
public final class ValidadeCalculator {

    private ValidadeCalculator() {
    }

    public static long diasParaVencer(LocalDate dataVencimento, LocalDate hoje) {
        return ChronoUnit.DAYS.between(hoje, dataVencimento);
    }

    public static StatusValidade classificar(LocalDate dataVencimento, LocalDate hoje) {
        long dias = diasParaVencer(dataVencimento, hoje);
        if (dias < 0) {
            return StatusValidade.VENCIDO;
        }
        if (dias <= 30) {
            return StatusValidade.PROXIMO_VENCIMENTO;
        }
        return StatusValidade.EM_DIA;
    }

    /**
     * Retorna o marco de alerta aplicável hoje ({@code 30}, {@code 15}, {@code 7} para preventivo;
     * {@code 0} para urgência no 1º dia de atraso e a cada 7 dias subsequentes), ou {@code null} se não houver alerta hoje.
     */
    public static Integer marcoAlertaHoje(LocalDate dataVencimento, LocalDate hoje) {
        long dias = diasParaVencer(dataVencimento, hoje);
        if (dias == 30 || dias == 15 || dias == 7) {
            return (int) dias;
        }
        if (dias < 0) {
            long atraso = -dias;
            if ((atraso - 1) % 7 == 0) {
                return 0;
            }
        }
        return null;
    }
}
