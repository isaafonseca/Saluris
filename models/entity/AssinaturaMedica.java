package br.com.saluris.domain.entity;

import java.time.OffsetDateTime;

import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Assinatura/decisão do Médico do Trabalho sobre um laudo ou exame: registra médico, CRM/UF, data e hora.
 * Usada para homologar ou para recusar (com justificativa obrigatória).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class AssinaturaMedica {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id")
    private Usuario medico;

    @Column(name = "crm_assinatura", length = 20)
    private String crmAssinatura;

    @Column(name = "crm_uf_assinatura", length = 2)
    private String crmUfAssinatura;

    @Column(name = "assinado_em")
    private OffsetDateTime assinadoEm;

    @Column(name = "justificativa_recusa", length = 1000)
    private String justificativaRecusa;

    public static AssinaturaMedica assinadaPor(Usuario medico, Long empresaDoRegistro) {
        validarMedico(medico, empresaDoRegistro);
        AssinaturaMedica a = new AssinaturaMedica();
        a.preencher(medico);
        return a;
    }

    public static AssinaturaMedica recusadaPor(Usuario medico, Long empresaDoRegistro, String justificativa) {
        validarMedico(medico, empresaDoRegistro);
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraNegocioException("A justificativa da recusa é obrigatória.");
        }
        AssinaturaMedica a = new AssinaturaMedica();
        a.preencher(medico);
        a.justificativaRecusa = justificativa.trim();
        return a;
    }

    private void preencher(Usuario medico) {
        this.medico = medico;
        this.crmAssinatura = medico.getCrm();
        this.crmUfAssinatura = medico.getCrmUf();
        this.assinadoEm = OffsetDateTime.now();
    }

    private static void validarMedico(Usuario medico, Long empresaDoRegistro) {
        if (medico == null || medico.getPerfil() != Perfil.MEDICO_TRABALHO) {
            throw new RegraNegocioException("Somente um Médico do Trabalho pode assinar ou recusar.");
        }
        if (!medico.isAtivo()) {
            throw new RegraNegocioException("O médico está inativo.");
        }
        if (medico.getCrm() == null || medico.getCrmUf() == null) {
            throw new RegraNegocioException("O médico não possui CRM cadastrado.");
        }
        if (empresaDoRegistro == null || !empresaDoRegistro.equals(medico.getEmpresaId())) {
            throw new RegraNegocioException("O médico não pertence à empresa do registro.");
        }
    }
}
