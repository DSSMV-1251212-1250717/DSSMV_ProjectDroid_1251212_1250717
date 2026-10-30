package pt.isep.dssmv.mapaisep.handler;

// "Envelope" com a resposta do Firebase, criado pelo NetworkHandler no fim de cada pedido.
// Sempre que um service faz um pedido, recebe um HttpResponse.
// Guarda duas coisas: o código (200 = correu bem, 404 = não existe, 401 = sessão expirada,
// 403 = sem permissão) e o corpo (o JSON com os dados).
// Os services precisam do código para decidir o que fazer: por exemplo, 404 na lista
// de amigos quer dizer que o aluno ainda não tem amigos, e não que houve um erro.
// Não decide nada sozinho: só guarda a resposta.
public class HttpResponse {
    private final int statusCode;
    private final String body;

    public HttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
}
