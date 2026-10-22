package pt.isep.dssmv.mapaisep.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

import pt.isep.dssmv.mapaisep.config.FirebaseConfig;
import pt.isep.dssmv.mapaisep.network.HttpClient;
import pt.isep.dssmv.mapaisep.network.HttpResponse;

public class AuthService {

    // Um minuto de margem: não enviamos para o Firestore um token quase expirado.
    private static final long TOKEN_EXPIRY_MARGIN_MS = 60 * 1000L;

    // A sessão usada pelos serviços enquanto a aplicação está aberta.
    private static String currentUid;
    private static String currentIdToken;
    private static String currentRefreshToken;
    private static long currentIdTokenExpiresAt;

    private final HttpClient httpClient = new HttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface LoginCallback {
        void onSuccess(
                String uid,
                String idToken,
                String refreshToken,
                long idTokenExpiresAt
        );

        void onError(String errorCode);
    }

    // É usado pelos serviços que precisam de chamar o Firestore.
    public interface TokenCallback {
        void onSuccess(String idToken);

        void onError(String errorCode);
    }

    public void login(String studentNumber, String password, LoginCallback callback) {
        new Thread(() -> {
            try {
                String email = studentNumber.trim() + FirebaseConfig.EMAIL_DOMAIN;

                JSONObject request = new JSONObject();
                request.put("email", email);
                request.put("password", password);
                request.put("returnSecureToken", true);

                HttpResponse response = httpClient.enviarPedido(
                        FirebaseConfig.AUTH_URL,
                        "POST",
                        null,
                        request.toString()
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
                long expiresAt = calcularExpiracao(body.getString("expiresIn"));

                mainHandler.post(() -> {
                    atualizarSessaoEmMemoria(uid, idToken, refreshToken, expiresAt);
                    callback.onSuccess(uid, idToken, refreshToken, expiresAt);
                });
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError("NETWORK_ERROR"));
            } catch (JSONException | NumberFormatException e) {
                mainHandler.post(() -> callback.onError("INVALID_RESPONSE"));
            }
        }).start();
    }

    /**
     * Entrega um idToken que ainda pode ser usado. Se estiver próximo de expirar,
     * troca o refreshToken por um par novo de tokens no Firebase.
     */
    public void obterIdTokenValido(Context context, TokenCallback callback) {
        if (currentIdToken != null
                && System.currentTimeMillis() < currentIdTokenExpiresAt - TOKEN_EXPIRY_MARGIN_MS) {
            mainHandler.post(() -> callback.onSuccess(currentIdToken));
            return;
        }

        String refreshToken = currentRefreshToken;
        String uid = currentUid;

        if (refreshToken == null || uid == null) {
            mainHandler.post(() -> callback.onError("SESSION_EXPIRED"));
            return;
        }

        new Thread(() -> {
            try {
                // Este endpoint recebe formulário, não JSON.
                String corpo = "grant_type=refresh_token&refresh_token="
                        + URLEncoder.encode(refreshToken, "UTF-8");

                HttpResponse response = httpClient.enviarPedido(
                        FirebaseConfig.TOKEN_REFRESH_URL,
                        "POST",
                        null,
                        corpo,
                        "application/x-www-form-urlencoded"
                );

                if (!response.isSuccessful()) {
                    String errorCode = lerCodigoErro(response);
                    mainHandler.post(() -> callback.onError(errorCode));
                    return;
                }

                JSONObject body = new JSONObject(response.getBody());
                String idTokenNovo = body.getString("id_token");
                String refreshTokenNovo = body.getString("refresh_token");
                long expiresAtNovo = calcularExpiracao(body.getString("expires_in"));

                mainHandler.post(() -> {
                    atualizarSessaoEmMemoria(
                            uid,
                            idTokenNovo,
                            refreshTokenNovo,
                            expiresAtNovo
                    );
                    SessionManager.atualizarTokens(
                            context,
                            idTokenNovo,
                            refreshTokenNovo,
                            expiresAtNovo
                    );
                    callback.onSuccess(idTokenNovo);
                });
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError("NETWORK_ERROR"));
            } catch (JSONException | NumberFormatException e) {
                mainHandler.post(() -> callback.onError("INVALID_RESPONSE"));
            }
        }).start();
    }

    private long calcularExpiracao(String segundos) {
        return System.currentTimeMillis() + Long.parseLong(segundos) * 1000L;
    }

    private void atualizarSessaoEmMemoria(
            String uid,
            String idToken,
            String refreshToken,
            long idTokenExpiresAt
    ) {
        currentUid = uid;
        currentIdToken = idToken;
        currentRefreshToken = refreshToken;
        currentIdTokenExpiresAt = idTokenExpiresAt;
    }

    private String lerCodigoErro(HttpResponse response) {
        try {
            JSONObject body = new JSONObject(response.getBody());
            return body.getJSONObject("error").getString("message");
        } catch (JSONException e) {
            return "HTTP_" + response.getStatusCode();
        }
    }

    public static String getCurrentUid() {
        return currentUid;
    }
    //O getIdToken é static

    // Usado apenas para verificar se existe uma sessão na memória.
    // Os pedidos ao Firestore devem usar obterIdTokenValido().
    public static String getIdToken() {
        return currentIdToken;
    }

    /** Repõe na memória a sessão guardada por menos de dois minutos. */
    public static boolean restaurarSessao(Context context) {
        String uid = SessionManager.obterUid(context);
        String idToken = SessionManager.obterIdToken(context);
        String refreshToken = SessionManager.obterRefreshToken(context);
        long expiresAt = SessionManager.obterExpiracaoIdToken(context);

        if (uid == null || idToken == null || refreshToken == null || expiresAt <= 0L) {
            return false;
        }

        currentUid = uid;
        currentIdToken = idToken;
        currentRefreshToken = refreshToken;
        currentIdTokenExpiresAt = expiresAt;
        return true;
    }

    // Termina a sessão local, sem revogar tokens já emitidos pelo Firebase.
    public static void logout() {
        currentUid = null;
        currentIdToken = null;
        currentRefreshToken = null;
        currentIdTokenExpiresAt = 0L;
    }
}
