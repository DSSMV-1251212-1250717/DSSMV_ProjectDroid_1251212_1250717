package pt.isep.dssmv.mapaisep.controllers;

import android.content.Context;
import android.graphics.Color;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.isep.dssmv.mapaisep.model.Aula;
import pt.isep.dssmv.mapaisep.model.Local;
import pt.isep.dssmv.mapaisep.services.LocaisService;

// O mapa do campus: limites, máscara à volta, biblioteca e sala da aula atual/próxima.
// O MenuPrincipal chama configurar() no onMapReady e definirAulas() quando o horário chega.
public class MapaCampus {
    // ISEP campus location.
    private static final LatLng ISEP = new LatLng(41.178194, -8.607083);

    // Geographic limits of the campus.
    private static final LatLngBounds CAMPUS_ISEP = new LatLngBounds(new LatLng(41.1770955, -8.6091907), new LatLng(41.1795948, -8.6056156));

    // Coordinates defining the campus contour.
    private static final List<LatLng> CONTORNO_ISEP = Arrays.asList(
        new LatLng(41.1788413, -8.6091907),
        new LatLng(41.1787122, -8.6091844),
        new LatLng(41.1781764, -8.6091645),
        new LatLng(41.1775921, -8.6091508),
        new LatLng(41.1774962, -8.6091237),
        new LatLng(41.1774235, -8.60907),
        new LatLng(41.1773791, -8.608952),
        new LatLng(41.1772868, -8.6086727),
        new LatLng(41.177153, -8.6083029),
        new LatLng(41.1771157, -8.6081637),
        new LatLng(41.1770955, -8.6080457),
        new LatLng(41.1771197, -8.6079492),
        new LatLng(41.1771682, -8.6078687),
        new LatLng(41.1775591, -8.6074688),
        new LatLng(41.1778014, -8.6072181),
        new LatLng(41.1778975, -8.6071315),
        new LatLng(41.1782186, -8.6067965),
        new LatLng(41.1782727, -8.606743),
        new LatLng(41.1783187, -8.6066959),
        new LatLng(41.1787766, -8.6062276),
        new LatLng(41.1788154, -8.6061922),
        new LatLng(41.1788431, -8.6061662),
        new LatLng(41.1788603, -8.6061505),
        new LatLng(41.1790416, -8.6059858),
        new LatLng(41.1793091, -8.6057709),
        new LatLng(41.1795948, -8.6056156),
        new LatLng(41.1795477, -8.607945),
        new LatLng(41.1795443, -8.6081136),
        new LatLng(41.1795441, -8.608126),
        new LatLng(41.1795261, -8.6088933),
        new LatLng(41.1791749, -8.6088826),
        new LatLng(41.1788569, -8.6087911)
    );

    private final Context context;
    private final LocaisService locaisService = new LocaisService();
    private final Map<String, Marker> marcadoresLocais = new HashMap<>();

    private GoogleMap mapa;
    private Marker marcadorBiblioteca;
    private List<Local> locaisCampus = new ArrayList<>();
    private List<Aula> aulasHoje = new ArrayList<>();

    public MapaCampus(Context context) {
        this.context = context;
    }

    // Chamado no onMapReady do MenuPrincipal: prepara o mapa e desenha o campus.
    public void configurar(GoogleMap googleMap) {
        mapa = googleMap;

        mapa.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mapa.setBuildingsEnabled(true);
        mapa.setLatLngBoundsForCameraTarget(CAMPUS_ISEP);
        mapa.setMinZoomPreference(16f);
        mapa.setMaxZoomPreference(21f);

        mapa.getUiSettings().setZoomControlsEnabled(true);

        adicionarMascaraExterior();

        // Salas e biblioteca vêm da coleção locais; são desenhadas quando chegarem.
        carregarLocais();

        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.17827, -8.60727), 19f));
    }

    // Chamado quando o horário de hoje chega: a próxima aula é usada para destacar a sala.
    public void definirAulas(List<Aula> aulas) {
        aulasHoje = new ArrayList<>(aulas);
        desenharMarcadoresLocais();
    }

    // Adds a white mask outside the campus boundary.
    private void adicionarMascaraExterior() {
        List<LatLng> limiteExterior = Arrays.asList(new LatLng(41.25, -8.70), new LatLng(41.25, -8.50), new LatLng(41.10, -8.50),
                new LatLng(41.10, -8.70));

        mapa.addPolygon(new PolygonOptions().addAll(limiteExterior).addHole(CONTORNO_ISEP).strokeWidth(0f).fillColor(Color.WHITE).clickable(false));
    }

    // A biblioteca vem da coleção locais (documento "Biblioteca"), como as salas.
    // Não depende do horário: aparece sempre, a azul.
    private void desenharBiblioteca() {
        if (mapa == null || marcadorBiblioteca != null) {
            return;
        }

        for (Local local : locaisCampus) {
            if (local.getId().equals("Biblioteca")) {
                marcadorBiblioteca = mapa.addMarker(new MarkerOptions()
                        .position(new LatLng(local.getLatitude(), local.getLongitude()))
                        .title(local.getNome())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                return;
            }
        }
    }

    // Obtém as salas e a biblioteca guardadas na coleção Firestore locais.
    private void carregarLocais() {
        locaisService.obterLocais(context, (resultado, erro) -> {
            if (mapa == null) {
                return;
            }

            if (erro != null) {
                Toast.makeText(context, "Não foi possível carregar os locais do mapa (" + erro + ")", Toast.LENGTH_LONG).show();
                return;
            }

            if (resultado == null || resultado.isEmpty()) {
                Toast.makeText(context, "Não existem locais válidos para mostrar no mapa.", Toast.LENGTH_LONG).show();
                return;
            }

            locaisCampus = resultado;
            desenharBiblioteca();
            desenharMarcadoresLocais();
        });
    }

    // Só desenha a sala da aula atual ou próxima. A biblioteca é desenhada à parte.
    private void desenharMarcadoresLocais() {
        if (mapa == null) {
            return;
        }

        Aula proxima = obterProximaAula();

        for (Marker marcador : marcadoresLocais.values()) {
            marcador.remove();
        }
        marcadoresLocais.clear();

        if (proxima == null || locaisCampus.isEmpty()) {
            return;
        }

        String idProximaSala = proxima.getSalaId();
        for (Local local : locaisCampus) {
            // O ID vem do horário e do documento Firestore. Ignoramos apenas
            // diferenças acidentais de maiúsculas ou espaços (por exemplo,
            // "I203 " em vez de "I203").
            if (!local.getId().trim().equalsIgnoreCase(idProximaSala.trim())) {
                continue;
            }

            String estado = proxima.estaADecorrer(java.time.LocalTime.now())
                    ? "Aula atual: "
                    : "Próxima aula: ";
            Marker marcador = mapa.addMarker(new MarkerOptions()
                    .position(new LatLng(local.getLatitude(), local.getLongitude()))
                    .title(estado + proxima.getDisciplina()
                            + " — " + local.getNome()
                            + " às " + proxima.getHoraInicio())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            marcadoresLocais.put(local.getId(), marcador);
            return;
        }
    }

    // A lista vem ordenada por horaInicio: usa a aula atual ou, se não existir, a próxima.
    private Aula obterProximaAula() {
        java.time.LocalTime agora = java.time.LocalTime.now();
        for (Aula aula : aulasHoje) {
            if (aula.estaADecorrer(agora) || !aula.getHoraInicio().isBefore(agora)) {
                return aula;
            }
        }
        return null;
    }
}
