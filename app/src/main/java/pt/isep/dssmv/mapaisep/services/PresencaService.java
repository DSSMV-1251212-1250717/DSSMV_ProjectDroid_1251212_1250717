package pt.isep.dssmv.mapaisep.services;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;

import pt.isep.dssmv.mapaisep.config.FirebaseConfig;
import pt.isep.dssmv.mapaisep.network.HttpClient;
import pt.isep.dssmv.mapaisep.network.HttpResponse;

// Regista no Firestore se o aluno está na biblioteca: documento presencas/{uid}.
// Estar presente = o documento existir. PATCH cria/renova, DELETE apaga.
public class PresencaService {

    private final HttpClient httpClient = new HttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Interface que quem chama preenche. erro é null se o pedido correu bem.
    public interface PresencaCallback {
        void onResult(String erro);
    }

    // PATCH presencas/{uid}: cria o documento se não existir, ou renova a hora se já existir.
    public void marcarPresenca(PresencaCallback callback) {
        // Sessão: uid (qual o documento) e token (prova de identidade).
        String uid = AuthService.getCurrentUid();
        String idToken = AuthService.getIdToken();

        if (uid == null || idToken == null) {
            mainHandler.post(() -> callback.onResult("NOT_AUTHENTICATED"));
            return;
        }

        // Thread secundária: a rede não pode bloquear a main thread.
        new Thread(() -> {
            try {
                String endereco = FirebaseConfig.FIRESTORE_URL + "/presencas/" + uid;

                // Monta o corpo como objeto Java:
                // { "fields": { "atualizadoEm": { "timestampValue": "2026-10-23T10:00:00Z" } } }
                JSONObject hora = new JSONObject().put("timestampValue", Instant.now().toString());
                JSONObject campos = new JSONObject().put("atualizadoEm", hora);
                JSONObject corpo = new JSONObject().put("fields", campos);

                // Envia-o como texto JSON (toString = serialização).
                HttpResponse response = httpClient.enviarPedido(endereco, "PATCH", idToken, corpo.toString());

                // Correu mal: entrega o erro na main thread.
                if (!response.isSuccessful()) {
                    String erro = "HTTP_" + response.getStatusCode();
                    mainHandler.post(() -> callback.onResult(erro));
                    return;
                }

                // Correu bem: entrega "sem erro" (null) na main thread.
                mainHandler.post(() -> callback.onResult(null));

            } catch (IOException e) {
                mainHandler.post(() -> callback.onResult("NETWORK_ERROR"));
            } catch (JSONException e) {
                mainHandler.post(() -> callback.onResult("INVALID_DATA"));
            }
        }).start();
    }

    // DELETE presencas/{uid}: apaga o documento, o aluno deixa de contar como presente.
    public void removerPresenca(PresencaCallback callback) {
        String uid = AuthService.getCurrentUid();
        String idToken = AuthService.getIdToken();

        if (uid == null || idToken == null) {
            mainHandler.post(() -> callback.onResult("NOT_AUTHENTICATED"));
            return;
        }

        new Thread(() -> {
            try {
                String endereco = FirebaseConfig.FIRESTORE_URL + "/presencas/" + uid;

                // Apagar não leva corpo: o endereço já diz qual é o documento.
                HttpResponse response = httpClient.enviarPedido(endereco, "DELETE", idToken, null);

                if (!response.isSuccessful()) {
                    String erro = "HTTP_" + response.getStatusCode();
                    mainHandler.post(() -> callback.onResult(erro));
                    return;
                }

                mainHandler.post(() -> callback.onResult(null));

            } catch (IOException e) {
                mainHandler.post(() -> callback.onResult("NETWORK_ERROR"));
            }
        }).start();
    }
}
