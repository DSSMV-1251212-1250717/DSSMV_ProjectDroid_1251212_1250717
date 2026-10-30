package pt.isep.dssmv.mapaisep.exceptions;

import java.io.IOException;

// Lançada quando a app deixou de ter um token válido e o aluno tem de fazer login outra vez.
// Acontece em três casos:
// 1. não há sessão em memória (por exemplo, o Android fechou a app em segundo plano);
// 2. o Firebase respondeu 401 (não aceitou o token);
// 3. o Firebase recusou a renovação do token (o token dura 1 hora).
// A falta de internet NÃO é sessão expirada: continua a ser NETWORK_ERROR.
//
// É uma IOException porque nasce da resposta a um pedido ao servidor: assim, qualquer
// catch (IOException) já a apanha e a app não vai abaixo. A mensagem é sempre o CODIGO,
// que é o que os services mandam ao ecrã. Os ecrãs, ao recebê-lo, chamam
// MenuConta.sessaoExpirada(activity): mensagem "A sessão terminou…" e volta ao login.
public class SessaoExpiradaException extends IOException {

    // O único código de erro para "sessão expirada" em toda a app.
    public static final String CODIGO = "SESSAO_EXPIRADA";

    public SessaoExpiradaException() {
        super(CODIGO);
    }
}
