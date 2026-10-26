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
import pt.isep.dssmv.mapaisep.dto.LocalDTO;
import pt.isep.dssmv.mapaisep.mappers.LocalCampusMapper;
import pt.isep.dssmv.mapaisep.model.Local;
import pt.isep.dssmv.mapaisep.network.HttpClient;
import pt.isep.dssmv.mapaisep.network.HttpResponse;

/** Lê os locais fixos do campus através de um pedido HTTP GET. */
public class    LocaisService {
    private final HttpClient httpClient = new HttpClient();
    private final LocalCampusMapper mapper = new LocalCampusMapper();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface LocaisCallback {
        void onResult(List<Local> locais, String erro);
    }

    public void obterLocais(Context context, LocaisCallback callback) {
        new AuthService().obterIdTokenValido(context, new AuthService.TokenCallback() {
            @Override
            public void onSuccess(String idToken) {
                new Thread(() -> {
                    try {
                        HttpResponse resposta = httpClient.enviarPedido(
                                FirebaseConfig.FIRESTORE_URL + "/locais",
                                "GET", idToken, null
                        );

                        if (!resposta.isSuccessful()) {
                            devolverErro(callback, "HTTP_" + resposta.getStatusCode());
                            return;
                        }

                        JSONArray documentos = new JSONObject(resposta.getBody())
                                .optJSONArray("documents");
                        List<Local> locais = new ArrayList<>();

                        if (documentos != null) {
                            for (int i = 0; i < documentos.length(); i++) {
                                try {
                                    LocalDTO dto = mapper.desserializar(
                                            documentos.getJSONObject(i)
                                    );
                                    locais.add(mapper.converterParaModelo(dto));
                                } catch (JSONException ignored) {
                                    // Um local sem coordenadas não impede os restantes de aparecer.
                                }
                            }
                        }

                        mainHandler.post(() -> callback.onResult(locais, null));
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

    private void devolverErro(LocaisCallback callback, String erro) {
        mainHandler.post(() -> callback.onResult(null, erro));
    }
}
