package br.com.saluris.exception;

/**
 * Recurso inexistente <b>ou</b> pertencente a outra empresa. As duas situações respondem igual (404)
 * para não revelar a existência de dados de outros tenants.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
