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

// Listar, adicionar e remover amigos no Firestore.
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

    // Context: objeto da app através do qual se pedem coisas ao Android
    // (ficheiros, SharedPreferences, strings, abrir ecrãs, GPS/Wi-Fi).
    // Aqui só é passado ao AuthService, que o usa para guardar o token.
    public void listarAmigos(Context context, ListaCallback callback) {
        String uidAtual = AuthService.getCurrentUid();
        if (uidAtual == null) {
            mainHandler.post(() -> callback.onResult(null, "NOT_AUTHENTICATED"));
            return;
        }

        // 1. Pedir o token (a chave que o Firestore exige em cada pedido).
        // O TokenCallback é uma interface do AuthService com dois métodos vazios:
        // onSuccess e onError. Aqui preenchemos esses métodos com o que queremos
        // que aconteça. Criar este objeto não executa nada: o AuthService guarda-o
        // e chama o onSuccess quando tiver o token, ou o onError se falhar.
        new AuthService().obterIdTokenValido(context, new AuthService.TokenCallback() {

            // Chamado pelo AuthService, na main thread, com o token pronto a usar.
            @Override
            public void onSuccess(String idToken) {
                // Os pedidos HTTP demoram; na main thread o ecrã congelava
                // (e o Android nem deixa). Por isso correm numa thread secundária.
                new Thread(() -> {
                    try {
                        // 2. Pedir a lista de amigos.
                        HttpResponse resposta = httpClient.enviarPedido(
                                FirebaseConfig.FIRESTORE_URL + "/alunos/" + uidAtual + "/amigos",
                                "GET", idToken, null);

                        // 404: ainda não tens amigos.
                        if (resposta.getStatusCode() == 404) {
                            mainHandler.post(() -> callback.onResult(new ArrayList<>(), null));
                            return;
                        }
                        if (!resposta.isSuccessful()) {
                            String erro = "HTTP_" + resposta.getStatusCode();
                            mainHandler.post(() -> callback.onResult(null, erro));
                            return;
                        }

                        // 3. Converter cada documento num Amigo.
                        JSONArray documentos = new JSONObject(resposta.getBody()).optJSONArray("documents");
                        List<Amigo> amigos = new ArrayList<>();
                        if (documentos != null) {
                            for (int i = 0; i < documentos.length(); i++) {
                                AmigoDTO dto = amigoMapper.desserializarAmigo(documentos.getJSONObject(i));
                                amigos.add(amigoMapper.converterParaModelo(dto));
                            }
                        }

                        // 4. Mostrar no ecrã. Só a main thread pode mexer no ecrã,
                        // por isso o mainHandler.post manda o onResult da Activity
                        // correr lá.
                        mainHandler.post(() -> callback.onResult(amigos, null));
                    } catch (IOException e) {
                        mainHandler.post(() -> callback.onResult(null, "NETWORK_ERROR"));
                    } catch (JSONException e) {
                        mainHandler.post(() -> callback.onResult(null, "INVALID_FRIEND_DATA"));
                    }
                }).start();
            }

            // Chamado pelo AuthService se não conseguir o token (sem rede, sessão expirada).
            @Override
            public void onError(String erro) {
                callback.onResult(null, erro);
            }
        });
    }

    public void adicionarAmigo(Context context, String numero, OperacaoCallback callback) {
        if (!numero.matches("[0-9]{7}")) {
            mainHandler.post(() -> callback.onResult("INVALID_STUDENT_NUMBER"));
            return;
        }
        String uidAtual = AuthService.getCurrentUid();
        if (uidAtual == null) {
            mainHandler.post(() -> callback.onResult("NOT_AUTHENTICATED"));
            return;
        }

        // 1. Pedir o token.
        new AuthService().obterIdTokenValido(context, new AuthService.TokenCallback() {
            @Override
            public void onSuccess(String idToken) {
                new Thread(() -> {
                    try {
                        // 2. Os teus dados.
                        Amigo eu = obterAluno(uidAtual, idToken);

                        // 3. Procurar o amigo pelo número.
                        Amigo amigo = procurarAluno(numero, idToken);

                        // 4. Verificar.
                        if (amigo == null) {
                            mainHandler.post(() -> callback.onResult("STUDENT_NOT_FOUND"));
                            return;
                        }
                        if (uidAtual.equals(amigo.getUid())) {
                            mainHandler.post(() -> callback.onResult("CANNOT_ADD_SELF"));
                            return;
                        }

                        // 5. Gravar a amizade dos dois lados.
                        gravarAmizade(idToken, eu, amigo);

                        // 6. Avisar o ecrã que correu bem.
                        mainHandler.post(() -> callback.onResult(null));
                    } catch (IOException e) {
                        mainHandler.post(() -> callback.onResult(e.getMessage()));
                    } catch (JSONException e) {
                        mainHandler.post(() -> callback.onResult("INVALID_FRIEND_DATA"));
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
        String uidAtual = AuthService.getCurrentUid();
        if (uidAtual == null) {
            mainHandler.post(() -> callback.onResult("NOT_AUTHENTICATED"));
            return;
        }

        // 1. Pedir o token.
        new AuthService().obterIdTokenValido(context, new AuthService.TokenCallback() {
            @Override
            public void onSuccess(String idToken) {
                new Thread(() -> {
                    try {
                        // 2. Apagar a amizade dos dois lados.
                        apagarAmizade(idToken, uidAtual, amigo.getUid());

                        // 3. Avisar o ecrã que correu bem.
                        mainHandler.post(() -> callback.onResult(null));
                    } catch (IOException e) {
                        mainHandler.post(() -> callback.onResult(e.getMessage()));
                    } catch (JSONException e) {
                        mainHandler.post(() -> callback.onResult("INVALID_FRIEND_DATA"));
                    }
                }).start();
            }

            @Override
            public void onError(String erro) {
                callback.onResult(erro);
            }
        });
    }

    // GET ao aluno com este uid.
    private Amigo obterAluno(String uid, String idToken) throws IOException, JSONException {
        HttpResponse resposta = httpClient.enviarPedido(
                FirebaseConfig.FIRESTORE_URL + "/alunos/" + uid, "GET", idToken, null);

        if (!resposta.isSuccessful()) {
            throw new IOException("HTTP_" + resposta.getStatusCode());
        }
        return lerAluno(new JSONObject(resposta.getBody()));
    }

    // Pesquisa o aluno com este número. Devolve null se não existir.
    private Amigo procurarAluno(String numero, String idToken) throws IOException, JSONException {
        JSONObject filtro = new JSONObject()
                .put("field", new JSONObject().put("fieldPath", "numero"))
                .put("op", "EQUAL")
                .put("value", new JSONObject().put("stringValue", numero));

        JSONObject query = new JSONObject()
                .put("from", new JSONArray().put(new JSONObject().put("collectionId", "alunos")))
                .put("where", new JSONObject().put("fieldFilter", filtro))
                .put("limit", 1);

        HttpResponse resposta = httpClient.enviarPedido(
                FirebaseConfig.FIRESTORE_URL + ":runQuery", "POST", idToken,
                new JSONObject().put("structuredQuery", query).toString());

        if (!resposta.isSuccessful()) {
            throw new IOException("HTTP_" + resposta.getStatusCode());
        }

        JSONArray resultados = new JSONArray(resposta.getBody());
        for (int i = 0; i < resultados.length(); i++) {
            if (resultados.getJSONObject(i).has("document")) {
                return lerAluno(resultados.getJSONObject(i).getJSONObject("document"));
            }
        }
        return null;
    }

    // Grava o amigo na tua lista e tu na lista dele, num só pedido.
    private void gravarAmizade(String idToken, Amigo eu, Amigo amigo)
            throws IOException, JSONException {
        JSONArray escritas = new JSONArray()
                .put(new JSONObject().put("update", documentoAmigo(eu.getUid(), amigo)))
                .put(new JSONObject().put("update", documentoAmigo(amigo.getUid(), eu)));

        HttpResponse resposta = httpClient.enviarPedido(
                FirebaseConfig.FIRESTORE_URL + ":commit", "POST", idToken,
                new JSONObject().put("writes", escritas).toString());

        if (!resposta.isSuccessful()) {
            throw new IOException("HTTP_" + resposta.getStatusCode());
        }
    }

    // Apaga o amigo da tua lista e tu da lista dele, num só pedido.
    private void apagarAmizade(String idToken, String uidAtual, String uidAmigo)
            throws IOException, JSONException {
        JSONArray escritas = new JSONArray()
                .put(new JSONObject().put("delete", caminhoAmizade(uidAtual, uidAmigo)))
                .put(new JSONObject().put("delete", caminhoAmizade(uidAmigo, uidAtual)));

        HttpResponse resposta = httpClient.enviarPedido(
                FirebaseConfig.FIRESTORE_URL + ":commit", "POST", idToken,
                new JSONObject().put("writes", escritas).toString());

        if (!resposta.isSuccessful()) {
            throw new IOException("HTTP_" + resposta.getStatusCode());
        }
    }

    // Lê o uid, o nome e o número de um documento de aluno.
    private Amigo lerAluno(JSONObject documento) throws JSONException {
        JSONObject campos = documento.getJSONObject("fields");
        String caminho = documento.getString("name");

        return new Amigo(
                caminho.substring(caminho.lastIndexOf('/') + 1),
                campos.getJSONObject("nome").getString("stringValue"),
                campos.getJSONObject("numero").getString("stringValue"));
    }

    // Documento com o nome e o número do amigo, guardado na lista do dono.
    private JSONObject documentoAmigo(String uidDono, Amigo amigo) throws JSONException {
        JSONObject campos = new JSONObject()
                .put("nome", new JSONObject().put("stringValue", amigo.getNome()))
                .put("numero", new JSONObject().put("stringValue", amigo.getNumero()));

        return new JSONObject()
                .put("name", caminhoAmizade(uidDono, amigo.getUid()))
                .put("fields", campos);
    }

    // Caminho alunos/{dono}/amigos/{amigo}.
    private String caminhoAmizade(String uidDono, String uidAmigo) {
        String url = FirebaseConfig.FIRESTORE_URL;
        return url.substring(url.indexOf("projects/")) + "/alunos/" + uidDono + "/amigos/" + uidAmigo;
    }
}
