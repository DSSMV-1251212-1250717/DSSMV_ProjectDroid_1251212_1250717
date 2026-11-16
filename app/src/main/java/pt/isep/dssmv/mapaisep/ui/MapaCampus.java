package pt.isep.dssmv.mapaisep.ui;

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
import pt.isep.dssmv.mapaisep.service.HorarioService;
import pt.isep.dssmv.mapaisep.service.LocaisService;

// O mapa do campus: limites, máscara à volta, biblioteca e sala da aula atual/próxima.
// O MenuPrincipalActivity chama configurar() no onMapReady e definirAulas() quando o horário chega.
public class MapaCampus {
    // Centro do campus do ISEP.
    private static final LatLng ISEP = new LatLng(41.178194, -8.607083);

    // Limites do campus (a câmara não sai daqui).
    private static final LatLngBounds CAMPUS_ISEP = new LatLngBounds(new LatLng(41.1770955, -8.6091907), new LatLng(41.1795948, -8.6056156));
    //CAMPUS_ISEP é um retângulo

    // Pontos do contorno do campus.
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
    //guarda o pin vermelho da sala que está no mapa (id da sala -> pin), para o poder apagar depois

    private GoogleMap mapa;
    private Marker marcadorBiblioteca;
    private List<Local> locaisCampus = new ArrayList<>();
    private List<Aula> aulasHoje = new ArrayList<>();

    public MapaCampus(Context context) {
        this.context = context;
    }

