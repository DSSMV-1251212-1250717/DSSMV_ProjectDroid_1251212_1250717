package pt.isep.dssmv.mapaisep.controllers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pt.isep.dssmv.mapaisep.model.Amigo;
import pt.isep.dssmv.mapaisep.services.AmigosService;
import pt.isep.dssmv.mapaisep.services.LocalizacaoService;

// Mostra a posição dos amigos no mapa e atualiza-a de 5 em 5 segundos.
// O MenuPrincipal cria-a, entrega-lhe o mapa e chama iniciar() / parar().
public class AmigosNoMapa {
    private static final long INTERVALO_MS = 5000;

    private final Context context;
    private final AmigosService amigosService = new AmigosService();
    private final LocalizacaoService localizacaoService = new LocalizacaoService();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Um marcador por amigo, guardado pelo uid, para o mover em vez de criar outro.
    private final Map<String, Marker> marcadores = new HashMap<>();

    private GoogleMap mapa;

    // true enquanto o mapa está aberto; o ciclo só continua enquanto for true.
    private boolean ativo;

    // Ciclo: consulta as posições e volta a agendar-se daqui a INTERVALO_MS.
    private final Runnable ciclo = new Runnable() {
        @Override
        public void run() {
            consultarLocalizacoesAmigos();
            if (ativo) {
                handler.postDelayed(this, INTERVALO_MS);
            }
        }
    };

    public AmigosNoMapa(Context context) {
        this.context = context;
    }

    // Chamado no onMapReady do MenuPrincipal, quando o Google Maps está pronto.
    public void definirMapa(GoogleMap mapa) {
        this.mapa = mapa;
    }

    // Começa o ciclo (mapa aberto).
    public void iniciar() {
        ativo = true;
        handler.removeCallbacks(ciclo);
        handler.post(ciclo);
    }

    // Pára o ciclo (página Início ou app em fundo).
    public void parar() {
        ativo = false;
        handler.removeCallbacks(ciclo);
    }

    // Pede a lista de amigos, depois a posição de cada um, e atualiza os marcadores.
    private void consultarLocalizacoesAmigos() {
        if (!ativo || mapa == null) {
            return;
        }

        amigosService.listarAmigos(context, (amigos, erro) -> {
            if (erro != null || amigos == null || !ativo) {
                return;
            }

            localizacaoService.obterLocalizacoesAmigos(context, amigos, (amigosComPosicao, erroLocalizacao) -> {
                if (erroLocalizacao != null || amigosComPosicao == null || !ativo || mapa == null) {
                    return;
                }

                Set<String> uidsComPosicao = new HashSet<>();
                for (Amigo amigo : amigosComPosicao) {
                    uidsComPosicao.add(amigo.getUid());
                    LatLng posicao = new LatLng(amigo.getLatitude(), amigo.getLongitude());
                    Marker marcador = marcadores.get(amigo.getUid());

                    if (marcador == null) {
                        marcador = mapa.addMarker(new MarkerOptions()
                                .position(posicao)
                                .title(amigo.getNome()));
                        marcadores.put(amigo.getUid(), marcador);
                    } else {
                        marcador.setPosition(posicao);
                    }
                }

                // Se já não houver localização no Firebase, remove o marcador do mapa.
                for (String uid : new HashSet<>(marcadores.keySet())) {
                    if (!uidsComPosicao.contains(uid)) {
                        Marker marcador = marcadores.remove(uid);
                        if (marcador != null) {
                            marcador.remove();
                        }
                    }
                }
            });
        });
    }
}
