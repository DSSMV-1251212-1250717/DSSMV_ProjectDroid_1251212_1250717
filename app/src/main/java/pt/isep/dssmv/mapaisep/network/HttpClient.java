package pt.isep.dssmv.mapaisep.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// Código HTTP partilhado pelo login e pelo horário, sem bibliotecas externas.
public class HttpClient {
    private static final int TIMEOUT_MS = 10000;

    // Este método espera pela resposta. O serviço deve chamá-lo numa thread secundária.
    // idToken e corpo podem ser null quando o pedido não precisa desses dados.
    public HttpResponse enviarPedido(String endereco, String metodo, String idToken, String corpo) throws IOException {
        return enviarPedido(
                endereco,
                metodo,
                idToken,
                corpo,
                "application/json; charset=UTF-8"
        );
    }

    // Permite enviar formulários quando uma API não recebe JSON.
    public HttpResponse enviarPedido(
            String endereco,
            String metodo,
            String idToken,
            String corpo,
            String contentType
    ) throws IOException {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(endereco);
            connection = (HttpURLConnection) url.openConnection();

            // O HttpURLConnection não aceita PATCH (lança ProtocolException).
            // Envia-se um POST com o cabeçalho X-HTTP-Method-Override: PATCH,
            // e o servidor da Google trata o pedido como um PATCH.
            if (metodo.equals("PATCH")) {
                connection.setRequestMethod("POST");
                connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            } else {
                connection.setRequestMethod(metodo);
            }

            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");

            // Um redirecionamento deve ser tratado pelo serviço, sem reenviar credenciais.
            connection.setInstanceFollowRedirects(false);

            if (idToken != null) {
                connection.setRequestProperty("Authorization", "Bearer " + idToken);
            }

            if (corpo != null) {
                connection.setRequestProperty("Content-Type", contentType);
                connection.setDoOutput(true);

                // Serialização: o texto JSON é convertido em bytes para ser enviado.
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(corpo.getBytes(StandardCharsets.UTF_8));
                }
            }

            int statusCode = connection.getResponseCode();
            InputStream stream;

            if (statusCode >= 200 && statusCode < 300) {
                stream = connection.getInputStream();
            } else {
                stream = connection.getErrorStream();
            }

            // Preserva também o corpo dos erros para o serviço conseguir interpretá-los.
            return new HttpResponse(statusCode, lerResposta(stream));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String lerResposta(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }

        StringBuilder resposta = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                resposta.append(linha);
            }
        }
        return resposta.toString();
    }
}
