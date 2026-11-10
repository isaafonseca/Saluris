package br.com.saluris.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.Empresa;
import br.com.saluris.domain.enums.PorteEmpresa;
import br.com.saluris.exception.ConflitoException;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.repository.EmpresaRepository;
import br.com.saluris.shared.validation.Cnpj;
import br.com.saluris.shared.validation.DocumentosBr;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Service
public class EmpresaService {

    public record EmpresaForm(
            @NotBlank(message = "Informe o CNPJ") @Cnpj String cnpj,
            @NotBlank(message = "Informe a razão social") @Size(max = 200) String razaoSocial,
            @Size(max = 200) String nomeFantasia,
            @NotBlank(message = "Informe o setor de atuação") @Size(max = 150) String setorAtuacao,
            @NotNull(message = "Informe o porte da empresa") PorteEmpresa porte,
            @Size(max = 150) String contatoNome,
            @NotBlank(message = "Informe o e-mail de contato") @Email @Size(max = 255) String emailContato,
            @Size(max = 20) String telefone) {
    }

    public record EmpresaResponse(
            Long id,
            String cnpj,
            String razaoSocial,
            String nomeFantasia,
            String setorAtuacao,
            PorteEmpresa porte,
            String contatoNome,
            String emailContato,
            String telefone,
            boolean ativa) {
    }

    private final EmpresaRepository empresaRepository;
    private final AuditoriaService auditoriaService;

    public EmpresaService(EmpresaRepository empresaRepository, AuditoriaService auditoriaService) {
        this.empresaRepository = empresaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponse> listar() {
        return empresaRepository.findAll(Sort.by(Sort.Direction.ASC, "razaoSocial"))
                .stream()
                .map(EmpresaService::paraResposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmpresaResponse buscar(Long id) {
        return paraResposta(buscarEntidade(id));
    }

    @Transactional
    public EmpresaResponse criar(EmpresaForm form) {
        String cnpj = DocumentosBr.somenteDigitos(form.cnpj());
        if (empresaRepository.existsByCnpj(cnpj)) {
            throw new ConflitoException("Já existe uma empresa cadastrada com este CNPJ.");
        }
        Empresa e = new Empresa();
        preencher(e, form, cnpj);
        Empresa salva = empresaRepository.save(e);
        auditoriaService.registrar("CRIAR_EMPRESA", "empresas", salva.getId(), salva.getId(),
                Map.of("cnpj", cnpj, "razaoSocial", salva.getRazaoSocial()));
        return paraResposta(salva);
    }

    @Transactional
    public EmpresaResponse atualizar(Long id, EmpresaForm form) {
        Empresa e = buscarEntidade(id);
        String cnpj = DocumentosBr.somenteDigitos(form.cnpj());
        empresaRepository.findByCnpj(cnpj)
                .filter(outra -> !outra.getId().equals(id))
                .ifPresent(outra -> {
                    throw new ConflitoException("Já existe outra empresa cadastrada com este CNPJ.");
                });
        preencher(e, form, cnpj);
        auditoriaService.registrar("ATUALIZAR_EMPRESA", "empresas", e.getId(), e.getId(),
                Map.of("cnpj", cnpj, "razaoSocial", e.getRazaoSocial()));
        return paraResposta(e);
    }

    @Transactional
    public EmpresaResponse alterarAtiva(Long id, boolean ativa) {
        Empresa e = buscarEntidade(id);
        e.setAtiva(ativa);
        auditoriaService.registrar(ativa ? "ATIVAR_EMPRESA" : "DESATIVAR_EMPRESA", "empresas", e.getId(), e.getId(),
                Map.of("ativa", ativa));
        return paraResposta(e);
    }

    @Transactional(readOnly = true)
    public Empresa buscarEntidade(Long id) {
        List<Empresa> visiveis = empresaRepository.findAll();
        return visiveis.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada."));
    }

    private static void preencher(Empresa e, EmpresaForm form, String cnpj) {
        e.setCnpj(cnpj);
        e.setRazaoSocial(form.razaoSocial().trim());
        e.setNomeFantasia(form.nomeFantasia() != null && !form.nomeFantasia().isBlank() ? form.nomeFantasia().trim() : null);
        e.setSetorAtuacao(form.setorAtuacao().trim());
        e.setPorte(form.porte());
        e.setContatoNome(form.contatoNome() != null && !form.contatoNome().isBlank() ? form.contatoNome().trim() : null);
        e.setEmailContato(form.emailContato().trim().toLowerCase());
        e.setTelefone(form.telefone() != null && !form.telefone().isBlank() ? form.telefone().trim() : null);
    }

    public static EmpresaResponse paraResposta(Empresa e) {
        return new EmpresaResponse(
                e.getId(),
                e.getCnpj(),
                e.getRazaoSocial(),
                e.getNomeFantasia(),
                e.getSetorAtuacao(),
                e.getPorte(),
                e.getContatoNome(),
                e.getEmailContato(),
                e.getTelefone(),
                e.isAtiva());
    }
}
