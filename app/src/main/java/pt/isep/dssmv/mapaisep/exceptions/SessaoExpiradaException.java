package pt.isep.dssmv.mapaisep.exceptions;

import java.io.IOException;

// Lançada quando o aluno tem de fazer login outra vez: não há sessão em memória,
// o Firebase respondeu 401, ou recusou renovar o token. A falta de internet não conta.
// É uma IOException, por isso qualquer catch (IOException) já a apanha.
public class SessaoExpiradaException extends IOException {

    // O único código de erro para "sessão expirada" em toda a app.
    public static final String CODIGO = "SESSAO_EXPIRADA";

    public SessaoExpiradaException() {
        super(CODIGO);
    }
}
