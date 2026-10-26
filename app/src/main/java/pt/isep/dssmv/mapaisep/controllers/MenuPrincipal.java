package pt.isep.dssmv.mapaisep.controllers;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.model.Aula;
import pt.isep.dssmv.mapaisep.model.LocalizacaoAmigo;
import pt.isep.dssmv.mapaisep.model.LocalMapa;
import pt.isep.dssmv.mapaisep.services.AmigosService;
import pt.isep.dssmv.mapaisep.services.AuthService;
import pt.isep.dssmv.mapaisep.services.BibliotecaService;
import pt.isep.dssmv.mapaisep.services.DetetorPresenca;
import pt.isep.dssmv.mapaisep.services.HorarioService;
import pt.isep.dssmv.mapaisep.services.LocalizacaoService;
import pt.isep.dssmv.mapaisep.services.LocaisService;
import pt.isep.dssmv.mapaisep.services.SessionManager;

public class MenuPrincipal extends AppCompatActivity implements OnMapReadyCallback {
    // ISEP campus location.
    private static final LatLng ISEP = new LatLng(41.178194, -8.607083);
    // A biblioteca é um ponto fixo e fica sempre visível no mapa.
    private static final LatLng BIBLIOTECA_ISEP = new LatLng(41.178766, -8.606559);

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

    private GoogleMap mapa;

    private ActivityResultLauncher<String[]> permissaoLocalizacao;
    private LocationManager gestorLocalizacao;
    private LocationListener ouvinteLocalizacao;
    private boolean atualizacoesLocalizacaoAtivas;
    private final LocalizacaoService localizacaoService = new LocalizacaoService();
    private final LocaisService locaisService = new LocaisService();
    private final Map<String, Marker> marcadoresAmigos = new HashMap<>();
    private final Map<String, Marker> marcadoresLocais = new HashMap<>();
    private Marker marcadorBiblioteca;
    private List<LocalMapa> locaisCampus = new ArrayList<>();
    private List<Aula> aulasHoje = new ArrayList<>();
    private final Handler handlerAmigos = new Handler(Looper.getMainLooper());
    private boolean mapaAberto;
    private Location ultimaLocalizacaoPartilhada;
    private long ultimoEnvioLocalizacao;

    private final Runnable atualizarAmigos = new Runnable() {
        @Override
        public void run() {
            consultarLocalizacoesAmigos();
            if (mapaAberto) {
                handlerAmigos.postDelayed(this, 5000);
            }
        }
    };

    // Deteta, pelo Wi-Fi, se o aluno está na biblioteca e atualiza a presença no Firestore.
    private DetetorPresenca detetorPresenca;

