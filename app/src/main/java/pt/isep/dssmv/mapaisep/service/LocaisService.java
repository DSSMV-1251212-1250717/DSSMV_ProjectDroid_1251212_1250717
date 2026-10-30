package pt.isep.dssmv.mapaisep.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pt.isep.dssmv.mapaisep.helper.FirebaseConfig;
import pt.isep.dssmv.mapaisep.dto.LocalDTO;
import pt.isep.dssmv.mapaisep.mappers.LocalCampusMapper;
import pt.isep.dssmv.mapaisep.exceptions.SessaoExpiradaException;
import pt.isep.dssmv.mapaisep.handler.JsonHandler;
import pt.isep.dssmv.mapaisep.model.Local;
import pt.isep.dssmv.mapaisep.handler.NetworkHandler;
import pt.isep.dssmv.mapaisep.handler.HttpResponse;

/** Lê os locais fixos do campus através de um pedido HTTP GET. */
public class LocaisService {
    private final NetworkHandler networkHandler = new NetworkHandler();
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
                        HttpResponse resposta = networkHandler.enviarPedido(
                                FirebaseConfig.FIRESTORE_URL + "/locais",
                                "GET", idToken, null
                        );

                        if (!resposta.isSuccessful()) {
                            mainHandler.post(() -> callback.onResult(null, "HTTP_" + resposta.getStatusCode()));
                            return;
                        }

                        JSONArray documentos = new JSONObject(resposta.getBody())
                                .optJSONArray("documents");
                        List<Local> locais = new ArrayList<>();

                        if (documentos != null) {
                            for (int i = 0; i < documentos.length(); i++) {
                                try {
                                    // JSON -> LocalDTO (JsonHandler) -> Local (LocalCampusMapper).
                                    LocalDTO dto = JsonHandler.json2LocalDTO(documentos.getJSONObject(i));
                                    locais.add(LocalCampusMapper.localDTO2Local(dto));
                                } catch (JSONException | IllegalArgumentException ignored) {
                                    // Um local sem coordenadas, com coordenadas em texto inválido
                                    // (NumberFormatException) ou recusado pelo Local (InvalidDataException)
                                    // não impede os restantes de aparecer.
                                }
                            }
                        }

                        mainHandler.post(() -> callback.onResult(locais, null));
                    } catch (SessaoExpiradaException e) {
                        mainHandler.post(() -> callback.onResult(null, SessaoExpiradaException.CODIGO));
                    } catch (IOException e) {
                        mainHandler.post(() -> callback.onResult(null, "NETWORK_ERROR"));
                    } catch (JSONException e) {
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
