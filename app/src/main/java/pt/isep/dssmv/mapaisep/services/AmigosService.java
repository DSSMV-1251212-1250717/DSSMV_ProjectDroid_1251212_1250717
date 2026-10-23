package pt.isep.dssmv.mapaisep.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pt.isep.dssmv.mapaisep.config.FirebaseConfig;
import pt.isep.dssmv.mapaisep.dto.AmigoDTO;
import pt.isep.dssmv.mapaisep.mappers.AmigoMapper;
import pt.isep.dssmv.mapaisep.model.Amigo;
import pt.isep.dssmv.mapaisep.network.HttpClient;
import pt.isep.dssmv.mapaisep.network.HttpResponse;

/** Pedidos HTTP do caso de uso: listar, adicionar e remover amigos. */
public class AmigosService {

    private final HttpClient httpClient = new HttpClient();
    private final AmigoMapper amigoMapper = new AmigoMapper();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ListaCallback {
        void onResult(List<Amigo> amigos, String erro);
    }

    public interface OperacaoCallback {
        void onResult(String erro);
    }

    public void listarAmigos(Context context, ListaCallback callback) {
        obterToken(context, new TokenResult() {
            @Override
            public void onSuccess(String uid, String idToken) {
                new Thread(() -> {
                    try {
                        String endereco = FirebaseConfig.FIRESTORE_URL
                                + "/alunos/" + uid + "/amigos";

                        HttpResponse resposta = httpClient.enviarPedido(
                                endereco, "GET", idToken, null
                        );

                        if (resposta.getStatusCode() == 404) {
                            mainHandler.post(() -> callback.onResult(new ArrayList<>(), null));
                            return;
                        }

                        if (!resposta.isSuccessful()) {
                            devolverListaErro(callback, "HTTP_" + resposta.getStatusCode());
                            return;
                        }

                        JSONObject corpo = new JSONObject(resposta.getBody());
                        JSONArray documentos = corpo.optJSONArray("documents");
                        List<Amigo> amigos = new ArrayList<>();

                        if (documentos != null) {
                            for (int i = 0; i < documentos.length(); i++) {
                                AmigoDTO dto = amigoMapper.desserializarAmigo(
                                        documentos.getJSONObject(i)
                                );
                                amigos.add(amigoMapper.converterParaModelo(dto));
                            }
                        }

                        mainHandler.post(() -> callback.onResult(amigos, null));
                    } catch (IOException e) {
                        devolverListaErro(callback, "NETWORK_ERROR");
                    } catch (JSONException e) {
                        devolverListaErro(callback, "INVALID_FRIEND_DATA");
                    }
                }).start();
            }

            @Override
            public void onError(String erro) {
                callback.onResult(null, erro);
            }
        });
    }

    public void adicionarAmigo(
            Context context,
            String numeroProcurado,
            OperacaoCallback callback
    ) {
        if (!numeroProcurado.matches("[0-9]{7}")) {
            mainHandler.post(() -> callback.onResult("INVALID_STUDENT_NUMBER"));
            return;
        }

        obterToken(context, new TokenResult() {
            @Override
            public void onSuccess(String uidAtual, String idToken) {
                new Thread(() -> {
                    try {
                        AlunoDados alunoAtual = obterAluno(uidAtual, idToken);
                        AlunoDados alunoEncontrado = procurarAluno(numeroProcurado, idToken);

                        if (alunoEncontrado == null) {
                            devolverOperacaoErro(callback, "STUDENT_NOT_FOUND");
                            return;
                        }

                        if (uidAtual.equals(alunoEncontrado.uid)) {
                            devolverOperacaoErro(callback, "CANNOT_ADD_SELF");
                            return;
                        }

                        JSONObject pedido = criarPedidoAdicionar(
                                alunoAtual,
                                alunoEncontrado
                        );

                        enviarCommit(idToken, pedido);
                        mainHandler.post(() -> callback.onResult(null));
                    } catch (IOException e) {
                        devolverOperacaoErro(callback, e.getMessage());
                    } catch (JSONException e) {
                        devolverOperacaoErro(callback, "INVALID_FRIEND_DATA");
                    }
                }).start();
            }

            @Override
            public void onError(String erro) {
                callback.onResult(erro);
            }
        });
    }

    public void removerAmigo(Context context, Amigo amigo, OperacaoCallback callback) {
        obterToken(context, new TokenResult() {
            @Override
            public void onSuccess(String uidAtual, String idToken) {
                new Thread(() -> {
                    try {
                        JSONObject pedido = criarPedidoRemover(uidAtual, amigo.getUid());
                        enviarCommit(idToken, pedido);
                        mainHandler.post(() -> callback.onResult(null));
                    } catch (IOException e) {
                        devolverOperacaoErro(callback, e.getMessage());
                    } catch (JSONException e) {
                        devolverOperacaoErro(callback, "INVALID_FRIEND_DATA");
                    }
                }).start();
            }

            @Override
            public void onError(String erro) {
                callback.onResult(erro);
            }
        });
    }

