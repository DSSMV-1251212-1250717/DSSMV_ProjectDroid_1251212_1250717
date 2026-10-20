package pt.isep.dssmv.mapaisep.network;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

// Servidor HTTP local de teste: não usa credenciais reais nem contacta o Firebase.
public class HttpClientTest {
    private ServerSocket server;
    private ExecutorService executor;
    private String baseUrl;
    private String requestMethod;
    private String requestBody;
    private final Map<String, String> requestHeaders = new HashMap<>();
    private final HttpClient client = new HttpClient();

    @Before
    public void iniciarServidor() throws Exception {
        server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        server.setSoTimeout(3000);
        baseUrl = "http://127.0.0.1:" + server.getLocalPort();
        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void pararServidor() throws Exception {
        server.close();
        executor.shutdownNow();
    }

    @Test
    public void getEnviaTokenERecebeJson() throws Exception {
        HttpResponse response = executarPedido("GET", "test-token", null,
                200, "{\"nome\":\"João\"}", "");
        assertEquals("GET", requestMethod);
        assertEquals("Bearer test-token", requestHeaders.get("authorization"));
        assertTrue(response.isSuccessful());
        assertEquals("{\"nome\":\"João\"}", response.getBody());
    }

    @Test
    public void postEnviaJsonUtf8SemTokenEAceita201() throws Exception {
        HttpResponse response = executarPedido("POST", null, "{\"nome\":\"João\"}",
                201, "{}", "");
        assertEquals("POST", requestMethod);
        assertEquals("{\"nome\":\"João\"}", requestBody);
        assertEquals("application/json; charset=UTF-8", requestHeaders.get("content-type"));
        assertNull(requestHeaders.get("authorization"));
        assertTrue(response.isSuccessful());
        assertEquals(201, response.getStatusCode());
    }

    @Test
    public void preservaEstadoECorpoDeErro() throws Exception {
        HttpResponse response = executarPedido("GET", null, null,
                403, "{\"error\":\"PERMISSION_DENIED\"}", "");
        assertFalse(response.isSuccessful());
        assertEquals(403, response.getStatusCode());
        assertEquals("{\"error\":\"PERMISSION_DENIED\"}", response.getBody());
    }

    @Test
    public void aceitaRespostaSemCorpo() throws Exception {
        HttpResponse response = executarPedido("GET", null, null, 204, "", "");
        assertTrue(response.isSuccessful());
        assertEquals(204, response.getStatusCode());
        assertEquals("", response.getBody());
    }

    @Test
    public void naoSegueRedirecionamentosComCredenciais() throws Exception {
        HttpResponse response = executarPedido("GET", "test-token", null, 302, "",
                "Location: http://127.0.0.1:1/destino\r\n");
        assertEquals(302, response.getStatusCode());
        assertFalse(response.isSuccessful());
    }

    private HttpResponse executarPedido(String method, String token, String body,
                                       int status, String responseBody, String extraHeaders)
            throws Exception {
        Future<Void> task = executor.submit(() -> {
            try (Socket socket = server.accept()) {
                socket.setSoTimeout(3000);
                InputStream input = socket.getInputStream();
                requestMethod = lerLinha(input).split(" ")[0];
                String line = lerLinha(input);
                while (!line.isEmpty()) {
                    int separator = line.indexOf(':');
                    requestHeaders.put(line.substring(0, separator).toLowerCase(Locale.ROOT),
                            line.substring(separator + 1).trim());
                    line = lerLinha(input);
                }

                int length = 0;
                if (requestHeaders.containsKey("content-length")) {
                    length = Integer.parseInt(requestHeaders.get("content-length"));
                }
                ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();
                for (int i = 0; i < length; i++) {
                    int value = input.read();
                    if (value == -1) {
                        throw new IllegalStateException("Corpo do pedido incompleto");
                    }
                    requestBytes.write(value);
                }
                requestBody = requestBytes.toString(StandardCharsets.UTF_8.name());

                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                String headers = "HTTP/1.1 " + status + " Test\r\n"
                        + "Content-Type: application/json; charset=UTF-8\r\n"
                        + "Content-Length: " + bytes.length + "\r\n"
                        + extraHeaders + "Connection: close\r\n\r\n";
                OutputStream output = socket.getOutputStream();
                output.write(headers.getBytes(StandardCharsets.US_ASCII));
                output.write(bytes);
                output.flush();
            }
            return null;
        });

        HttpResponse response = client.enviarPedido(baseUrl + "/teste", method, token, body);
        task.get(5, TimeUnit.SECONDS);
        return response;
    }

    private String lerLinha(InputStream input) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int value;
        while ((value = input.read()) != -1 && value != '\n') {
            if (value != '\r') {
                bytes.write(value);
            }
        }
        return bytes.toString(StandardCharsets.US_ASCII.name());
    }
}
