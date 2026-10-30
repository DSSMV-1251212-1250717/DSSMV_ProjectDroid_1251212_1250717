package pt.isep.dssmv.mapaisep.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import pt.isep.dssmv.mapaisep.helper.FirebaseConfig;
import pt.isep.dssmv.mapaisep.dto.AlunoDTO;
import pt.isep.dssmv.mapaisep.dto.AulaDTO;
import pt.isep.dssmv.mapaisep.exceptions.SessaoExpiradaException;
import pt.isep.dssmv.mapaisep.handler.JsonHandler;
import pt.isep.dssmv.mapaisep.mappers.AulaMapper;
import pt.isep.dssmv.mapaisep.model.Aula;
import pt.isep.dssmv.mapaisep.handler.NetworkHandler;
import pt.isep.dssmv.mapaisep.handler.HttpResponse;

public class HorarioService {
    private final NetworkHandler networkHandler = new NetworkHandler();

    // Permite à thread secundária pôr o callback na fila da main thread.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Método que quem pede o horário tem de preencher, para receber as aulas ou o erro.
    public interface HorarioCallback {
        void onResult(List<Aula> resultado, String erro);
    }

    // Pede as aulas de hoje do aluno com sessão iniciada.
    // O uid do aluno com sessão iniciada diz qual é o documento a ler.
    public void obterAulasHoje(Context context, HorarioCallback callback) {
        obterAulasHoje(context, AuthService.getCurrentUid(), callback);
    }

    // Pede as aulas de hoje de qualquer aluno (por exemplo, um amigo), pelo uid.
    public void obterAulasHoje(Context context, String uid, HorarioCallback callback) {
        if (uid == null) {
            mainHandler.post(() -> callback.onResult(null, SessaoExpiradaException.CODIGO));
            return;
        }

        // Antes da consulta ao Firestore, confirma que o token ainda serve.
        new AuthService().obterIdTokenValido(context, new AuthService.TokenCallback() {
            @Override
            public void onSuccess(String idToken) {
                obterAulasHojeComToken(uid, idToken, callback);
            }

            @Override
            public void onError(String erro) {
                callback.onResult(null, erro);
            }
        });
    }

    // O token já foi confirmado ou renovado; agora podemos consultar o Firestore.
    private void obterAulasHojeComToken(
            String uid,
            String idToken,
            HorarioCallback callback
    ) {
        // Os pedidos HTTP correm numa thread secundária, para o ecrã não congelar.
        new Thread(() -> {
            try {
                // 1. Saber a turma do aluno (o id do documento é o uid do Firebase Authentication).
                String turmaId = consultarTurma(uid, idToken);

                // 2. Dia da semana de hoje (na base de dados: 1 = segunda ... 7 = domingo).
                Calendar calendar = Calendar.getInstance();

                int diaAndroid = calendar.get(Calendar.DAY_OF_WEEK);

                int diaSemana;
                if (diaAndroid == Calendar.SUNDAY) {
                    diaSemana = 7;
                } else {
                    diaSemana = diaAndroid - 1;
                }

                // 3. Montar a pesquisa: aulas desta turma neste dia.
                JSONObject request = construirConsulta(turmaId, diaSemana);

                // 4. Enviar a pesquisa (runQuery é um POST: os filtros vão no corpo JSON).
                HttpResponse resposta = networkHandler.enviarPedido(FirebaseConfig.FIRESTORE_URL + ":runQuery", "POST", idToken, request.toString());
                if (!resposta.isSuccessful()) {
                    throw new IOException("HTTP_" + resposta.getStatusCode());
                }

                // 5. Converter cada documento recebido numa Aula.
                JSONArray documentos = new JSONArray(resposta.getBody());

                List<Aula> resultado = new ArrayList<>();

                for (int i = 0; i < documentos.length(); i++) {
                    JSONObject item = documentos.getJSONObject(i);

                    // O Firestore pode devolver entradas sem documento: saltam-se.
                    if (!item.has("document")) {
                        continue;
                    }

                    JSONObject documento = item.getJSONObject("document");

                    // JSON -> AulaDTO (JsonHandler): os dados ficam como na API, por exemplo "09:00" em texto.
                    AulaDTO dto = JsonHandler.json2AulaDTO(documento);

                    // AulaDTO -> Aula (AulaMapper): "09:00" passa a LocalTime; o construtor valida os dados.
                    Aula modelo = AulaMapper.aulaDTO2Aula(dto);

                    resultado.add(modelo);
                }

                // 6. Ordenar pela hora de início.
                Collections.sort(resultado, (primeira, segunda) -> primeira.getHoraInicio().compareTo(segunda.getHoraInicio()));

                // 7. Entregar as aulas: o Handler põe o onResult do MenuPrincipalActivity na main thread.
                mainHandler.post(() -> callback.onResult(resultado, null));
            } catch (SessaoExpiradaException e) {
                mainHandler.post(() -> callback.onResult(null, SessaoExpiradaException.CODIGO));
            } catch (IOException e) {
                String mensagem = e.getMessage();
                if (mensagem == null) {
                    mensagem = "NETWORK_ERROR";
                }
                final String erro = mensagem;

                // Devolve o erro na main thread.
                mainHandler.post(() -> callback.onResult(null, erro));
            } catch (JSONException | IllegalArgumentException | DateTimeException e) {
                // JSONException: formato errado. IllegalArgumentException: inclui a InvalidDataException
                // (a Aula recusou os dados) e o NumberFormatException. DateTimeException: hora mal escrita.
                // Dados inválidos ou que não passam a validação da Aula.
                final String erro = "INVALID_LESSON_DATA";

                mainHandler.post(() -> callback.onResult(null, erro));
            }
        }).start();
    }

    // GET ao documento do aluno, para saber a turma.
    private String consultarTurma(String uid, String idToken) throws IOException, JSONException {
        String endereco = FirebaseConfig.FIRESTORE_URL + "/alunos/" + uid;

        HttpResponse resposta = networkHandler.enviarPedido(endereco, "GET", idToken, null);
        if (!resposta.isSuccessful()) {
            throw new IOException("HTTP_" + resposta.getStatusCode());
        }

        // JSON -> AlunoDTO (JsonHandler); só interessa a turma, por exemplo "TurmaB".
        AlunoDTO aluno = JsonHandler.json2AlunoDTO(new JSONObject(resposta.getBody()));
        if (aluno.getTurmaId() == null) {
            throw new JSONException("Aluno sem turma");
        }
        return aluno.getTurmaId();
    }

    // Pesquisa do Firestore: turmaId igual à turma E diaSemana igual ao dia.
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

        return request;
    }

    // Filtro do Firestore "campo igual a valor".
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

    // Escolhe, numa lista de aulas ordenada pela hora de início, a aula que está a decorrer
    // ou, se não houver, a próxima do dia. Devolve null se já não houver mais aulas.
    // Está aqui (e não no ecrã do mapa) porque é lógica sobre aulas, não desenho.
    public static Aula obterProximaAula(List<Aula> aulas, LocalTime agora) {
        for (int i = 0; i < aulas.size(); i++) {
            Aula aula = aulas.get(i);
            if (aula.estaADecorrer(agora) || !aula.getHoraInicio().isBefore(agora)) {
                return aula;
            }
        }
        return null;
    }
}
