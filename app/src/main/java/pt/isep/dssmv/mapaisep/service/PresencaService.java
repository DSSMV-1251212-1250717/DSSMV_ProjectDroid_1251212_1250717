package pt.isep.dssmv.mapaisep.service;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;

import pt.isep.dssmv.mapaisep.helper.FirebaseConfig;
import pt.isep.dssmv.mapaisep.handler.NetworkHandler;
import pt.isep.dssmv.mapaisep.handler.HttpResponse;
import pt.isep.dssmv.mapaisep.handler.JsonHandler;
import pt.isep.dssmv.mapaisep.dto.PresencaDTO;
import pt.isep.dssmv.mapaisep.exceptions.SessaoExpiradaException;

// Regista no Firestore se o aluno está na biblioteca: documento presencas/{uid}.
// Estar presente = o documento existir. PATCH cria/renova, DELETE apaga.
public class PresencaService {

    private final NetworkHandler networkHandler = new NetworkHandler();
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
            mainHandler.post(() -> callback.onResult(SessaoExpiradaException.CODIGO));
            return;
        }

        // Thread secundária: a rede não pode bloquear a main thread.
        new Thread(() -> {
            try {
                String endereco = FirebaseConfig.FIRESTORE_URL + "/presencas/" + uid;

                // Corpo: DTO com a hora atual -> JSON (JsonHandler).
                PresencaDTO presenca = new PresencaDTO(Instant.now().toString());
                JSONObject corpo = JsonHandler.presencaDTO2Json(presenca);

                // Envia-o como texto JSON (toString = serialização).
                HttpResponse response = networkHandler.enviarPedido(endereco, "PATCH", idToken, corpo.toString());

                // Correu mal: entrega o erro na main thread.
                if (!response.isSuccessful()) {
                    String erro = "HTTP_" + response.getStatusCode();
                    mainHandler.post(() -> callback.onResult(erro));
                    return;
                }

                // Correu bem: entrega "sem erro" (null) na main thread.
                mainHandler.post(() -> callback.onResult(null));

            } catch (SessaoExpiradaException e) {
                mainHandler.post(() -> callback.onResult(SessaoExpiradaException.CODIGO));
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
            mainHandler.post(() -> callback.onResult(SessaoExpiradaException.CODIGO));
            return;
        }

        new Thread(() -> {
            try {
                String endereco = FirebaseConfig.FIRESTORE_URL + "/presencas/" + uid;

                // Apagar não leva corpo: o endereço já diz qual é o documento.
                HttpResponse response = networkHandler.enviarPedido(endereco, "DELETE", idToken, null);

                if (!response.isSuccessful()) {
                    String erro = "HTTP_" + response.getStatusCode();
                    mainHandler.post(() -> callback.onResult(erro));
                    return;
                }

                mainHandler.post(() -> callback.onResult(null));

            } catch (SessaoExpiradaException e) {
                mainHandler.post(() -> callback.onResult(SessaoExpiradaException.CODIGO));
            } catch (IOException e) {
                mainHandler.post(() -> callback.onResult("NETWORK_ERROR"));
            }
        }).start();
    }
}
