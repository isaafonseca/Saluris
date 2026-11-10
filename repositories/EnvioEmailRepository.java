package br.com.saluris.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.EnvioEmail;
import br.com.saluris.domain.enums.StatusEnvioEmail;

public interface EnvioEmailRepository extends JpaRepository<EnvioEmail, Long> {

    /** E-mails prontos para (re)envio pela rotina agendada. */
    List<EnvioEmail> findByStatusAndProximaTentativaLessThanEqual(StatusEnvioEmail status, OffsetDateTime limite);
}
