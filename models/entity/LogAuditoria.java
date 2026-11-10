package br.com.saluris.domain.entity;

import java.time.OffsetDateTime;
import java.util.Map;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.com.saluris.tenant.TenantFilter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Log de auditoria (append-only). {@code empresaId} nulo para ações do Administrador na plataforma. */
@Entity
@Table(name = "logs_auditoria")
@Filter(name = TenantFilter.NAME, condition = TenantFilter.CONDITION)
@Getter
@Setter
@NoArgsConstructor
public class LogAuditoria extends BaseEntity {

    @Column(name = "empresa_id", updatable = false)
    private Long empresaId;

    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;

    /** Ex.: CRIAR_USUARIO, GERAR_RELATORIO, ASSINAR_LAUDO. */
    @Column(name = "acao", nullable = false, length = 50, updatable = false)
    private String acao;

    /** Ex.: usuarios, laudos. */
    @Column(name = "entidade", nullable = false, length = 50, updatable = false)
    private String entidade;

    @Column(name = "entidade_id", updatable = false)
    private Long entidadeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalhes", updatable = false)
    private Map<String, Object> detalhes;

    @Column(name = "ip_origem", length = 45, updatable = false)
    private String ipOrigem;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    protected void aoCriar() {
        if (criadoEm == null) {
            criadoEm = OffsetDateTime.now();
        }
    }
}
