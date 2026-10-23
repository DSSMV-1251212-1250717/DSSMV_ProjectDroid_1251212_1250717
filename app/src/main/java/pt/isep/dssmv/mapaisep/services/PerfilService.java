package pt.isep.dssmv.mapaisep.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import pt.isep.dssmv.mapaisep.config.FirebaseConfig;
import pt.isep.dssmv.mapaisep.network.HttpClient;
import pt.isep.dssmv.mapaisep.network.HttpResponse;

/** Obtém os dados do aluno autenticado para o ecrã de perfil. */
public class PerfilService {
    private final HttpClient httpClient = new HttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface PerfilCallback {
        void onResult(String numero, String turma, String erro);
    }

    public void obterPerfil(Context context, PerfilCallback callback) {
        String uid = AuthService.getCurrentUid();
        if (uid == null) {
            callback.onResult(null, null, "NOT_AUTHENTICATED");
            return;
        }

        new AuthService().obterIdTokenValido(context, new AuthService.TokenCallback() {
            @Override public void onSuccess(String idToken) {
                new Thread(() -> {
                    try {
                        HttpResponse resposta = httpClient.enviarPedido(
                                FirebaseConfig.FIRESTORE_URL + "/alunos/" + uid,
                                "GET", idToken, null);
                        if (!resposta.isSuccessful()) {
                            devolver(callback, null, null, "HTTP_" + resposta.getStatusCode());
                            return;
                        }
                        JSONObject campos = new JSONObject(resposta.getBody()).getJSONObject("fields");
                        String numero = lerValor(campos.getJSONObject("numero"));
                        String turma = lerValor(campos.getJSONObject("turmaId"));
                        devolver(callback, numero, turma, null);
                    } catch (Exception e) {
                        devolver(callback, null, null, "PROFILE_ERROR");
                    }
                }).start();
            }
            @Override public void onError(String erro) { devolver(callback, null, null, erro); }
        });
    }

    private String lerValor(JSONObject valor) {
        if (valor.has("stringValue")) return valor.optString("stringValue");
        return valor.optString("integerValue");
    }

    private void devolver(PerfilCallback callback, String numero, String turma, String erro) {
        mainHandler.post(() -> callback.onResult(numero, turma, erro));
    }
}
