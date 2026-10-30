package pt.isep.dssmv.mapaisep.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import pt.isep.dssmv.mapaisep.model.Amigo;
import pt.isep.dssmv.mapaisep.service.AmigosService;
import pt.isep.dssmv.mapaisep.service.LocalizacaoService;
import pt.isep.dssmv.mapaisep.service.PerfilService;
import pt.isep.dssmv.mapaisep.helper.IconeFotoHelper;

// Mostra a posição dos amigos no mapa e atualiza-a de 5 em 5 segundos.
// O MenuPrincipalActivity cria-a, entrega-lhe o mapa e chama iniciar() / parar().
public class AmigosNoMapa {
    private static final long INTERVALO_MS = 5000;

    private final Context context;
    private final AmigosService amigosService = new AmigosService();
    private final LocalizacaoService localizacaoService = new LocalizacaoService();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Uma posição mais antiga do que isto aparece a cinzento: o amigo já fechou o mapa
    // (ou a app) e a posição guardada pode já não dizer onde ele está.
    private static final long VALIDADE_POSICAO_MS = 5 * 60_000L;

    // Um marcador por amigo, guardado pelo uid, para o mover em vez de criar outro.
    private final Map<String, Marker> marcadores = new HashMap<>();

    // Foto de cada amigo (texto Base64, ou null se não tiver), pedida uma só vez.
    private final Map<String, String> fotos = new HashMap<>();

    // Legenda que cada marcador mostra ("visto há 7 min"), ou null se a posição é recente.
    private final Map<String, String> legendas = new HashMap<>();

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

    // Chamado no onMapReady do MenuPrincipalActivity, quando o Google Maps está pronto.
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
                for (int i = 0; i < amigosComPosicao.size(); i++) {
                    Amigo amigo = amigosComPosicao.get(i);
                    uidsComPosicao.add(amigo.getUid());
                    LatLng posicao = new LatLng(amigo.getLatitude(), amigo.getLongitude());
                    Marker marcador = marcadores.get(amigo.getUid());

                    // Posição com mais de 5 minutos: o amigo continua no mapa, mas a cinzento e com "visto há".
                    String legenda = legendaPosicao(amigo);

                    if (marcador == null) {
                        marcador = mapa.addMarker(new MarkerOptions()
                                .position(posicao)
                                .title(amigo.getNome()));
                        marcadores.put(amigo.getUid(), marcador);
                        legendas.put(amigo.getUid(), legenda);

                        // A foto só é pedida uma vez; se já chegou antes, usa-se a guardada.
                        if (fotos.containsKey(amigo.getUid())) {
                            atualizarIcone(amigo.getUid(), marcador);
                        } else {
                            pedirFoto(amigo.getUid(), marcador);
                        }
                    } else {
                        marcador.setPosition(posicao);

                        // Só troca o ícone quando a legenda muda (no máximo uma vez por minuto).
                        String antes = legendas.get(amigo.getUid());
                        if (!legendas.containsKey(amigo.getUid()) || !Objects.equals(antes, legenda)) {
                            legendas.put(amigo.getUid(), legenda);
                            atualizarIcone(amigo.getUid(), marcador);
                        }
                    }
                }

                // Se já não houver localização no Firebase, remove o marcador do mapa.
                // Copia os uids para uma lista, porque o ciclo remove marcadores do Map.
                List<String> uids = new ArrayList<>(marcadores.keySet());
                for (int i = 0; i < uids.size(); i++) {
                    String uid = uids.get(i);
                    if (!uidsComPosicao.contains(uid)) {
                        Marker marcador = marcadores.remove(uid);
                        legendas.remove(uid);
                        if (marcador != null) {
                            marcador.remove();
                        }
                    }
                }
            });
        });
    }

    // Pede a foto do amigo e, quando chega, guarda-a e põe-na no marcador.
    private void pedirFoto(String uid, Marker marcador) {
        new PerfilService().obterFoto(context, uid, fotoBase64 -> {
            if (!ativo || marcador != marcadores.get(uid)) {
                return;
            }
            // fotoBase64 pode ser null: o amigo não tem foto (fica o marcador normal).
            fotos.put(uid, fotoBase64);
            atualizarIcone(uid, marcador);
        });
    }

    // Foto a cores com contorno vermelho (posição recente)
    // ou foto a cinzento com contorno cinzento (posição com mais de 5 minutos).
    private void atualizarIcone(String uid, Marker marcador) {
        // A foto ainda não chegou: o ícone é posto quando chegar (pedirFoto).
        if (!fotos.containsKey(uid)) {
            return;
        }
        String foto = fotos.get(uid);
        String legenda = legendas.get(uid);
        boolean antiga = legenda != null;

        int corContorno;
        if (antiga) {
            corContorno = Color.GRAY;
        } else {
            corContorno = Color.RED;
        }
        marcador.setIcon(IconeFotoHelper.criar(foto, corContorno, antiga, legenda));
    }

    // Texto por cima de um amigo com posição antiga: "visto há 7 min", "visto há 1h 32 min",
    // "visto há 2 dias". Devolve null se a posição tem menos de 5 minutos (não leva texto).
    private String legendaPosicao(Amigo amigo) {
        Instant agora = Instant.now();
        if (amigo.temPosicaoRecente(agora, VALIDADE_POSICAO_MS)) {
            return null;
        }
        Instant hora = amigo.getPosicaoAtualizadaEm();
        if (hora == null) {
            // Posição guardada antes de a hora ser enviada: não se sabe de quando é.
            return "visto há muito tempo";
        }

        long minutos = Duration.between(hora, agora).toMinutes();
        if (minutos < 60) {
            return "visto há " + minutos + " min";
        }
        long horas = minutos / 60;
        if (horas < 24) {
            return "visto há " + horas + "h " + (minutos % 60) + " min";
        }
        long dias = horas / 24;
        if (dias == 1) {
            return "visto há 1 dia";
        }
        return "visto há " + dias + " dias";
    }
}