    private void obterToken(Context context, TokenResult resultado) {
        String uid = AuthService.getCurrentUid();
        if (uid == null) {
            mainHandler.post(() -> resultado.onError("NOT_AUTHENTICATED"));
            return;
        }

        new AuthService().obterIdTokenValido(
                context,
                new AuthService.TokenCallback() {
                    @Override
                    public void onSuccess(String idToken) {
                        resultado.onSuccess(uid, idToken);
                    }

                    @Override
                    public void onError(String erro) {
                        resultado.onError(erro);
                    }
                }
        );
    }

    private AlunoDados obterAluno(String uid, String idToken) throws IOException, JSONException {
        HttpResponse resposta = httpClient.enviarPedido(
                FirebaseConfig.FIRESTORE_URL + "/alunos/" + uid,
                "GET",
                idToken,
                null
        );

        if (!resposta.isSuccessful()) {
            throw new IOException("HTTP_" + resposta.getStatusCode());
        }

        return lerAluno(new JSONObject(resposta.getBody()));
    }

    private AlunoDados procurarAluno(String numero, String idToken)
            throws IOException, JSONException {
        JSONObject filtro = new JSONObject();
        filtro.put("field", new JSONObject().put("fieldPath", "numero"));
        filtro.put("op", "EQUAL");
        filtro.put("value", new JSONObject().put("stringValue", numero));

        JSONObject query = new JSONObject();
        query.put("from", new JSONArray().put(
                new JSONObject().put("collectionId", "alunos")
        ));
        query.put("where", new JSONObject().put("fieldFilter", filtro));
        query.put("limit", 1);

        JSONObject pedido = new JSONObject().put("structuredQuery", query);
        HttpResponse resposta = httpClient.enviarPedido(
                FirebaseConfig.FIRESTORE_URL + ":runQuery",
                "POST",
                idToken,
                pedido.toString()
        );

        if (!resposta.isSuccessful()) {
            throw new IOException("HTTP_" + resposta.getStatusCode());
        }

        JSONArray resultados = new JSONArray(resposta.getBody());
        for (int i = 0; i < resultados.length(); i++) {
            JSONObject resultado = resultados.getJSONObject(i);
            if (resultado.has("document")) {
                return lerAluno(resultado.getJSONObject("document"));
            }
        }

        return null;
    }

    private AlunoDados lerAluno(JSONObject documento) throws JSONException {
        JSONObject campos = documento.getJSONObject("fields");
        String caminho = documento.getString("name");
        String uid = caminho.substring(caminho.lastIndexOf('/') + 1);

        return new AlunoDados(
                uid,
                campos.getJSONObject("nome").getString("stringValue"),
                campos.getJSONObject("numero").getString("stringValue")
        );
    }

    private JSONObject criarPedidoAdicionar(AlunoDados atual, AlunoDados encontrado)
            throws JSONException {
        JSONArray escritas = new JSONArray();
        escritas.put(criarAtualizacao(atual.uid, encontrado));
        escritas.put(criarAtualizacao(encontrado.uid, atual));
        return new JSONObject().put("writes", escritas);
    }

    private JSONObject criarAtualizacao(String uidDono, AlunoDados amigo)
            throws JSONException {
        JSONObject campos = new JSONObject();
        campos.put("nome", new JSONObject().put("stringValue", amigo.nome));
        campos.put("numero", new JSONObject().put("stringValue", amigo.numero));

        JSONObject documento = new JSONObject();
        documento.put(
                "name",
                FirebaseConfig.FIRESTORE_URL.substring(
                        FirebaseConfig.FIRESTORE_URL.indexOf("projects/")
                ) + "/alunos/" + uidDono + "/amigos/" + amigo.uid
        );
        documento.put("fields", campos);

        return new JSONObject().put("update", documento);
    }

    private JSONObject criarPedidoRemover(String uidAtual, String uidAmigo)
            throws JSONException {
        JSONArray escritas = new JSONArray();
        escritas.put(new JSONObject().put("delete", caminhoAmizade(uidAtual, uidAmigo)));
        escritas.put(new JSONObject().put("delete", caminhoAmizade(uidAmigo, uidAtual)));
        return new JSONObject().put("writes", escritas);
    }

    private String caminhoAmizade(String uidDono, String uidAmigo) {
        return FirebaseConfig.FIRESTORE_URL.substring(
                FirebaseConfig.FIRESTORE_URL.indexOf("projects/")
        ) + "/alunos/" + uidDono + "/amigos/" + uidAmigo;
    }

    private void enviarCommit(String idToken, JSONObject pedido) throws IOException {
        HttpResponse resposta = httpClient.enviarPedido(
                FirebaseConfig.FIRESTORE_URL + ":commit",
                "POST",
                idToken,
                pedido.toString()
        );

        if (!resposta.isSuccessful()) {
            throw new IOException("HTTP_" + resposta.getStatusCode());
        }
    }

    private void devolverListaErro(ListaCallback callback, String erro) {
        mainHandler.post(() -> callback.onResult(null, erro));
    }

    private void devolverOperacaoErro(OperacaoCallback callback, String erro) {
        mainHandler.post(() -> callback.onResult(erro));
    }

    private interface TokenResult {
        void onSuccess(String uid, String idToken);

        void onError(String erro);
    }

    private static class AlunoDados {
        private final String uid;
        private final String nome;
        private final String numero;

        private AlunoDados(String uid, String nome, String numero) {
            this.uid = uid;
            this.nome = nome;
            this.numero = numero;
        }
    }
}
