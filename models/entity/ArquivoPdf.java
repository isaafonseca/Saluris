package br.com.saluris.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Metadados de um arquivo PDF armazenado (laudos, documentos SST e resultados de exames). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArquivoPdf {

    public static final String MIME_TYPE = "application/pdf";

    @Column(name = "arquivo_nome", length = 255)
    private String nome;

    @Column(name = "arquivo_path", length = 500)
    private String path;

    @Column(name = "arquivo_mime_type", length = 100)
    private String mimeType;

    @Column(name = "arquivo_tamanho")
    private Long tamanho;

    /** SHA-256 em hexadecimal (64 caracteres): integridade do arquivo assinado. */
    @Column(name = "arquivo_sha256", length = 64)
    private String sha256;

    public static ArquivoPdf of(String nome, String path, long tamanho, String sha256) {
        return new ArquivoPdf(nome, path, MIME_TYPE, tamanho, sha256);
    }
}