    // true quando o pedido de permissão veio do botão da biblioteca (e não do mapa).
    private boolean pedidoDaBiblioteca;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check whether the student has an active session in memory.
        if (AuthService.getIdToken() == null
                || !SessionManager.sessaoValida(this)) {

            AuthService.logout();
            SessionManager.terminarSessao(this);

            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        gestorLocalizacao = (LocationManager) getSystemService(LOCATION_SERVICE);
        ouvinteLocalizacao = new LocationListener() {
            @Override
            public void onLocationChanged(Location localizacao) {
                atualizarPontoLocalizacao(localizacao);
            }
        };

        setContentView(R.layout.menu_principal);

        carregarHorario();
        carregarLotacaoBiblioteca();

        // No fim de cada ciclo do detetor, volta a pedir a lotação (o número no ecrã acompanha quem entra e sai).
        detetorPresenca = new DetetorPresenca(this, new Runnable() {
            @Override
            public void run() {
                carregarLotacaoBiblioteca();
            }
        });

        // Register the location permission callback.
        permissaoLocalizacao = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultados -> {
            if (temPermissaoLocalizacao()) {
                ativarLocalizacao();
            } else {
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
            }

            if (temLocalizacaoPrecisa()) {
                // Já pode ver as redes Wi-Fi: liga o detetor e o cartão passa a mostrar o número.
                detetorPresenca.iniciar();
                carregarLotacaoBiblioteca();
            } else if (pedidoDaBiblioteca
                    && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                // O Android já não mostra a janela ("não perguntar novamente"): abre as definições da app.
                Intent definicoes = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null));
                startActivity(definicoes);
            }
            pedidoDaBiblioteca = false;
        });

        // Botão do cartão da biblioteca: pede a localização precisa.
        findViewById(R.id.btnPermissaoBiblioteca).setOnClickListener(v -> {
            pedidoDaBiblioteca = true;
            permissaoLocalizacao.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION });
        });

        // Get references to the home and map pages.
        View paginaInicio = findViewById(R.id.paginaInicio);
        View paginaMapa = findViewById(R.id.paginaMapa);
        mostrarPagina(SessionManager.ultimaPaginaEraMapa(this), paginaInicio, paginaMapa);

        // Find the Google Maps fragment in the layout.
        SupportMapFragment fragmentoMapa = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapaGoogle);

        if (fragmentoMapa != null) {
            fragmentoMapa.getMapAsync(this);
        }

        // Show the home page.
        findViewById(R.id.btnInicio).setOnClickListener(v -> {

            mostrarPagina(false, paginaInicio, paginaMapa);
        });

        // Show the map page.
        findViewById(R.id.btnMapa).setOnClickListener(v -> {

            mostrarPagina(true, paginaInicio, paginaMapa);
        });

        // Configure the account menu.
        ImageButton btnMenu = findViewById(R.id.btnMenu);

        btnMenu.setOnClickListener(anchor -> {
            PopupMenu menuConta = new PopupMenu(this, anchor);

            menuConta.getMenuInflater().inflate(R.menu.menu_account, menuConta.getMenu());

            menuConta.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_home) {
                    mostrarPagina(false, paginaInicio, paginaMapa);
                    return true;
                }

                if (item.getItemId() == R.id.action_profile) {
                    startActivity(new Intent(this, PerfilActivity.class));
                    return true;
                }

                if (item.getItemId() == R.id.action_friends) {
                    startActivity(new Intent(this, AmigosActivity.class));
                    return true;
                }

                if (item.getItemId() == R.id.action_logout) {
                    terminarSessao();
                    return true;
                }

                return false;
            });

            menuConta.show();
        });
    }

    // Pede o horário ao HorarioService e mostra-o na página Início.
    private void carregarHorario() {
        TextView textoAulas = findViewById(R.id.textoAulas);

        // Mensagem mostrada enquanto o pedido HTTP decorre.
        textoAulas.setText(R.string.horario_loading);

        // O serviço vai buscar os dados; a Activity só os mostra.
        HorarioService horarioService = new HorarioService();

        // Callback: diz o que fazer quando o serviço tiver o horário.
        // Criar este objeto não executa o onResult().
        HorarioService.HorarioCallback callback = new HorarioService.HorarioCallback() {

            // Chamado pelo serviço na main thread (através do Handler),
            // por isso pode alterar a interface.
            @Override
            public void onResult(List<Aula> resultado, String erro) {
                // Ignore results if the Activity was closed.
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // Handle errors returned by HorarioService.
                if (erro != null) {
                    if (erro.equals("NOT_AUTHENTICATED") || erro.equals("HTTP_401")) {
                        Toast.makeText(MenuPrincipal.this, R.string.session_expired, Toast.LENGTH_LONG).show();
                        terminarSessao();
                    } else if (erro.equals("HTTP_403")) {
                        textoAulas.setText(R.string.horario_error_permission);
                    } else if (erro.equals("HTTP_404")) {
                        textoAulas.setText(R.string.horario_error_student);
                    } else if (erro.equals("INVALID_LESSON_DATA")) {
                        textoAulas.setText(R.string.horario_error_data);
                    } else {
                        textoAulas.setText(R.string.horario_error_generic);
                    }
                    return;
                }

                // A próxima aula será usada no mapa para destacar a sala correta.
                aulasHoje = new ArrayList<>(resultado);
                desenharMarcadoresLocais();

                // Display a message if there are no lessons today.
                if (resultado.isEmpty()) {
                    textoAulas.setText(R.string.horario_empty);
                    return;
                }

                // Build the text displayed in the timetable.
                StringBuilder horario = new StringBuilder();

                for (int i = 0; i < resultado.size(); i++) {
                    Aula item = resultado.get(i);

                    if (horario.length() > 0) {
                        horario.append("\n\n");
                    }

                    // Hora de início, hora de fim, disciplina e sala.
                    horario.append(getString(R.string.horario_lesson_details, item.getHoraInicio().toString(), item.getHoraFim().toString(),
                            item.getDisciplina(), item.getSala().getCodigo()));
                }

                // Update the TextView with the retrieved lessons.
                textoAulas.setText(horario.toString());
            }
        };

        // Passa o callback ao serviço. O serviço faz o pedido numa thread
        // secundária e este método retorna logo, sem esperar pelo Firestore.
        horarioService.obterAulasHoje(this, callback);
    }

    // Mantém o pedido da lotação da biblioteca iniciado quando o menu abre.
    private void carregarLotacaoBiblioteca() {
        // Caixa de texto da biblioteca (menu_principal.xml).
        TextView textoBiblioteca = findViewById(R.id.textoBiblioteca);
        Button btnPermissao = findViewById(R.id.btnPermissaoBiblioteca);

        // Sem localização precisa: mostra a mensagem e o botão em vez da lotação.
        if (!temLocalizacaoPrecisa()) {
            textoBiblioteca.setText(R.string.biblioteca_sem_permissao);
            btnPermissao.setVisibility(View.VISIBLE);
            return;
        }
        btnPermissao.setVisibility(View.GONE);

        // Mensagem mostrada enquanto o pedido HTTP decorre.
        textoBiblioteca.setText(R.string.biblioteca_loading);

        BibliotecaService bibliotecaService = new BibliotecaService();
        // Callback: diz o que fazer quando o BibliotecaService tiver a lotação.
        BibliotecaService.BibliotecaCallback callback = new BibliotecaService.BibliotecaCallback() {
            //apenas dizemos oque fazer quando a bibliotecaService tiver a lotacao
            //ainda temos de chamar um metodo para realmente carregar
            @Override
            public void onResult(Integer lotacaoEstimativa, String erro) {
                // Se o ecrã já foi fechado, não há onde mostrar o resultado.
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (erro != null) {
                    textoBiblioteca.setText(R.string.biblioteca_indisponivel);
                    return;
                }

                // Substitui o %1$d do texto pelo número recebido.
                textoBiblioteca.setText(getString(R.string.biblioteca_lotacao, lotacaoEstimativa));
            }
        };
        bibliotecaService.obterLotacao(callback);
        //aqui entregamos a implementacao
        //ou seja; pedimos ao service para ir buscar a informacao e depois realizar a implementacao implementada anteriormente
    }

    // Ends the current authentication session.
    private void terminarSessao() {
        // Remove authentication information from application memory.
        AuthService.logout();
        SessionManager.terminarSessao(this);

        Intent irParaLogin = new Intent(this, LoginActivity.class);

        // Clear the activity navigation history.
        irParaLogin.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(irParaLogin);
        finish();
    }

    // Called when Google Maps becomes available.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;

        mapa.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mapa.setBuildingsEnabled(true);
        mapa.setLatLngBoundsForCameraTarget(CAMPUS_ISEP);
        mapa.setMinZoomPreference(16f);
        mapa.setMaxZoomPreference(21f);

        mapa.getUiSettings().setZoomControlsEnabled(true);

        adicionarMascaraExterior();

        desenharBiblioteca();
        carregarLocais();

        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.17827, -8.60727), 19f));

        if (temPermissaoLocalizacao()) {
            ativarLocalizacao();
        }
    }

    // Adds a white mask outside the campus boundary.
    private void adicionarMascaraExterior() {
        List<LatLng> limiteExterior = Arrays.asList(new LatLng(41.25, -8.70), new LatLng(41.25, -8.50), new LatLng(41.10, -8.50),
                new LatLng(41.10, -8.70));

        mapa.addPolygon(new PolygonOptions().addAll(limiteExterior).addHole(CONTORNO_ISEP).strokeWidth(0f).fillColor(Color.WHITE).clickable(false));
    }

    // A biblioteca não depende do horário: é sempre um ponto de interesse do campus.
    private void desenharBiblioteca() {
        if (mapa == null || marcadorBiblioteca != null) {
            return;
        }

        marcadorBiblioteca = mapa.addMarker(new MarkerOptions()
                .position(BIBLIOTECA_ISEP)
                .title("Biblioteca ISEP")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
    }

    // Obtém as salas e a biblioteca guardadas na coleção Firestore locais.
    private void carregarLocais() {
        locaisService.obterLocais(this, (resultado, erro) -> {
            if (mapa == null) {
                return;
            }

            if (erro != null) {
                Toast.makeText(this, "Não foi possível carregar os locais do mapa (" + erro + ")", Toast.LENGTH_LONG).show();
                return;
            }

            if (resultado == null || resultado.isEmpty()) {
                Toast.makeText(this, "Não existem locais válidos para mostrar no mapa.", Toast.LENGTH_LONG).show();
                return;
            }

            locaisCampus = resultado;
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

        String idProximaSala = proxima.getSala().getCodigo();
        for (LocalMapa local : locaisCampus) {
            if (!local.getId().equals(idProximaSala)) {
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

    // Requests location permissions if necessary.
    private void pedirPermissaoOuAtivarLocalizacao() {
        if (temPermissaoLocalizacao()) {
            ativarLocalizacao();
        } else {
            permissaoLocalizacao.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }
            );
        }
    }

    // Checks whether location permissions were granted.
    // Só a localização precisa (FINE) deixa ver as redes Wi-Fi.
    private boolean temLocalizacaoPrecisa() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean temPermissaoLocalizacao() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Enables the user's location on Google Maps.
    @SuppressWarnings("MissingPermission")
    private void ativarLocalizacao() {
        if (mapa != null && temPermissaoLocalizacao()) {
            mapa.setMyLocationEnabled(true);

            mapa.getUiSettings()
                    .setMyLocationButtonEnabled(true);

            iniciarAtualizacoesLocalizacao();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void iniciarAtualizacoesLocalizacao() {

        if (gestorLocalizacao == null
                || atualizacoesLocalizacaoAtivas
                || !temPermissaoLocalizacao()) {
            return;
        }

        boolean fornecedorAtivo = false;

        if (gestorLocalizacao.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gestorLocalizacao.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    1f,
                    ouvinteLocalizacao,
                    Looper.getMainLooper()
            );
            fornecedorAtivo = true;
        }

        if (gestorLocalizacao.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            gestorLocalizacao.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000,
                    1f,
                    ouvinteLocalizacao,
                    Looper.getMainLooper()
            );
            fornecedorAtivo = true;
        }

        atualizacoesLocalizacaoAtivas = fornecedorAtivo;

        Location ultimaPosicao = gestorLocalizacao.getLastKnownLocation(
                LocationManager.GPS_PROVIDER
        );

        if (ultimaPosicao == null) {
            ultimaPosicao = gestorLocalizacao.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER
            );
        }

        if (ultimaPosicao != null) {
            atualizarPontoLocalizacao(ultimaPosicao);
        }
    }

    private void atualizarPontoLocalizacao(Location localizacao) {

        if (mapa == null) {
            return;
        }

        // O marcador nativo do Google Maps atualiza-se automaticamente.
        // Usamos esta posição apenas para a partilhar com os amigos.
        if (mapaAberto && deveEnviarLocalizacao(localizacao)) {
            localizacaoService.atualizarMinhaLocalizacao(
                    this, localizacao.getLatitude(), localizacao.getLongitude()
            );
            ultimaLocalizacaoPartilhada = new Location(localizacao);
            ultimoEnvioLocalizacao = System.currentTimeMillis();
        }
    }

    // Evita enviar um PATCH por cada pequena variação do GPS.
    private boolean deveEnviarLocalizacao(Location localizacao) {
        return ultimaLocalizacaoPartilhada == null
                || ultimaLocalizacaoPartilhada.distanceTo(localizacao) >= 3f
                || System.currentTimeMillis() - ultimoEnvioLocalizacao >= 5000;
    }

    private void iniciarConsultaAmigos() {
        handlerAmigos.removeCallbacks(atualizarAmigos);
        handlerAmigos.post(atualizarAmigos);
    }

    private void pararConsultaAmigos() {
        handlerAmigos.removeCallbacks(atualizarAmigos);
    }

    private void consultarLocalizacoesAmigos() {
        if (!mapaAberto || mapa == null) {
            return;
        }

        new AmigosService().listarAmigos(this, (amigos, erro) -> {
            if (erro != null || amigos == null || !mapaAberto) {
                return;
            }

            localizacaoService.obterLocalizacoesAmigos(this, amigos, (localizacoes, erroLocalizacao) -> {
                if (erroLocalizacao != null || localizacoes == null || !mapaAberto || mapa == null) {
                    return;
                }

                Set<String> amigosComPosicao = new HashSet<>();
                for (LocalizacaoAmigo localizacao : localizacoes) {
                    amigosComPosicao.add(localizacao.getUid());
                    LatLng posicao = new LatLng(localizacao.getLatitude(), localizacao.getLongitude());
                    Marker marcador = marcadoresAmigos.get(localizacao.getUid());

                    if (marcador == null) {
                        marcador = mapa.addMarker(new MarkerOptions()
                                .position(posicao)
                                .title(localizacao.getNome()));
                        marcadoresAmigos.put(localizacao.getUid(), marcador);
                    } else {
                        marcador.setPosition(posicao);
                    }
                }

                // Se já não houver localização no Firebase, remove o marcador do mapa.
                for (String uid : new HashSet<>(marcadoresAmigos.keySet())) {
                    if (!amigosComPosicao.contains(uid)) {
                        Marker marcador = marcadoresAmigos.remove(uid);
                        if (marcador != null) {
                            marcador.remove();
                        }
                    }
                }
            });
        });
    }

    @Override
    protected void onPause() {
        SessionManager.registarAtividade(this);
        pararConsultaAmigos();
        super.onPause();
        pararAtualizacoesLocalizacao();

        // App em fundo: pára os scans de Wi-Fi.
        if (detetorPresenca != null) {
            detetorPresenca.parar();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AuthService.getIdToken() == null
                || !SessionManager.sessaoValida(this)) {
            terminarSessao();
            return;
        }

        SessionManager.registarAtividade(this);
        ativarLocalizacao();

        // App aberta: começa os scans de Wi-Fi (ver as redes precisa da localização precisa).
        if (detetorPresenca != null && temLocalizacaoPrecisa()) {
            detetorPresenca.iniciar();
        }

        if (mapaAberto) {
            iniciarConsultaAmigos();
        }
    }

    private void pararAtualizacoesLocalizacao() {

        if (gestorLocalizacao != null && atualizacoesLocalizacaoAtivas) {
            gestorLocalizacao.removeUpdates(ouvinteLocalizacao);
            atualizacoesLocalizacaoAtivas = false;
        }
    }

    private void mostrarPagina(boolean mostrarMapa, View paginaInicio, View paginaMapa) {
        paginaInicio.setVisibility(mostrarMapa ? View.GONE : View.VISIBLE);
        paginaMapa.setVisibility(mostrarMapa ? View.VISIBLE : View.GONE);
        SessionManager.guardarPaginaMapa(this, mostrarMapa);
        mapaAberto = mostrarMapa;

        if (mostrarMapa) {
            pedirPermissaoOuAtivarLocalizacao();
            iniciarConsultaAmigos();
        } else {
            pararConsultaAmigos();
        }
    }
}
