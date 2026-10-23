package pt.isep.dssmv.mapaisep.services;
import pt.isep.dssmv.mapaisep.config.BibliotecaConfig;
import pt.isep.dssmv.mapaisep.config.FirebaseConfig;
import pt.isep.dssmv.mapaisep.network.HttpClient;
import pt.isep.dssmv.mapaisep.network.HttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.time.Instant;

//O AuthService e o BibliotecaService estão no mesmo pacote
//nao é preciso importar

public class BibliotecaService {

    private final HttpClient httpClient = new HttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    public interface BibliotecaCallback {
        // Integer (e não int) para poder ser null quando há erro.
        void onResult(Integer lotacaoEstimativa, String erro);
    }

    public void obterLotacao(BibliotecaCallback callback){
        //O getIdToken é static
        // idToken: prova ao Firestore que o pedido vem de um aluno autenticado.
        // Vai no cabeçalho "Authorization: Bearer"; sem ele, o Firestore recusa (401/403).
        String idToken = AuthService.getIdToken();

        // Sem token não há sessão: entrega o erro na main thread e não faz o pedido.
        if (idToken == null) {
            mainHandler.post(() -> callback.onResult(null, "NOT_AUTHENTICATED"));
            //pede para correr na main thread a funcao implementada
            return;
        }

        new Thread(() -> {
            // Se uma linha do try lançar uma exceção, o Java pára ali e salta logo
            // para o catch desse tipo; as linhas que faltam no try não correm.
            try {
                // 1. Endereço da operação de contagem do Firestore.
                String endereco = FirebaseConfig.FIRESTORE_URL + ":runAggregationQuery";

                // 2. Corpo: "conta as presenças dos últimos 5 minutos".
                JSONObject pedido = construirContagem();

                // 3. POST com o corpo em texto JSON (a consulta vai no corpo, por isso não é GET).
                HttpResponse response = httpClient.enviarPedido(endereco, "POST", idToken, pedido.toString());

                if (!response.isSuccessful()) {
                    String erro = "HTTP_" + response.getStatusCode();   // ex.: "HTTP_403"
                    mainHandler.post(() -> callback.onResult(null, erro));
                    return;
                }

                // 4. Resposta: [ { "result": { "aggregateFields": { "total": { "integerValue": "3" } } } } ]
                JSONArray resultados = new JSONArray(response.getBody());
                String valor = resultados.getJSONObject(0)
                        .getJSONObject("result")
                        .getJSONObject("aggregateFields")
                        .getJSONObject("total")
                        .getString("integerValue");      // "3", em texto
                Integer lotacao = Integer.parseInt(valor);               // 3, em número

                mainHandler.post(() -> callback.onResult(lotacao, null));

            } catch (IOException e) {
                // Lançada pelo httpClient.enviarPedido: sem internet, servidor inalcançável
                // ou passaram os 10 segundos de timeout. Entrega o erro na main thread.
                mainHandler.post(() -> callback.onResult(null, "NETWORK_ERROR"));
            } catch (JSONException | NumberFormatException e) {
                // JSON diferente do esperado (ex.: falta o numeroPessoas) ou valor que não é número.
                mainHandler.post(() -> callback.onResult(null, "INVALID_DATA"));
            }
        }).start();
    }

    // Monta o corpo do runAggregationQuery, de dentro para fora:
    // conta os documentos de presencas com atualizadoEm >= (agora - 5 minutos).
    private JSONObject construirContagem() throws JSONException {
        // Agora menos 5 minutos, por exemplo "2026-10-23T09:55:00Z".
        String limite = Instant.now().minusMillis(BibliotecaConfig.VALIDADE_PRESENCA_MS).toString();

        // Filtro: atualizadoEm >= limite
        JSONObject fieldFilter = new JSONObject();
        fieldFilter.put("field", new JSONObject().put("fieldPath", "atualizadoEm"));
        fieldFilter.put("op", "GREATER_THAN_OR_EQUAL");
        fieldFilter.put("value", new JSONObject().put("timestampValue", limite));
        JSONObject where = new JSONObject().put("fieldFilter", fieldFilter);

        // Onde procurar: a coleção presencas.
        JSONArray from = new JSONArray().put(new JSONObject().put("collectionId", "presencas"));

        JSONObject structuredQuery = new JSONObject();
        structuredQuery.put("from", from);
        structuredQuery.put("where", where);

        // O que calcular: contar, e chamar "total" ao resultado.
        JSONArray aggregations = new JSONArray()
                .put(new JSONObject().put("alias", "total").put("count", new JSONObject()));

        JSONObject agregacao = new JSONObject();
        agregacao.put("structuredQuery", structuredQuery);
        agregacao.put("aggregations", aggregations);

        return new JSONObject().put("structuredAggregationQuery", agregacao);
    }
}
