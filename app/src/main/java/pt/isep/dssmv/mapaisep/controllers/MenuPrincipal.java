package pt.isep.dssmv.mapaisep.controllers;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.model.Aula;
import pt.isep.dssmv.mapaisep.services.AuthService;
import pt.isep.dssmv.mapaisep.services.HorarioService;
import pt.isep.dssmv.mapaisep.services.SessionManager;

// Ecrã principal: página Início (horário e biblioteca) e página Mapa.
// Cada tarefa grande está numa classe própria; aqui só se criam, ligam e desligam:
//   MapaCampus            -> campus, biblioteca e sala da próxima aula no mapa
//   LocalizacaoUtilizador -> o teu ponto no mapa e o envio da tua posição
//   AmigosNoMapa          -> posição dos amigos (de 5 em 5 segundos)
//   CartaoBiblioteca      -> lotação e deteção de presença por Wi-Fi
public class MenuPrincipal extends AppCompatActivity implements OnMapReadyCallback {

    private final MapaCampus mapaCampus = new MapaCampus(this);
    private final AmigosNoMapa amigosNoMapa = new AmigosNoMapa(this);

    // Criadas no onCreate (precisam do ecrã já carregado / dos serviços do Android).
    private LocalizacaoUtilizador localizacao;
    private CartaoBiblioteca cartaoBiblioteca;

    private ActivityResultLauncher<String[]> permissaoLocalizacao;
    private boolean mapaAberto;

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

        localizacao = new LocalizacaoUtilizador(this);

        setContentView(R.layout.menu_principal);

        carregarHorario();

        // Botão "Ativar localização" do cartão: pede a localização precisa.
        cartaoBiblioteca = new CartaoBiblioteca(this, () -> {
            pedidoDaBiblioteca = true;
            permissaoLocalizacao.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION });
        });
        cartaoBiblioteca.carregarLotacao();

        // Register the location permission callback.
        permissaoLocalizacao = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultados -> {
            if (LocalizacaoUtilizador.temPermissaoLocalizacao(this)) {
                localizacao.ativar();
            } else {
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
            }

            if (LocalizacaoUtilizador.temLocalizacaoPrecisa(this)) {
                // Já pode ver as redes Wi-Fi: liga o detetor e o cartão passa a mostrar o número.
                cartaoBiblioteca.iniciar();
                cartaoBiblioteca.carregarLotacao();
            } else if (pedidoDaBiblioteca
                    && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                // O Android já não mostra a janela ("não perguntar novamente"): abre as definições da app.
                Intent definicoes = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null));
                startActivity(definicoes);
            }
            pedidoDaBiblioteca = false;
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
                mapaCampus.definirAulas(resultado);

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
                            item.getDisciplina(), item.getSalaId()));
                }

                // Update the TextView with the retrieved lessons.
                textoAulas.setText(horario.toString());
            }
        };

        // Passa o callback ao serviço. O serviço faz o pedido numa thread
        // secundária e este método retorna logo, sem esperar pelo Firestore.
        horarioService.obterAulasHoje(this, callback);
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

    // Called when Google Maps becomes available: entrega o mapa a cada peça.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        amigosNoMapa.definirMapa(googleMap);
        localizacao.definirMapa(googleMap);
        mapaCampus.configurar(googleMap);

        if (LocalizacaoUtilizador.temPermissaoLocalizacao(this)) {
            localizacao.ativar();
        }
    }

    // Requests location permissions if necessary.
    private void pedirPermissaoOuAtivarLocalizacao() {
        if (LocalizacaoUtilizador.temPermissaoLocalizacao(this)) {
            localizacao.ativar();
        } else {
            permissaoLocalizacao.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }
            );
        }
    }

    @Override
    protected void onPause() {
        SessionManager.registarAtividade(this);
        amigosNoMapa.parar();
        super.onPause();

        if (localizacao != null) {
            localizacao.parar();
        }

        // App em fundo: pára os scans de Wi-Fi.
        if (cartaoBiblioteca != null) {
            cartaoBiblioteca.parar();
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
        localizacao.ativar();

        // App aberta: começa os scans de Wi-Fi (só com localização precisa).
        cartaoBiblioteca.iniciar();

        if (mapaAberto) {
            amigosNoMapa.iniciar();
        }
    }

    private void mostrarPagina(boolean mostrarMapa, View paginaInicio, View paginaMapa) {
        paginaInicio.setVisibility(mostrarMapa ? View.GONE : View.VISIBLE);
        paginaMapa.setVisibility(mostrarMapa ? View.VISIBLE : View.GONE);
        SessionManager.guardarPaginaMapa(this, mostrarMapa);
        mapaAberto = mostrarMapa;
        localizacao.definirMapaAberto(mostrarMapa);

        if (mostrarMapa) {
            pedirPermissaoOuAtivarLocalizacao();
            amigosNoMapa.iniciar();
        } else {
            amigosNoMapa.parar();
        }
    }
}
