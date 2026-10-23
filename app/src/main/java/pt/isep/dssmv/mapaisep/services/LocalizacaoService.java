package pt.isep.dssmv.mapaisep.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pt.isep.dssmv.mapaisep.config.FirebaseConfig;
import pt.isep.dssmv.mapaisep.dto.LocalizacaoDTO;
import pt.isep.dssmv.mapaisep.mappers.LocalizacaoMapper;
import pt.isep.dssmv.mapaisep.model.Amigo;
import pt.isep.dssmv.mapaisep.model.LocalizacaoAmigo;
import pt.isep.dssmv.mapaisep.network.HttpClient;
import pt.isep.dssmv.mapaisep.network.HttpResponse;

/** Comunica com a coleção Firestore localizacoes através de pedidos HTTP manuais. */
public class LocalizacaoService {
    private final HttpClient httpClient = new HttpClient();
    private final LocalizacaoMapper mapper = new LocalizacaoMapper();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface LocalizacoesCallback {
        void onResult(List<LocalizacaoAmigo> localizacoes, String erro);
    }

    /**
     * Atualiza sempre o mesmo documento: localizacoes/{uid}.
     * PATCH altera latitude e longitude sem criar uma lista de posições antigas.
     */
    public void atualizarMinhaLocalizacao(Context context, double latitude, double longitude) {
        String uid = AuthService.getCurrentUid();
        if (uid == null) {
            return;
        }

        new AuthService().obterIdTokenValido(context, new AuthService.TokenCallback() {
            @Override
            public void onSuccess(String idToken) {
                new Thread(() -> {
                    try {
                        JSONObject campos = new JSONObject();
                        campos.put("latitude", new JSONObject().put("doubleValue", latitude));
                        campos.put("longitude", new JSONObject().put("doubleValue", longitude));

                        JSONObject corpo = new JSONObject();
                        corpo.put("fields", campos);

                        httpClient.enviarPedido(
                                FirebaseConfig.FIRESTORE_URL + "/localizacoes/" + uid,
                                "PATCH",
                                idToken,
                                corpo.toString()
                        );
                    } catch (IOException | JSONException ignored) {
                        // A localização no próprio mapa continua disponível mesmo sem rede.
                    }
                }).start();
            }

            @Override
            public void onError(String erro) {
                // Não há interface a atualizar neste pedido em segundo plano.
            }
        });
    }

    /** Obtém a última posição de cada amigo que decidiu partilhá-la. */
    public void obterLocalizacoesAmigos(
            Context context,
            List<Amigo> amigos,
            LocalizacoesCallback callback
    ) {
        new AuthService().obterIdTokenValido(context, new AuthService.TokenCallback() {
            @Override
            public void onSuccess(String idToken) {
                new Thread(() -> {
                    try {
                        List<LocalizacaoAmigo> resultado = new ArrayList<>();

                        for (Amigo amigo : amigos) {
                            HttpResponse resposta = httpClient.enviarPedido(
                                    FirebaseConfig.FIRESTORE_URL
                                            + "/localizacoes/" + amigo.getUid(),
                                    "GET",
                                    idToken,
                                    null
                            );

                            // 404 significa que este amigo ainda não partilhou localização.
                            if (resposta.getStatusCode() == 404) {
                                continue;
                            }

                            if (!resposta.isSuccessful()) {
                                devolverErro(callback, "HTTP_" + resposta.getStatusCode());
                                return;
                            }

                            LocalizacaoDTO dto = mapper.desserializar(
                                    new JSONObject(resposta.getBody()), amigo
                            );
                            resultado.add(mapper.converterParaModelo(dto));
                        }

                        mainHandler.post(() -> callback.onResult(resultado, null));
                    } catch (IOException e) {
                        devolverErro(callback, "NETWORK_ERROR");
                    } catch (JSONException e) {
                        devolverErro(callback, "INVALID_LOCATION_DATA");
                    }
                }).start();
            }

            @Override
            public void onError(String erro) {
                callback.onResult(null, erro);
            }
        });
    }

    private void devolverErro(LocalizacoesCallback callback, String erro) {
        mainHandler.post(() -> callback.onResult(null, erro));
    }
}
