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

    // Allows the secondary thread to schedule the callback
    // on Android's main thread.
    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    // Defines the method that the caller must implement
    // to receive the timetable or an error.
    public interface HorarioCallback {

        void onResult(List<Aula> resultado, String erro);
    }

    // Retrieves today's lessons for the authenticated student.
    public void obterAulasHoje(HorarioCallback callback) {

        // UID identifies which student's document we need.
        String uid = AuthService.getCurrentUid();

        // ID Token proves the student's identity to Firestore.
        String idToken = AuthService.getIdToken();

        if (uid == null || idToken == null) {

            mainHandler.post(() ->
                    callback.onResult(null, "NOT_AUTHENTICATED")
            );

            return;
        }

        // THREAD:
        // Create a secondary thread to perform HTTP requests.
        // This prevents the main thread from blocking the UI
        // while waiting for Firestore responses.
        new Thread(() -> {

            try {

                // STEP 1: Retrieve the student's class.
                //
                // The Firestore document ID matches the
                // Firebase Authentication UID.
                String turmaId = consultarTurma(uid, idToken);

                // STEP 2: Determine the current weekday.
                //
                // Our database uses:
                // 1 = Monday, ..., 7 = Sunday.
                Calendar calendar = Calendar.getInstance();

                int diaAndroid =
                        calendar.get(Calendar.DAY_OF_WEEK);

                int diaSemana;
                if (diaAndroid == Calendar.SUNDAY) {
                    diaSemana = 7;
                } else {
                    diaSemana = diaAndroid - 1;
                }

                // STEP 3: Build a Firestore query.
                //
                // Retrieve lessons matching the student's
                // class and the current weekday.
                JSONObject request =
                        construirConsulta(turmaId, diaSemana);

                // STEP 4: Send the HTTP request to Firestore.
                //
                // runQuery uses POST because its filters
                // are sent in the JSON request body.
                String respostaJson = enviarPedido(
                        FirebaseConfig.FIRESTORE_URL + ":runQuery",
                        "POST",
                        idToken,
                        request
                );

                // STEP 5: Deserialize the received JSON.
                JSONArray documentos = new JSONArray(respostaJson);

                List<Aula> resultado = new ArrayList<>();

                for (int i = 0; i < documentos.length(); i++) {

                    JSONObject item = documentos.getJSONObject(i);

                    // Firestore can return responses without documents.
                    if (!item.has("document")) {
                        continue;
                    }

                    JSONObject documento =
                            item.getJSONObject("document");

                    JSONObject campos =
                            documento.getJSONObject("fields");

                    // DESERIALIZATION: JSON -> AulaDTO
                    //
                    // Read the JSON fields and create an AulaDTO.
                    // The DTO keeps data in the API's format,
                    // such as "09:00" stored as a String.
                    AulaDTO dto = aulaMapper.desserializarAula(campos);

                    // DTO TO MODEL: AulaDTO -> Aula
                    //
                    // Convert strings into their Java data types.
                    // For example, "09:00" becomes a LocalTime.
                    // Also create a Sala object.
                    //
                    // The Aula constructor validates the data.
                    Aula modelo = aulaMapper.converterParaModelo(dto);

                    // Add the validated Model object to the list.
                    resultado.add(modelo);
                }

                // Sort the lessons by their starting time.
                Collections.sort(
                        resultado,
                        (primeira, segunda) ->
                                primeira.getHoraInicio()
                                        .compareTo(segunda.getHoraInicio())
                );

                // CALLBACK - STEP 3:
                //
                // MenuPrincipal previously gave this service
                // a callback object containing its onResult()
                // implementation.
                //
                // Now that the HTTP requests have finished,
                // HorarioService has the requested data.
                //
                // Handler.post() places a task in the main
                // thread's execution queue.
                //
                // The main thread will call callback.onResult(),
                // executing the code implemented in MenuPrincipal.
                //
                // The result list is passed as an argument.
                mainHandler.post(() ->
                        callback.onResult(resultado, null)
                );

            } catch (IOException e) {

                String mensagem = e.getMessage();
                if (mensagem == null) {
                    mensagem = "NETWORK_ERROR";
                }
                final String erro = mensagem;

                // Return the error on the main thread.
                mainHandler.post(() ->
                        callback.onResult(null, erro)
                );

            } catch (JSONException | IllegalArgumentException | DateTimeException e) {

                // Handle invalid data or Model conversion errors.
                final String erro = "INVALID_LESSON_DATA";

                mainHandler.post(() ->
                        callback.onResult(null, erro)
                );
            }

        }).start();
    }

    // Retrieves the student's class identifier from Firestore.
    private String consultarTurma(String uid, String idToken)
            throws IOException, JSONException {

        String endereco = FirebaseConfig.FIRESTORE_URL + "/alunos/" + uid;

        // First HTTP request: GET the student's document.
        String resposta = enviarPedido(
                endereco,
                "GET",
                idToken,
                null
        );

        JSONObject documento = new JSONObject(resposta);

        JSONObject campos = documento.getJSONObject("fields");

        // Read the class identifier, for example "TurmaB".
        return campos.getJSONObject("turmaId")
                .getString("stringValue");
    }

    // Creates a Firestore query for class and weekday.
    private JSONObject construirConsulta(
            String turmaId,
            int diaSemana
    ) throws JSONException {

        JSONObject filtroTurma = criarFiltro(
                "turmaId",
                new JSONObject().put("stringValue", turmaId)
        );

        JSONObject filtroDia = criarFiltro(
                "diaSemana",
                new JSONObject().put(
                        "integerValue",
                        String.valueOf(diaSemana)
                )
        );

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

        // SERIALIZATION:
        // enviarPedido() will convert this JSONObject into text
        // and send it in the HTTP request body.
        return request;
    }

    // Creates a Firestore equality filter.
    private JSONObject criarFiltro(
            String campo,
            JSONObject valor
    ) throws JSONException {

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
    private String enviarPedido(String endereco, String metodo, String idToken,
                                JSONObject corpo) throws IOException {
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
