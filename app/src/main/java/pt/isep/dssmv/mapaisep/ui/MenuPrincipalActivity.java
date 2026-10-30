package pt.isep.dssmv.mapaisep.ui;

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
import pt.isep.dssmv.mapaisep.exceptions.SessaoExpiradaException;
import pt.isep.dssmv.mapaisep.model.Aula;
import pt.isep.dssmv.mapaisep.service.AuthService;
import pt.isep.dssmv.mapaisep.service.HorarioService;
import pt.isep.dssmv.mapaisep.helper.SessaoHelper;
import pt.isep.dssmv.mapaisep.LoginActivity;

// Ecrã principal: página Início (horário e biblioteca) e página Mapa.
// Cada tarefa grande está numa classe própria; aqui só se criam, ligam e desligam:
//   MapaCampus            -> campus, biblioteca e sala da próxima aula no mapa
//   LocalizacaoAluno      -> foto do aluno no mapa e envio da posição dele
//   AmigosNoMapa          -> posição dos amigos (de 5 em 5 segundos)
//   CartaoBiblioteca      -> lotação e deteção de presença por Wi-Fi
public class MenuPrincipalActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final MapaCampus mapaCampus = new MapaCampus(this);
    private final AmigosNoMapa amigosNoMapa = new AmigosNoMapa(this);

    // Criadas no onCreate (precisam do ecrã já carregado / dos serviços do Android).
    private LocalizacaoAluno localizacao;
    private CartaoBiblioteca cartaoBiblioteca;

    private ActivityResultLauncher<String[]> permissaoLocalizacao;
    private boolean mapaAberto;

    // true quando o pedido de permissão veio do botão da biblioteca (e não do mapa).
    private boolean pedidoDaBiblioteca;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Confirma se o aluno tem sessão ativa.
        if (AuthService.getIdToken() == null
                || !SessaoHelper.sessaoValida(this)) {

            AuthService.logout();
            SessaoHelper.terminarSessao(this);

            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        localizacao = new LocalizacaoAluno(this);

        setContentView(R.layout.activity_menu_principal);

        carregarHorario();

        // Botão "Ativar localização" do cartão: pede a localização precisa.
        cartaoBiblioteca = new CartaoBiblioteca(this, () -> {
            pedidoDaBiblioteca = true;
            permissaoLocalizacao.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION });
        });
        cartaoBiblioteca.carregarLotacao();

        // Regista o que fazer quando o aluno responde ao pedido de permissão.
        permissaoLocalizacao = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultados -> {
            if (LocalizacaoAluno.temPermissaoLocalizacao(this)) {
                localizacao.ativar();
            } else {
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
            }

            if (LocalizacaoAluno.temLocalizacaoPrecisa(this)) {
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

        // Vai buscar as páginas Início e Mapa.
        View paginaInicio = findViewById(R.id.paginaInicio);
        View paginaMapa = findViewById(R.id.paginaMapa);
        mostrarPagina(SessaoHelper.ultimaPaginaEraMapa(this), paginaInicio, paginaMapa);

        // Vai buscar o fragmento do Google Maps ao layout.
        SupportMapFragment fragmentoMapa = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapaGoogle);

        if (fragmentoMapa != null) {
            fragmentoMapa.getMapAsync(this);
        }

        // Botão Início: mostra a página Início.
        findViewById(R.id.btnInicio).setOnClickListener(v -> {

            mostrarPagina(false, paginaInicio, paginaMapa);
        });

        // Botão Mapa: mostra a página Mapa.
        findViewById(R.id.btnMapa).setOnClickListener(v -> {

            mostrarPagina(true, paginaInicio, paginaMapa);
        });

        // Menu da conta (botão dos 3 pontos).
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
                // Se o ecrã já foi fechado, ignora a resposta.
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // Trata os erros devolvidos pelo HorarioService.
                if (erro != null) {
                    if (erro.equals(SessaoExpiradaException.CODIGO)) {
                        MenuConta.sessaoExpirada(MenuPrincipalActivity.this);
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

                // Sem aulas hoje: mostra uma mensagem.
                if (resultado.isEmpty()) {
                    textoAulas.setText(R.string.horario_empty);
                    return;
                }

                // Monta o texto do horário.
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

                // Mostra as aulas no ecrã.
                textoAulas.setText(horario.toString());
            }
        };

        // Passa o callback ao serviço. O serviço faz o pedido numa thread
        // secundária e este método retorna logo, sem esperar pelo Firestore.
        horarioService.obterAulasHoje(this, callback);
    }

    // Termina a sessão e volta ao login.
    private void terminarSessao() {
        // Apaga o token e a sessão guardados.
        AuthService.logout();
        SessaoHelper.terminarSessao(this);

        Intent irParaLogin = new Intent(this, LoginActivity.class);

        // Limpa o histórico de ecrãs (o botão voltar não regressa aqui).
        irParaLogin.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(irParaLogin);
        finish();
    }

    // Chamado quando o Google Maps está pronto: entrega o mapa a cada peça.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        amigosNoMapa.definirMapa(googleMap);
        localizacao.definirMapa(googleMap);
        mapaCampus.configurar(googleMap);

        if (LocalizacaoAluno.temPermissaoLocalizacao(this)) {
            localizacao.ativar();
        }
    }

    // Pede a permissão de localização, se ainda não foi dada.
    private void pedirPermissaoOuAtivarLocalizacao() {
        if (LocalizacaoAluno.temPermissaoLocalizacao(this)) {
            localizacao.ativar();
        } else {
            permissaoLocalizacao.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }
            );
        }
    }

    @Override
    protected void onPause() {
        SessaoHelper.registarAtividade(this);
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
                || !SessaoHelper.sessaoValida(this)) {
            terminarSessao();
            return;
        }

        SessaoHelper.registarAtividade(this);
        localizacao.ativar();

        // App aberta: começa os scans de Wi-Fi (só com localização precisa).
        cartaoBiblioteca.iniciar();

        if (mapaAberto) {
            amigosNoMapa.iniciar();
        }
    }

    private void mostrarPagina(boolean mostrarMapa, View paginaInicio, View paginaMapa) {
        // Mostra uma página e esconde a outra.
        if (mostrarMapa) {
            paginaInicio.setVisibility(View.GONE);
            paginaMapa.setVisibility(View.VISIBLE);
        } else {
            paginaInicio.setVisibility(View.VISIBLE);
            paginaMapa.setVisibility(View.GONE);
        }
        SessaoHelper.guardarPaginaMapa(this, mostrarMapa);
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
