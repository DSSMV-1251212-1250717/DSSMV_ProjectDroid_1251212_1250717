package pt.isep.dssmv.mapaisep.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pt.isep.dssmv.mapaisep.helper.FirebaseConfig;
import pt.isep.dssmv.mapaisep.dto.LocalizacaoDTO;
import pt.isep.dssmv.mapaisep.mappers.LocalizacaoAmigoMapper;
import pt.isep.dssmv.mapaisep.exceptions.SessaoExpiradaException;
import pt.isep.dssmv.mapaisep.handler.JsonHandler;
import pt.isep.dssmv.mapaisep.model.Amigo;
import pt.isep.dssmv.mapaisep.handler.NetworkHandler;
import pt.isep.dssmv.mapaisep.handler.HttpResponse;

/** Comunica com a coleção Firestore localizacoes através de pedidos HTTP manuais. */
public class LocalizacaoService {
    private final NetworkHandler networkHandler = new NetworkHandler();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface LocalizacoesCallback {
        void onResult(List<Amigo> amigosComPosicao, String erro);
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
                        // DTO -> JSON (JsonHandler).
                        JSONObject corpo = JsonHandler.localizacaoDTO2Json(new LocalizacaoDTO(latitude, longitude));

                        networkHandler.enviarPedido(
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
                        List<Amigo> resultado = new ArrayList<>();

                        for (int i = 0; i < amigos.size(); i++) {
                            Amigo amigo = amigos.get(i);
                            HttpResponse resposta = networkHandler.enviarPedido(
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
                                mainHandler.post(() -> callback.onResult(null, "HTTP_" + resposta.getStatusCode()));
                                return;
                            }

                            // JSON -> LocalizacaoDTO (JsonHandler) -> posição guardada no próprio Amigo.
                            LocalizacaoDTO dto = JsonHandler.json2LocalizacaoDTO(new JSONObject(resposta.getBody()));
                            LocalizacaoAmigoMapper.preencherPosicao(dto, amigo);
                            resultado.add(amigo);
                        }

                        mainHandler.post(() -> callback.onResult(resultado, null));
                    } catch (SessaoExpiradaException e) {
                        mainHandler.post(() -> callback.onResult(null, SessaoExpiradaException.CODIGO));
                    } catch (IOException e) {
                        mainHandler.post(() -> callback.onResult(null, "NETWORK_ERROR"));
                    } catch (JSONException | IllegalArgumentException e) {
                        // Formato errado, número mal escrito ou posição recusada pelo Amigo.
                        mainHandler.post(() -> callback.onResult(null, "INVALID_LOCATION_DATA"));
                    }
                }).start();
            }

            @Override
            public void onError(String erro) {
                callback.onResult(null, erro);
            }
        });
    }
}
