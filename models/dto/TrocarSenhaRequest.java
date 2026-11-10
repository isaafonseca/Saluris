package br.com.saluris.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequest(
        @NotBlank(message = "Informe a senha atual") @Size(max = 72) String senhaAtual,
        @NotBlank(message = "Informe a nova senha")
        @Size(min = 10, max = 72, message = "A nova senha deve ter entre 10 e 72 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "A nova senha deve conter letras e números")
        String novaSenha) {
}
