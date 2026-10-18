package pt.isep.dssmv.mapaisep.services;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import pt.isep.dssmv.mapaisep.config.FirebaseConfig;
import pt.isep.dssmv.mapaisep.network.HttpClient;
import pt.isep.dssmv.mapaisep.network.HttpResponse;

public class AuthService {
    // A sessão fica apenas na memória do processo. Se este terminar, é necessário novo login.
    // Estes campos são lidos e alterados na main thread.
    private static String currentUid;
    private static String currentIdToken;
    private static String currentRefreshToken;

    private final HttpClient httpClient = new HttpClient();

    // Este Handler coloca tarefas na fila da main thread.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // A Activity implementa estes métodos e passa o objeto ao serviço.
    // O serviço chama um deles quando sabe o resultado do pedido.
    public interface LoginCallback {
        void onSuccess(String uid, String idToken, String refreshToken);
        void onError(String errorCode);
    }

    public void login(String studentNumber, String password, LoginCallback callback) {
        // start() inicia a thread secundária. login() não espera pela resposta HTTP.
        new Thread(() -> {
            try {
                String email = studentNumber.trim() + FirebaseConfig.EMAIL_DOMAIN;

                JSONObject request = new JSONObject();
                request.put("email", email);
                request.put("password", password);
                request.put("returnSecureToken", true);

                // POST envia as credenciais no corpo JSON através de HTTPS.
                // Ainda não há ID token: é este pedido que o vai obter.
                HttpResponse response = httpClient.enviarPedido(
                        FirebaseConfig.AUTH_URL, "POST", null, request.toString()
                );

                if (!response.isSuccessful()) {
                    String errorCode = lerCodigoErro(response);
                    mainHandler.post(() -> callback.onError(errorCode));
                    return;
                }

                JSONObject body = new JSONObject(response.getBody());
                String uid = body.getString("localId");
                String idToken = body.getString("idToken");
                String refreshToken = body.getString("refreshToken");

                // A tarefa abaixo será executada na main thread, não nesta thread secundária.
                mainHandler.post(() -> {
                    currentUid = uid;
                    currentIdToken = idToken;
                    currentRefreshToken = refreshToken;
                    callback.onSuccess(uid, idToken, refreshToken);
                });
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError("NETWORK_ERROR"));
            } catch (JSONException e) {
                mainHandler.post(() -> callback.onError("INVALID_RESPONSE"));
            }
        }).start();
    }

    private String lerCodigoErro(HttpResponse response) {
        try {
            JSONObject body = new JSONObject(response.getBody());
            return body.getJSONObject("error").getString("message");
        } catch (JSONException e) {
            return "HTTP_" + response.getStatusCode();
        }
    }

    // O UID identifica o aluno; não dá permissão para aceder aos dados.
    public static String getCurrentUid() {
        return currentUid;
    }

    // O ID token é enviado como Bearer token nos pedidos ao Firestore.
    // A renovação automática com o refreshToken ainda não está implementada.
    public static String getIdToken() {
        return currentIdToken;
    }

    // Termina a sessão local, sem revogar tokens já emitidos pelo Firebase.
    public static void logout() {
        currentUid = null;
        currentIdToken = null;
        currentRefreshToken = null;
    }
}
