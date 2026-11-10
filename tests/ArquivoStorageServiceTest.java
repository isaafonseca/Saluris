package br.com.saluris.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import br.com.saluris.config.SalurisProperties;
import br.com.saluris.domain.entity.ArquivoPdf;
import br.com.saluris.exception.RegraNegocioException;

class ArquivoStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void deveAceitarPdfValidoERejeitarOutrosFormatos() {
        SalurisProperties props = new SalurisProperties(
                null, null, null, new SalurisProperties.Storage(tempDir.toString()), null);
        ArquivoStorageService service = new ArquivoStorageService(props);

        MockMultipartFile pdfValido = new MockMultipartFile(
                "arquivo", "pcmso-2026.pdf", "application/pdf",
                "%PDF-1.4 conteudo de teste".getBytes(StandardCharsets.US_ASCII));

        ArquivoPdf salvo = service.armazenarPdf(pdfValido, 1L);
        assertThat(salvo.getNome()).isEqualTo("pcmso-2026.pdf");
        assertThat(salvo.getSha256()).hasSize(64);
        assertThat(service.carregarPdf(salvo).bytes()).startsWith("%PDF-".getBytes(StandardCharsets.US_ASCII));

        MockMultipartFile naoPdf = new MockMultipartFile(
                "arquivo", "planilha.xlsx", "application/vnd.ms-excel",
                "conteudo".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> service.armazenarPdf(naoPdf, 1L))
                .isInstanceOf(RegraNegocioException.class);

        MockMultipartFile falsoPdf = new MockMultipartFile(
                "arquivo", "falso.pdf", "application/pdf",
                "NAO_E_PDF".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> service.armazenarPdf(falsoPdf, 1L))
                .isInstanceOf(RegraNegocioException.class);
    }
}
