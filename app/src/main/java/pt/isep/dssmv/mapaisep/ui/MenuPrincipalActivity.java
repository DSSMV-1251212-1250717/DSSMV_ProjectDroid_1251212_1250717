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
import pt.isep.dssmv.mapaisep.service.DetetorPresenca;
import pt.isep.dssmv.mapaisep.service.HorarioService;
import pt.isep.dssmv.mapaisep.helper.SessaoHelper;
import pt.isep.dssmv.mapaisep.LoginActivity;

// Ecrã principal: página Início (horário e biblioteca) e página Mapa.
// Cada tarefa grande está numa classe própria; aqui só se criam, ligam e desligam:
//   MapaCampus            -> campus, biblioteca e sala da próxima aula no mapa
//   LocalizacaoAluno      -> foto do aluno no mapa e envio da posição dele
//   AmigosNoMapa          -> posição dos amigos (de 5 em 5 segundos)
//   CartaoBiblioteca      -> lotação da biblioteca (o detetor de presença é um só para a app: DetetorPresenca)
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
        // Consulta se há um idToken em memória (dado pelo Firebase quando se inicia sessão, para provar
        // quem somos) e se a sessão ainda é válida (menos de 2 minutos sem usar a app). Se faltar algum, volta ao login.
        if (AuthService.getIdToken() == null
                || !SessaoHelper.sessaoValida(this)) {
            //damos logOut se nao tivermos o IdToken porque sem ele a App nao consegue fazer nada
            //pedidos à firebase
            //em vez de deixarmos a app abrir e ela falhar em todos os pedidos
            //voltamos ao login

            AuthService.logout();
            SessaoHelper.terminarSessao(this);
            //limpa os dados

            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        localizacao = new LocalizacaoAluno(this);

        setContentView(R.layout.activity_menu_principal);
        //Carrega o XML. O mapa é criado aqui, ainda escondido.

        carregarHorario();

        // Botão "Ativar localização" do cartão: pede a localização precisa.
        //é preciso localizacao precisa porque a bilioteca é detetada pelo o wifi
        // e o android trata o wifi como localizacao precisa;
        cartaoBiblioteca = new CartaoBiblioteca(this, () -> {
            // () -> tamos a preencher o runnable que diz oque fazer quando
            //alguem clicar no botaopermissao
            pedidoDaBiblioteca = true;
            permissaoLocalizacao.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION });
            //permissaoLocalizacao.launch(...)
            // abre a janela do Android “Permitir localização?”, com a precisa (FINE) e a aproximada (COARSE).
        });

        //cria o cartao da biblioteca e passa-lhe uma runnable
       //Esse Runnable é  o que corre quando se carrega no botão “Ativar localização” do cartão.
        cartaoBiblioteca.carregarLotacao();
        //tenta carregar a lotacao, verifica as permissoes e mostra msgs de erro

        // Regista o que fazer quando o aluno responde ao pedido de permissão.
        permissaoLocalizacao = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultados -> {
            //resultados é o callback; corre quando o aluno responde na janela com precisa ou aproximada
            if (LocalizacaoAluno.temPermissaoLocalizacao(this)) {
                localizacao.ativar();
                //localizacao é geral, se ativa para o mapa ativa para tudo
                //para o mapa ;o mapa pede a permissão sozinho
                //em baixo é apra o wifi mas sao feitos aqui os dois
            } else {
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
            }

            if (LocalizacaoAluno.temLocalizacaoPrecisa(this)) {
                // Já pode ver as redes Wi-Fi: liga o detetor e o cartão passa a mostrar o número.
                DetetorPresenca.obter(this).iniciar();
                cartaoBiblioteca.carregarLotacao();
                //este so corre quando nao temos a lotacao à nossa frente
                //e quando ativamos a localizacao


            } else if (pedidoDaBiblioteca
                    && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                //so queremos que abra quando vem da biblioteca porque temos um botao
                // O Android já não mostra a janela ("não perguntar novamente"): abre as definições da app.
                //porque a pessoa recusou duas vezes;
                //é preciso abrir as definicoes para fazer isso
                Intent definicoes = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null));
                //Abre o ecrã das definições da nossa app no telemóvel
                //package é o nome da app para saber onde alterar
                startActivity(definicoes);
            }
            pedidoDaBiblioteca = false;
        });

        // Vai buscar as páginas Início e Mapa.
        View paginaInicio = findViewById(R.id.paginaInicio);
        View paginaMapa = findViewById(R.id.paginaMapa);
        //o ecra principal tem duas paginas no mesmo layout o inicio(com horario e biblioteca) e o mapa
        //mostramos uma e escondemos outra
        mostrarPagina(SessaoHelper.ultimaPaginaEraMapa(this), paginaInicio, paginaMapa);
        //mostrarPagina mete um visivel e o outro nao visivel

        //Vai buscar o fragmento do mapa e pede o objeto GoogleMap. Quando estiver pronto, a Google chama o onMapReady.
        //mapaGoogle é o id do mapa no xml
        SupportMapFragment fragmentoMapa = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapaGoogle);
        //O findFragmentById é do Android
        //por isso devolve um tipo genérico
        //O getMapAsync só existe no SupportMapFragment, que é a classe da Google.
        //como é um fragment vamos buscalo com
        //getSupportFragmentManager().findFragmentById(...)

        if (fragmentoMapa != null) {
            fragmentoMapa.getMapAsync(this);
            //o mapa demora carregar porque tem de carregar o Google Play Services e verificar a API key
            //avia-nos qunado tiver pronto
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
            //O Android já sabe que botão vigiar por causa do btnMenu.setOnClickListener(...).
            //anchor (o v) é a View clicada, que o Android passa sempre ao onClick(View v),
            //para o código saber qual View foi clicada (útil quando o mesmo listener serve vários botões).
            //Aqui usa-se como âncora: o PopupMenu abre colado ao btnMenu.
            //O que está dentro das chavetas é a implementação do onClick.
            PopupMenu menuConta = new PopupMenu(this, anchor);
            //cria um menu pequeno, que abre colado ao botão
            // O anchor é o botão clicado.

            menuConta.getMenuInflater().inflate(R.menu.menu_account, menuConta.getMenu());
            //inflate preenche o menu pequeno com as opções desse XML.
            //res/menu/menu_account.xml.

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
            //criamos o menuPopup com oque queremos
            //dizemos as regras, ou seja, oque acontece quando clicamos num botao
            //depois de tar tudo pronto mostramos o menu
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
                //O StringBuilder é um “texto em construção”:
                // vamos juntando bocados com o append(), e no fim o toString() dá a String final.

                for (int i = 0; i < resultado.size(); i++) {
                    Aula item = resultado.get(i);

                    if (horario.length() > 0) {
                        horario.append("\n\n");
                    }

                    // Hora de início, hora de fim, disciplina e sala.
                    horario.append(getString(R.string.horario_lesson_details, item.getHoraInicio().toString(), item.getHoraFim().toString(),
                            item.getDisciplina(), item.getSalaId()));
                    //O getString vai buscar o texto ao strings.xml
                    //como a gente quer a informacao
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
        //O Android guarda os ecrãs abertos numa pilha, e o botão “voltar” volta ao ecrã de baixo.
        //CLEAR_TASK apaga a pilha toda: o Menu, os Amigos, tudo o que estava aberto.
        //NEW_TASK é obrigatório para o CLEAR_TASK funcionar. Começa uma pilha nova, só com o Login.

        //fizemos logOut, se nao apagarmos a pilha
        //ao clicarmos no botao de voltar, como a pilha existe e tem informacao conseguimos voltar
        //ao menu onde está a nossa conta
        startActivity(irParaLogin);
        finish();
    }

    // Chamado quando o Google Maps está pronto: entrega o mapa a cada peça.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Entrega o mapa às três classes. para elas poderem meter no mapa as suas coisas
        //A Google chama isto quando o mapa está pronto
        //chamado pelo GOogle quando o mapa tá pronto
        amigosNoMapa.definirMapa(googleMap);
        //para poder meter os amigos
        //quando o mapa fica pronto entegamos ao amigos no Mapa para ele poder meter os marcadores dos amigos
        localizacao.definirMapa(googleMap);
        //VAI BUSCAR A NOSSA FOTO
        mapaCampus.configurar(googleMap);
        //desenha logo o campus, a biblioteca e a sala.

        if (LocalizacaoAluno.temPermissaoLocalizacao(this)) {
            localizacao.ativar();
            //ligar o gps, pede a loc
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
        //para contribuir para os dois minutos quando saimos da app
        amigosNoMapa.parar();
        super.onPause();

        if (localizacao != null) {
            localizacao.parar();
        }

        // Este ecrã deixou de estar visível: se mais nenhum ficar, o detetor da biblioteca desliga-se.
        DetetorPresenca.obter(this).ecraEscondido();
        //ecra escondido remove a contacao
    }

    @Override
    protected void onDestroy() {
        // O cartão deste ecrã deixa de existir: o detetor deixa de o avisar no fim de cada ciclo.
        DetetorPresenca.obter(this).definirAoTerminarCiclo(null);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        //avisa que o ecra ficou visivel
        super.onResume();

        // Um ecrã da app ficou visível: o detetor da biblioteca fica ligado.
        DetetorPresenca.obter(this).ecraVisivel();
        //ele ve se temos as permissoes;
        //adiciona à contagem

        // Consulta se há um idToken em memória (dado pelo Firebase quando se inicia sessão, para provar
        // quem somos) e se a sessão ainda é válida (menos de 2 minutos sem usar a app). Se faltar algum, volta ao login.
        if (AuthService.getIdToken() == null
                || !SessaoHelper.sessaoValida(this)) {
            terminarSessao();
            return;
        }

        SessaoHelper.registarAtividade(this);
        localizacao.ativar();

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
            //Abrir a página Mapa liga o ciclo dos amigos. Ir para a Início desliga-o.
        } else {
            amigosNoMapa.parar();
        }
    }
}