    // Chamado no onMapReady do MenuPrincipalActivity: prepara o mapa e desenha o campus.
    public void configurar(GoogleMap googleMap) {
        mapa = googleMap;
        //guarda o mapa

        mapa.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        //vista de satélite
        mapa.setBuildingsEnabled(true);
        //mostra os edifícios em 3D
        mapa.setLatLngBoundsForCameraTarget(CAMPUS_ISEP);
        //a câmara não sai do campus
        mapa.setMinZoomPreference(16f);
        mapa.setMaxZoomPreference(21f);
        //limita o zoom entre 16 e 21

        mapa.getUiSettings().setZoomControlsEnabled(true);
        //mostra os botões + e -

        adicionarMascaraExterior();
        //pinta de branco tudo fora do campus

        // Salas e biblioteca vêm da coleção locais; são desenhadas quando chegarem.
        carregarLocais();
        //vai buscar as salas e a biblioteca à Firebase

        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.17827, -8.60727), 19f));
        //põe a câmara no centro do ISEP, com zoom 19
    }

    // Chamado quando o horário de hoje chega: a próxima aula é usada para destacar a sala.
    public void definirAulas(List<Aula> aulas) {
        aulasHoje = new ArrayList<>(aulas);
        //guarda as aulas de hoje (vêm do MenuPrincipalActivity quando o horário chega)
        desenharMarcadoresLocais();
        //desenha o pin vermelho da sala, agora que já se sabe a próxima aula
    }

    // Pinta de branco tudo o que fica fora do campus.
    private void adicionarMascaraExterior() {
        List<LatLng> limiteExterior = Arrays.asList(new LatLng(41.25, -8.70), new LatLng(41.25, -8.50), new LatLng(41.10, -8.50),
                new LatLng(41.10, -8.70));

        mapa.addPolygon(new PolygonOptions().addAll(limiteExterior).addHole(CONTORNO_ISEP).strokeWidth(0f).fillColor(Color.WHITE).clickable(false));
    }

    // A biblioteca vem da coleção locais (documento "Biblioteca"), como as salas.
    // Não depende do horário: aparece sempre, a azul.
    private void desenharBiblioteca() {
        if (mapa == null || marcadorBiblioteca != null) {
            //sem mapa não há onde desenhar; se o pin já existe, não desenha outro
            return;
        }

        for (int i = 0; i < locaisCampus.size(); i++) {
            //percorre os locais que vieram da Firebase
            Local local = locaisCampus.get(i);
            if (local.getId().equals("Biblioteca")) {
                //encontrou a biblioteca
                marcadorBiblioteca = mapa.addMarker(new MarkerOptions()
                        //add Marker é um metodo do GOogleMap que poe um marcador
                        .position(new LatLng(local.getLatitude(), local.getLongitude()))
                        //onde fica: as coordenadas da biblioteca
                        .title(local.getNome())
                        //texto ao tocar no pin
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                        //pin normal da Google, pintado de azul
                //guarda o pin no marcadorBiblioteca e sai (só há uma biblioteca)
                return;
            }
        }
    }

    // Obtém as salas e a biblioteca guardadas na coleção Firestore locais.
    private void carregarLocais() {
        locaisService.obterLocais(context, (resultado, erro) -> {
            //todos os locais
            //o LocaisService faz um GET à coleção locais (salas e biblioteca, com as coordenadas)
            //resultado, erro: os dois valores que o service entrega quando acaba
            //preenche locaisCampus;
            if (mapa == null) {
                //o mapa já não existe: não há onde desenhar
                return;
            }

            if (erro != null) {
                //o pedido falhou: mostra o erro
                Toast.makeText(context, "Não foi possível carregar os locais do mapa (" + erro + ")", Toast.LENGTH_LONG).show();
                return;
            }

            if (resultado == null || resultado.isEmpty()) {
                //não veio nenhum local
                Toast.makeText(context, "Não existem locais válidos para mostrar no mapa.", Toast.LENGTH_LONG).show();
                return;
            }

            locaisCampus = resultado;
            //guarda a lista de locais (salas e biblioteca)
            desenharBiblioteca();
            //desenharBiblioteca(): procura na lista o local “Biblioteca” e põe lá um pin azul.
            desenharMarcadoresLocais();
            //desenharMarcadoresLocais(): vê qual é a aula atual ou a próxima,
            // procura na lista a sala dessa aula e põe lá um pin vermelho.
        });
    }

    // Só desenha a sala da aula atual ou próxima. A biblioteca é desenhada à parte.
    private void desenharMarcadoresLocais() {
        if (mapa == null) {
            return;
        }

        // Escolher a próxima aula é lógica sobre aulas: está no HorarioService.
        Aula proxima = HorarioService.obterProximaAula(aulasHoje, java.time.LocalTime.now());
        //a aula que está a decorrer agora ou, se não houver, a próxima de hoje (null se já não há)

            //marcadoresLocais: o Map (id da sala -> pin) com o pin vermelho que está no mapa agora
            List<Marker> marcadoresAntigos = new ArrayList<>(marcadoresLocais.values());
            //Usa-se a lista porque o for com i e get(i) não funciona num Maps
            //o pino guardado no mapa
            //tiramos dos marcadores locais para os antigos
            for (int i = 0; i < marcadoresAntigos.size(); i++) {
                marcadoresAntigos.get(i).remove();
                //tira cada pin antigo do mapa (desaparece do ecrã)
                //tiramos dos antigos
            }
            marcadoresLocais.clear();
            //esvazia o Map: já não há pin vermelho guardado
            //apaga o pin vermelho antigo (do mapa e da lista), para não ficarem dois

        if (proxima == null || locaisCampus.isEmpty()) {
            //locais campus sao todos os locais que vieram da firebase
            //sem aula ou sem locais: não há sala para marcar
            return;
        }

        String idProximaSala = proxima.getSalaId();
        //a sala dessa aula, por exemplo "I203"
        for (int i = 0; i < locaisCampus.size(); i++) {
            Local local = locaisCampus.get(i);
            // O ID vem do horário e do documento Firestore. Ignoramos apenas
            // diferenças acidentais de maiúsculas ou espaços (por exemplo,
            // "I203 " em vez de "I203").
            if (!local.getId().trim().equalsIgnoreCase(idProximaSala.trim())) {
                //não é a sala da aula: passa ao local seguinte
                continue;
            }
            //encontrou a sala da aula

            String estado;
            if (proxima.estaADecorrer(java.time.LocalTime.now())) {
                estado = "Aula atual: ";
            } else {
                estado = "Próxima aula: ";
            }
            //já começou -> "Aula atual: "; ainda não -> "Próxima aula: "
            Marker marcador = mapa.addMarker(new MarkerOptions()
                    .position(new LatLng(local.getLatitude(), local.getLongitude()))
                    .title(estado + proxima.getDisciplina()
                            + " — " + local.getNome()
                            + " às " + proxima.getHoraInicio())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            //põe o pin vermelho na sala; o título aparece ao tocar (ex.: "Próxima aula: DSSMV — I203 às 14:00")
            marcadoresLocais.put(local.getId(), marcador);
            //guarda o pin, para o apagar quando a aula mudar
            return;
        }
    }
}
