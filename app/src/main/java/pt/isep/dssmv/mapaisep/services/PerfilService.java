package pt.isep.dssmv.mapaisep.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import pt.isep.dssmv.mapaisep.config.FirebaseConfig;
import pt.isep.dssmv.mapaisep.network.HttpClient;
import pt.isep.dssmv.mapaisep.network.HttpResponse;

/** Obtém os dados do aluno autenticado para o ecrã de perfil. */
public class PerfilService {
    private final HttpClient httpClient = new HttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // fotoBase64 vem null quando o aluno ainda não escolheu nenhuma foto.
    public interface PerfilCallback {
        void onResult(String numero, String turma, String fotoBase64, String erro);
    }

    // erro = null quer dizer que a foto ficou guardada.
    public interface FotoCallback {
        void onResult(String erro);
    }

    public void obterPerfil(Context context, PerfilCallback callback) {
        String uid = AuthService.getCurrentUid();
        if (uid == null) {
            callback.onResult(null, null, null, "NOT_AUTHENTICATED");
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
                            devolver(callback, null, null, null, "HTTP_" + resposta.getStatusCode());
                            return;
                        }
                        JSONObject campos = new JSONObject(resposta.getBody()).getJSONObject("fields");
                        String numero = lerValor(campos.getJSONObject("numero"));
                        String turma = lerValor(campos.getJSONObject("turmaId"));

                        // A foto é opcional: só existe depois de o aluno a escolher.
                        String foto = null;
                        if (campos.has("fotoBase64")) {
                            foto = campos.getJSONObject("fotoBase64").getString("stringValue");
                        }

                        devolver(callback, numero, turma, foto, null);
                    } catch (Exception e) {
                        devolver(callback, null, null, null, "PROFILE_ERROR");
                    }
                }).start();
            }
            @Override public void onError(String erro) { devolver(callback, null, null, null, erro); }
        });
    }

    // Guarda a foto (já em texto Base64) no documento alunos/{uid}, campo fotoBase64.
    public void guardarFoto(Context context, String fotoBase64, FotoCallback callback) {
        String uid = AuthService.getCurrentUid();
        if (uid == null) {
            mainHandler.post(() -> callback.onResult("NOT_AUTHENTICATED"));
            return;
        }

        // 1. Pedir o token.
        new AuthService().obterIdTokenValido(context, new AuthService.TokenCallback() {
            @Override
            public void onSuccess(String idToken) {
                // O envio demora: corre numa thread secundária.
                new Thread(() -> {
                    try {
                        // 2. Corpo do pedido: só o campo fotoBase64.
                        JSONObject foto = new JSONObject();
                        foto.put("stringValue", fotoBase64);

                        JSONObject campos = new JSONObject();
                        campos.put("fotoBase64", foto);

                        JSONObject corpo = new JSONObject();
                        corpo.put("fields", campos);

                        // 3. PATCH com updateMask: muda SÓ o campo fotoBase64.
                        // Sem o updateMask, o Firestore substituía o documento inteiro
                        // e apagava o nome, o número e a turma do aluno.
                        String endereco = FirebaseConfig.FIRESTORE_URL + "/alunos/" + uid
                                + "?updateMask.fieldPaths=fotoBase64";

                        HttpResponse resposta = httpClient.enviarPedido(
                                endereco, "PATCH", idToken, corpo.toString());

                        // 4. Avisar o ecrã.
                        if (!resposta.isSuccessful()) {
                            String erro = "HTTP_" + resposta.getStatusCode();
                            mainHandler.post(() -> callback.onResult(erro));
                            return;
                        }
                        mainHandler.post(() -> callback.onResult(null));
                    } catch (IOException e) {
                        mainHandler.post(() -> callback.onResult("NETWORK_ERROR"));
                    } catch (JSONException e) {
                        mainHandler.post(() -> callback.onResult("INVALID_DATA"));
                    }
                }).start();
            }

            @Override
            public void onError(String erro) {
                callback.onResult(erro);
            }
        });
    }

    // fotoBase64 = null se o aluno não tiver foto ou se o pedido falhar.
    public interface FotoAlunoCallback {
        void onResult(String fotoBase64);
    }

    // Vai buscar a foto de qualquer aluno (por exemplo, um amigo): GET a alunos/{uid}.
    public void obterFoto(Context context, String uid, FotoAlunoCallback callback) {
        new AuthService().obterIdTokenValido(context, new AuthService.TokenCallback() {
            @Override
            public void onSuccess(String idToken) {
                new Thread(() -> {
                    String foto = null;
                    try {
                        HttpResponse resposta = httpClient.enviarPedido(
                                FirebaseConfig.FIRESTORE_URL + "/alunos/" + uid, "GET", idToken, null);
                        if (resposta.isSuccessful()) {
                            JSONObject campos = new JSONObject(resposta.getBody()).getJSONObject("fields");
                            if (campos.has("fotoBase64")) {
                                foto = campos.getJSONObject("fotoBase64").getString("stringValue");
                            }
                        }
                    } catch (IOException | JSONException e) {
                        // Sem foto: o cartão fica com o ícone por defeito.
                    }

                    // A lambda só aceita variáveis que não mudam depois; por isso copia-se.
                    String resultado = foto;
                    mainHandler.post(() -> callback.onResult(resultado));
                }).start();
            }

            @Override
            public void onError(String erro) {
                callback.onResult(null);
            }
        });
    }

    private String lerValor(JSONObject valor) {
        if (valor.has("stringValue")) return valor.optString("stringValue");
        return valor.optString("integerValue");
    }

    private void devolver(PerfilCallback callback, String numero, String turma, String foto, String erro) {
        mainHandler.post(() -> callback.onResult(numero, turma, foto, erro));
    }
}
