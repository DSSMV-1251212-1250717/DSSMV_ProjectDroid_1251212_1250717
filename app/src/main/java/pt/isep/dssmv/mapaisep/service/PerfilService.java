package pt.isep.dssmv.mapaisep.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import pt.isep.dssmv.mapaisep.helper.FirebaseConfig;
import pt.isep.dssmv.mapaisep.dto.AlunoDTO;
import pt.isep.dssmv.mapaisep.exceptions.SessaoExpiradaException;
import pt.isep.dssmv.mapaisep.handler.JsonHandler;
import pt.isep.dssmv.mapaisep.handler.NetworkHandler;
import pt.isep.dssmv.mapaisep.handler.HttpResponse;

/** Obtém os dados do aluno autenticado para o ecrã de perfil. */
public class PerfilService {
    private final NetworkHandler networkHandler = new NetworkHandler();
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
            callback.onResult(null, null, null, SessaoExpiradaException.CODIGO);
            return;
        }

        new AuthService().obterIdTokenValido(context, new AuthService.TokenCallback() {
            @Override
            public void onSuccess(String idToken) {
                new Thread(() -> {
                    try {
                        HttpResponse resposta = networkHandler.enviarPedido(
                                FirebaseConfig.FIRESTORE_URL + "/alunos/" + uid,
                                "GET", idToken, null);
                        if (!resposta.isSuccessful()) {
                            mainHandler.post(() -> callback.onResult(null, null, null, "HTTP_" + resposta.getStatusCode()));
                            return;
                        }
                        // JSON -> AlunoDTO (JsonHandler). A foto é opcional (null se não existir).
                        AlunoDTO aluno = JsonHandler.json2AlunoDTO(new JSONObject(resposta.getBody()));
                        if (aluno.getNumero() == null || aluno.getTurmaId() == null) {
                            throw new JSONException("Perfil incompleto");
                        }

                        mainHandler.post(() -> callback.onResult(aluno.getNumero(), aluno.getTurmaId(), aluno.getFotoBase64(), null));
                    } catch (SessaoExpiradaException e) {
                        mainHandler.post(() -> callback.onResult(null, null, null, SessaoExpiradaException.CODIGO));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onResult(null, null, null, "PROFILE_ERROR"));
                    }
                }).start();
            }

            @Override
            public void onError(String erro) {
                callback.onResult(null, null, null, erro);
            }
        });
    }

    // Guarda a foto (já em texto Base64) no documento alunos/{uid}, campo fotoBase64.
    public void guardarFoto(Context context, String fotoBase64, FotoCallback callback) {
        String uid = AuthService.getCurrentUid();
        if (uid == null) {
            mainHandler.post(() -> callback.onResult(SessaoExpiradaException.CODIGO));
            return;
        }

        // 1. Pedir o token.
        new AuthService().obterIdTokenValido(context, new AuthService.TokenCallback() {
            @Override
            public void onSuccess(String idToken) {
                // O envio demora: corre numa thread secundária.
                new Thread(() -> {
                    try {
                        // 2. Corpo do pedido: DTO -> JSON (JsonHandler), só com o campo fotoBase64.
                        AlunoDTO dto = new AlunoDTO(uid, null, null, null, fotoBase64);
                        JSONObject corpo = JsonHandler.fotoDTO2Json(dto);

                        // 3. PATCH com updateMask: muda SÓ o campo fotoBase64.
                        // Sem o updateMask, o Firestore substituía o documento inteiro
                        // e apagava o nome, o número e a turma do aluno.
                        String endereco = FirebaseConfig.FIRESTORE_URL + "/alunos/" + uid
                                + "?updateMask.fieldPaths=fotoBase64";

                        HttpResponse resposta = networkHandler.enviarPedido(
                                endereco, "PATCH", idToken, corpo.toString());

                        // 4. Avisar o ecrã.
                        if (!resposta.isSuccessful()) {
                            String erro = "HTTP_" + resposta.getStatusCode();
                            mainHandler.post(() -> callback.onResult(erro));
                            return;
                        }
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
                        HttpResponse resposta = networkHandler.enviarPedido(
                                FirebaseConfig.FIRESTORE_URL + "/alunos/" + uid, "GET", idToken, null);
                        if (resposta.isSuccessful()) {
                            // JSON -> AlunoDTO (JsonHandler); só interessa a foto.
                            foto = JsonHandler.json2AlunoDTO(new JSONObject(resposta.getBody())).getFotoBase64();
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
}
