package pt.isep.dssmv.mapaisep.services;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import pt.isep.dssmv.mapaisep.config.FirebaseConfig;
import pt.isep.dssmv.mapaisep.dto.AulaDTO;
import pt.isep.dssmv.mapaisep.mappers.AulaMapper;
import pt.isep.dssmv.mapaisep.model.Aula;
import pt.isep.dssmv.mapaisep.network.HttpClient;
import pt.isep.dssmv.mapaisep.network.HttpResponse;

public class HorarioService {
    private final HttpClient httpClient = new HttpClient();
    private final AulaMapper aulaMapper = new AulaMapper();

    // Põe o resultado na fila da main thread.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Interface que quem chama implementa para receber o horário ou o erro.
    public interface HorarioCallback {
        void onResult(List<Aula> resultado, String erro);
    }

    // Vai buscar as aulas de hoje do aluno autenticado.
    public void obterAulasHoje(HorarioCallback callback) {
        // uid: qual o aluno. idToken: prova a identidade ao Firestore.
        String uid = AuthService.getCurrentUid();
        String idToken = AuthService.getIdToken();

        if (uid == null || idToken == null) {
            mainHandler.post(() -> callback.onResult(null, "NOT_AUTHENTICATED"));

            return;
        }

        // Thread secundária: a rede não pode bloquear a main thread.
        new Thread(() -> {
            try {
                // 1. Turma do aluno (o documento do aluno tem o uid como id).
                String turmaId = consultarTurma(uid, idToken);

                // 2. Dia da semana, na nossa base: 1 = segunda ... 7 = domingo.
                Calendar calendar = Calendar.getInstance();

                int diaAndroid = calendar.get(Calendar.DAY_OF_WEEK);

                int diaSemana;
                if (diaAndroid == Calendar.SUNDAY) {
                    diaSemana = 7;
                } else {
                    diaSemana = diaAndroid - 1;
                }

                // 3. Consulta: aulas desta turma neste dia.
                JSONObject request = construirConsulta(turmaId, diaSemana);

                // 4. runQuery usa POST porque os filtros vão no corpo JSON.
                String respostaJson = enviarPedido(FirebaseConfig.FIRESTORE_URL + ":runQuery", "POST", idToken, request);

                // 5. Desserialização da resposta.
                JSONArray documentos = new JSONArray(respostaJson);

                List<Aula> resultado = new ArrayList<>();

                for (int i = 0; i < documentos.length(); i++) {
                    JSONObject item = documentos.getJSONObject(i);

                    // O Firestore pode devolver entradas sem documento.
                    if (!item.has("document")) {
                        continue;
                    }

                    JSONObject documento = item.getJSONObject("document");

                    JSONObject campos = documento.getJSONObject("fields");

                    // JSON -> AulaDTO (dados no formato da API, ex.: "09:00" como texto).
                    AulaDTO dto = aulaMapper.desserializarAula(campos);

                    // AulaDTO -> Aula ("09:00" passa a LocalTime; o construtor valida).
                    Aula modelo = aulaMapper.converterParaModelo(dto);

                    resultado.add(modelo);
                }

                // Ordena pela hora de início.
                Collections.sort(resultado, (primeira, segunda) -> primeira.getHoraInicio().compareTo(segunda.getHoraInicio()));

                // Entrega o resultado na main thread: corre o onResult do MenuPrincipal.
                mainHandler.post(() -> callback.onResult(resultado, null));
            } catch (IOException e) {
                String mensagem = e.getMessage();
                if (mensagem == null) {
                    mensagem = "NETWORK_ERROR";
                }
                final String erro = mensagem;

                // O erro também é entregue na main thread.
                mainHandler.post(() -> callback.onResult(null, erro));
            } catch (JSONException | IllegalArgumentException | DateTimeException e) {
                // JSON inválido ou dados que o modelo rejeita.
                final String erro = "INVALID_LESSON_DATA";

                mainHandler.post(() -> callback.onResult(null, erro));
            }
        }).start();
    }

    // GET ao documento do aluno para ler a turma (ex.: "TurmaB").
    private String consultarTurma(String uid, String idToken) throws IOException, JSONException {
        String endereco = FirebaseConfig.FIRESTORE_URL + "/alunos/" + uid;

        String resposta = enviarPedido(endereco, "GET", idToken, null);

        JSONObject documento = new JSONObject(resposta);

        JSONObject campos = documento.getJSONObject("fields");

        return campos.getJSONObject("turmaId").getString("stringValue");
    }

    // Constrói a consulta do Firestore: turma E dia da semana.
    private JSONObject construirConsulta(String turmaId, int diaSemana) throws JSONException {
        JSONObject filtroTurma = criarFiltro("turmaId", new JSONObject().put("stringValue", turmaId));

        JSONObject filtroDia = criarFiltro("diaSemana", new JSONObject().put("integerValue", String.valueOf(diaSemana)));

        JSONArray filtros = new JSONArray();
        filtros.put(filtroTurma);
        filtros.put(filtroDia);

        JSONObject compositeFilter = new JSONObject();
        compositeFilter.put("op", "AND");
        compositeFilter.put("filters", filtros);

        JSONObject where = new JSONObject();
        where.put("compositeFilter", compositeFilter);

        JSONObject collection = new JSONObject();
        collection.put("collectionId", "aulas");

        JSONArray from = new JSONArray();
        from.put(collection);

        JSONObject structuredQuery = new JSONObject();
        structuredQuery.put("from", from);
        structuredQuery.put("where", where);

        JSONObject request = new JSONObject();
        request.put("structuredQuery", structuredQuery);

        // Serialização: o enviarPedido() converte isto em texto para o corpo do pedido.
        return request;
    }

    // Cria um filtro de igualdade do Firestore (campo == valor).
    private JSONObject criarFiltro(String campo, JSONObject valor) throws JSONException {
        JSONObject field = new JSONObject();
        field.put("fieldPath", campo);

        JSONObject fieldFilter = new JSONObject();
        fieldFilter.put("field", field);
        fieldFilter.put("op", "EQUAL");
        fieldFilter.put("value", valor);

        JSONObject filtro = new JSONObject();
        filtro.put("fieldFilter", fieldFilter);

        return filtro;
    }

    // O serviço interpreta o estado HTTP; o HttpClient trata apenas do transporte.
    private String enviarPedido(String endereco, String metodo, String idToken, JSONObject corpo) throws IOException {
        String textoJson = null;
        if (corpo != null) {
            textoJson = corpo.toString();
        }

        HttpResponse response = httpClient.enviarPedido(endereco, metodo, idToken, textoJson);
        if (!response.isSuccessful()) {
            throw new IOException("HTTP_" + response.getStatusCode());
        }
        return response.getBody();
    }
}
