package br.com.saluris.service;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.EnvioEmail;
import br.com.saluris.domain.entity.Notificacao;
import br.com.saluris.domain.enums.StatusEnvioEmail;
import br.com.saluris.repository.EnvioEmailRepository;

/**
 * Fila e despacho de e-mails com tolerância a falhas (Quadro 16 / RNF10).
 * <p>Em caso de falha no servidor SMTP, registra em log e reagenda para a próxima execução da rotina (08h00).
 * Se nenhum {@code SMTP_HOST} estiver configurado, opera em modo simulado registrando o envio no log.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final ZoneId FUSO_BR = ZoneId.of("America/Sao_Paulo");

    private final EnvioEmailRepository envioEmailRepository;
    private final JavaMailSender mailSender;
    private final String smtpHost;
    private final String smtpFrom;

    public EmailService(EnvioEmailRepository envioEmailRepository,
            JavaMailSender mailSender,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${spring.mail.username:nao-responda@saluris.com.br}") String smtpFrom) {
        this.envioEmailRepository = envioEmailRepository;
        this.mailSender = mailSender;
        this.smtpHost = smtpHost;
        this.smtpFrom = smtpFrom == null || smtpFrom.isBlank() ? "nao-responda@saluris.com.br" : smtpFrom;
    }

    @Transactional
    public EnvioEmail enfileirar(Notificacao notificacao, String destinatario, String assunto, String corpo,
            boolean sensivel) {
        EnvioEmail envio = new EnvioEmail();
        envio.setNotificacao(notificacao);
        envio.setDestinatario(destinatario);
        envio.setAssunto(assunto);
        envio.setCorpo(corpo);
        envio.setSensivel(sensivel);
        envio.setProximaTentativa(OffsetDateTime.now());
        EnvioEmail salvo = envioEmailRepository.save(envio);
        tentarEnviar(salvo);
        return salvo;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Transactional
    public int processarFilaPendente() {
        List<EnvioEmail> pendentes = envioEmailRepository.findByStatusAndProximaTentativaLessThanEqual(
                StatusEnvioEmail.PENDENTE, OffsetDateTime.now());
        int enviados = 0;
        for (EnvioEmail envio : pendentes) {
            if (tentarEnviar(envio)) {
                enviados++;
            }
        }
        return enviados;
    }

    private boolean tentarEnviar(EnvioEmail envio) {
        try {
            if (smtpHost == null || smtpHost.isBlank()) {
                log.info("[E-MAIL SIMULADO] Para={} | Assunto={}", envio.getDestinatario(), envio.getAssunto());
            } else {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(smtpFrom);
                msg.setTo(envio.getDestinatario());
                msg.setSubject(envio.getAssunto());
                msg.setText(envio.getCorpo());
                mailSender.send(msg);
                log.info("E-mail enviado para {} ({})", envio.getDestinatario(), envio.getAssunto());
            }
            envio.registrarEnvio();
            return true;
        } catch (Exception e) {
            OffsetDateTime proxima = proximoHorarioRotina();
            log.error("Falha ao enviar e-mail para '{}' (assunto '{}'). Reagendado para {}: {}",
                    envio.getDestinatario(), envio.getAssunto(), proxima, e.getMessage());
            envio.registrarFalha(e.getMessage(), proxima);
            return false;
        }
    }

    private static OffsetDateTime proximoHorarioRotina() {
        ZonedDateTime agora = ZonedDateTime.now(FUSO_BR);
        ZonedDateTime hojeAsOito = agora.with(LocalTime.of(8, 0, 0));
        ZonedDateTime proximo = agora.isBefore(hojeAsOito) ? hojeAsOito : hojeAsOito.plusDays(1);
        return proximo.toOffsetDateTime();
    }
}
