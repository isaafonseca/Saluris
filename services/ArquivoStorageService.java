package br.com.saluris.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.com.saluris.config.SalurisProperties;
import br.com.saluris.domain.entity.ArquivoPdf;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.exception.RegraNegocioException;

/**
 * Armazenamento e validação de arquivos PDF (RN06: apenas PDF, máx. 20 MB, verificação de assinatura %PDF- e SHA-256).
 */
@Service
public class ArquivoStorageService {

    private static final long TAMANHO_MAXIMO_BYTES = 20L * 1024L * 1024L;
    private static final byte[] CABECALHO_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    public record PdfCarregado(String nomeOriginal, byte[] bytes) {
    }

    private final Path diretorioBase;

    public ArquivoStorageService(SalurisProperties props) {
        String dir = props.storage() != null ? props.storage().dir() : null;
        if (dir == null || dir.isBlank()) {
            this.diretorioBase = Path.of(System.getProperty("user.home"), ".saluris", "arquivos");
        } else {
            this.diretorioBase = Path.of(dir);
        }
    }

    public ArquivoPdf armazenarPdf(MultipartFile file, Long empresaId) {
        if (file == null || file.isEmpty()) {
            throw new RegraNegocioException("Selecione um arquivo em formato PDF.");
        }
        String nomeOriginal = sanitizarNome(file.getOriginalFilename());
        if (!nomeOriginal.toLowerCase().endsWith(".pdf")) {
            throw new RegraNegocioException("Somente arquivos em formato PDF (.pdf) são permitidos.");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()
                && !contentType.equalsIgnoreCase(ArquivoPdf.MIME_TYPE)
                && !contentType.equalsIgnoreCase("application/octet-stream")) {
            throw new RegraNegocioException("Tipo de arquivo inválido. Envie apenas documentos PDF.");
        }
        if (file.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new RegraNegocioException("O arquivo excede o tamanho máximo permitido de 20 MB.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível ler o arquivo enviado.");
        }
        if (bytes.length < CABECALHO_PDF.length || !comecaComPdf(bytes)) {
            throw new RegraNegocioException("O conteúdo do arquivo não corresponde a um documento PDF válido.");
        }
        return armazenarBytesPdf(nomeOriginal, bytes, empresaId);
    }

    public ArquivoPdf armazenarBytesPdf(String nomeOriginal, byte[] bytes, Long empresaId) {
        String sha256 = calcularSha256(bytes);
        String relativePath = "empresa/" + empresaId + "/" + UUID.randomUUID() + ".pdf";
        Path destino = diretorioBase.resolve(relativePath).normalize();
        if (!destino.startsWith(diretorioBase.normalize())) {
            throw new RegraNegocioException("Caminho de armazenamento inválido.");
        }

        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gravar arquivo PDF em disco.", e);
        }

        return ArquivoPdf.of(sanitizarNome(nomeOriginal), relativePath, bytes.length, sha256);
    }

    public PdfCarregado carregarPdf(ArquivoPdf arquivo) {
        if (arquivo == null || arquivo.getPath() == null) {
            throw new RecursoNaoEncontradoException("Arquivo não encontrado.");
        }
        Path caminho = diretorioBase.resolve(arquivo.getPath()).normalize();
        if (!caminho.startsWith(diretorioBase.normalize()) || !Files.exists(caminho)) {
            throw new RecursoNaoEncontradoException("Arquivo PDF não encontrado no armazenamento.");
        }
        try {
            return new PdfCarregado(arquivo.getNome(), Files.readAllBytes(caminho));
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler o arquivo PDF.", e);
        }
    }

    private static boolean comecaComPdf(byte[] bytes) {
        for (int i = 0; i < CABECALHO_PDF.length; i++) {
            if (bytes[i] != CABECALHO_PDF[i]) {
                return false;
            }
        }
        return true;
    }

    private static String sanitizarNome(String original) {
        if (original == null || original.isBlank()) {
            return "documento.pdf";
        }
        String base = Path.of(original).getFileName().toString().trim();
        base = base.replaceAll("[\\r\\n\"\\\\/]", "_");
        return base.isEmpty() ? "documento.pdf" : base;
    }

    private static String calcularSha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
