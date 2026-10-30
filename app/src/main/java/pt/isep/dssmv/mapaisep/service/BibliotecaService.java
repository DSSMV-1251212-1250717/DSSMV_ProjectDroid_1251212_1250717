package pt.isep.dssmv.mapaisep.service;
import pt.isep.dssmv.mapaisep.helper.BibliotecaConfig;
import pt.isep.dssmv.mapaisep.helper.FirebaseConfig;
import pt.isep.dssmv.mapaisep.handler.NetworkHandler;
import pt.isep.dssmv.mapaisep.handler.HttpResponse;
import pt.isep.dssmv.mapaisep.handler.JsonHandler;
import pt.isep.dssmv.mapaisep.exceptions.SessaoExpiradaException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.time.Instant;

// O AuthService e o BibliotecaService estão no mesmo pacote: não é preciso importar.

public class BibliotecaService {

    private final NetworkHandler networkHandler = new NetworkHandler();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    public interface BibliotecaCallback {
        // Integer (e não int) para poder ser null quando há erro.
        void onResult(Integer lotacaoEstimativa, String erro);
    }

    public void obterLotacao(Context context, BibliotecaCallback callback) {
        // Sem sessão: entrega o erro na main thread e não faz o pedido.
        if (AuthService.getCurrentUid() == null) {
            mainHandler.post(() -> callback.onResult(null, SessaoExpiradaException.CODIGO));
            return;
        }

        // idToken: prova ao Firestore que o pedido vem de um aluno autenticado.
        // Vai no cabeçalho "Authorization: Bearer"; sem ele, o Firestore recusa (401/403).
        // O AuthService entrega um token válido e renova-o se estiver quase a expirar (dura 1 hora).
        new AuthService().obterIdTokenValido(context, new AuthService.TokenCallback() {
            @Override
            public void onSuccess(String idToken) {
                pedirLotacao(idToken, callback);
            }

            @Override
            public void onError(String erro) {
                callback.onResult(null, erro);
            }
        });
    }

    // O token já foi confirmado ou renovado: faz o pedido de contagem ao Firestore.
    private void pedirLotacao(String idToken, BibliotecaCallback callback) {
        new Thread(() -> {
            // Se uma linha do try lançar uma exceção, o Java pára ali e salta logo
            // para o catch desse tipo; as linhas que faltam no try não correm.
            try {
                // 1. Endereço da operação de contagem do Firestore.
                String endereco = FirebaseConfig.FIRESTORE_URL + ":runAggregationQuery";

                // 2. Corpo: "conta as presenças dos últimos 5 minutos".
                JSONObject pedido = construirContagem();

                // 3. POST com o corpo em texto JSON (a consulta vai no corpo, por isso não é GET).
                HttpResponse response = networkHandler.enviarPedido(endereco, "POST", idToken, pedido.toString());

                if (!response.isSuccessful()) {
                    String erro = "HTTP_" + response.getStatusCode();   // ex.: "HTTP_403"
                    mainHandler.post(() -> callback.onResult(null, erro));
                    return;
                }

                // 4. Resposta -> número de pessoas (JsonHandler).
                Integer lotacao = JsonHandler.json2Contagem(response.getBody());

                mainHandler.post(() -> callback.onResult(lotacao, null));

            } catch (SessaoExpiradaException e) {
                mainHandler.post(() -> callback.onResult(null, SessaoExpiradaException.CODIGO));
            } catch (IOException e) {
                // Lançada pelo networkHandler.enviarPedido: sem internet, servidor inalcançável
                // ou passaram os 10 segundos de timeout. Entrega o erro na main thread.
                mainHandler.post(() -> callback.onResult(null, "NETWORK_ERROR"));
            } catch (JSONException | NumberFormatException e) {
                // JSON diferente do esperado (ex.: falta o campo total) ou valor que não é número.
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
