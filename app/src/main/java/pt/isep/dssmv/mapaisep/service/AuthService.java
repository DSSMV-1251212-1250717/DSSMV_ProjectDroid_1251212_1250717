package pt.isep.dssmv.mapaisep.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;

import java.io.IOException;
import java.net.URLEncoder;

import pt.isep.dssmv.mapaisep.dto.LoginDTO;
import pt.isep.dssmv.mapaisep.dto.SessaoDTO;
import pt.isep.dssmv.mapaisep.exceptions.SessaoExpiradaException;
import pt.isep.dssmv.mapaisep.handler.HttpResponse;
import pt.isep.dssmv.mapaisep.handler.JsonHandler;
import pt.isep.dssmv.mapaisep.handler.NetworkHandler;
import pt.isep.dssmv.mapaisep.helper.FirebaseConfig;
import pt.isep.dssmv.mapaisep.helper.SessaoHelper;

public class AuthService {

    // Um minuto de margem: não enviamos para o Firestore um token quase expirado.
    private static final long TOKEN_EXPIRY_MARGIN_MS = 60 * 1000L;

    // A sessão usada pelos serviços enquanto a aplicação está aberta.
    private static String currentUid;
    private static String currentIdToken;
    private static String currentRefreshToken;
    private static long currentIdTokenExpiresAt;

    private final NetworkHandler networkHandler = new NetworkHandler();
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

                // Model -> DTO -> JSON: o JsonHandler escreve o pedido de login.
                LoginDTO login = new LoginDTO(email, password);

                HttpResponse response = networkHandler.enviarPedido(
                        FirebaseConfig.AUTH_URL,
                        "POST",
                        null,
                        JsonHandler.loginDTO2Json(login).toString()
                );

                if (!response.isSuccessful()) {
                    String errorCode = lerCodigoErro(response);
                    mainHandler.post(() -> callback.onError(errorCode));
                    return;
                }

                // JSON -> SessaoDTO (JsonHandler).
                SessaoDTO sessao = JsonHandler.json2SessaoDTO(response.getBody());
                String uid = sessao.getUid();
                String idToken = sessao.getIdToken();
                String refreshToken = sessao.getRefreshToken();
                long expiresAt = calcularExpiracao(sessao.getExpiraEmSegundos());

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

        // Sem refreshToken não há como renovar: a sessão acabou.
        if (refreshToken == null || uid == null) {
            mainHandler.post(() -> callback.onError(SessaoExpiradaException.CODIGO));
            return;
        }

        new Thread(() -> {
            try {
                // Este endpoint recebe formulário, não JSON.
                String corpo = "grant_type=refresh_token&refresh_token="
                        + URLEncoder.encode(refreshToken, "UTF-8");

                HttpResponse response = networkHandler.enviarPedido(
                        FirebaseConfig.TOKEN_REFRESH_URL,
                        "POST",
                        null,
                        corpo,
                        "application/x-www-form-urlencoded"
                );

                // O Firebase recusou a renovação (por exemplo TOKEN_EXPIRED ou USER_DISABLED):
                // o aluno tem de fazer login outra vez.
                if (!response.isSuccessful()) {
                    throw new SessaoExpiradaException();
                }

                // JSON -> SessaoDTO (JsonHandler). A renovação usa nomes diferentes do login,
                // mas o JsonHandler trata das duas formas.
                SessaoDTO sessao = JsonHandler.json2SessaoDTO(response.getBody());
                String idTokenNovo = sessao.getIdToken();
                String refreshTokenNovo = sessao.getRefreshToken();
                long expiresAtNovo = calcularExpiracao(sessao.getExpiraEmSegundos());

                mainHandler.post(() -> {
                    atualizarSessaoEmMemoria(
                            uid,
                            idTokenNovo,
                            refreshTokenNovo,
                            expiresAtNovo
                    );
                    SessaoHelper.atualizarTokens(
                            context,
                            idTokenNovo,
                            refreshTokenNovo,
                            expiresAtNovo
                    );
                    callback.onSuccess(idTokenNovo);
                });
            } catch (SessaoExpiradaException e) {
                mainHandler.post(() -> callback.onError(SessaoExpiradaException.CODIGO));
            } catch (IOException e) {
                // Sem internet: a sessão continua válida, só não foi possível renovar agora.
                mainHandler.post(() -> callback.onError("NETWORK_ERROR"));
            } catch (JSONException | NumberFormatException e) {
                mainHandler.post(() -> callback.onError("INVALID_RESPONSE"));
            }
        }).start();
    }

    private long calcularExpiracao(long segundos) {
        return System.currentTimeMillis() + segundos * 1000L;
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

    // Código de erro do Firebase (por exemplo INVALID_LOGIN_CREDENTIALS) ou HTTP_x.
    private String lerCodigoErro(HttpResponse response) {
        String mensagem = JsonHandler.json2MensagemErro(response.getBody());
        if (mensagem == null) {
            return "HTTP_" + response.getStatusCode();
        }
        return mensagem;
    }

    public static String getCurrentUid() {
        return currentUid;
    }

    // Usado apenas para verificar se existe uma sessão na memória.
    // Os pedidos ao Firestore devem usar obterIdTokenValido().
    public static String getIdToken() {
        return currentIdToken;
    }

    /** Repõe na memória a sessão guardada por menos de dois minutos. */
    public static boolean restaurarSessao(Context context) {
        String uid = SessaoHelper.obterUid(context);
        String idToken = SessaoHelper.obterIdToken(context);
        String refreshToken = SessaoHelper.obterRefreshToken(context);
        long expiresAt = SessaoHelper.obterExpiracaoIdToken(context);

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
