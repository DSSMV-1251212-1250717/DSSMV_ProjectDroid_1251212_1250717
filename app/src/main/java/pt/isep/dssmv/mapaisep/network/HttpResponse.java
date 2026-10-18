package pt.isep.dssmv.mapaisep.network;

// Guarda a resposta HTTP, sem decidir o que ela significa para cada serviço.
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
