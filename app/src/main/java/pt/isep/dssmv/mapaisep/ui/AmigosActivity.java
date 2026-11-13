package pt.isep.dssmv.mapaisep.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.service.DetetorPresenca;
import pt.isep.dssmv.mapaisep.adapter.ListViewAdapterAmigos;
import pt.isep.dssmv.mapaisep.exceptions.DuplicatedDataException;
import pt.isep.dssmv.mapaisep.exceptions.SessaoExpiradaException;
import pt.isep.dssmv.mapaisep.model.Amigo;
import pt.isep.dssmv.mapaisep.service.AmigosService;
import pt.isep.dssmv.mapaisep.service.AuthService;
import pt.isep.dssmv.mapaisep.service.PerfilService;
import pt.isep.dssmv.mapaisep.helper.SessaoHelper;
import pt.isep.dssmv.mapaisep.LoginActivity;

public class AmigosActivity extends AppCompatActivity {

    private AmigosService amigosService;
    private final PerfilService perfilService = new PerfilService();
    private EditText inputNumero;
    private MaterialButton botaoAdicionar;
    private TextView textoEstado;

    // A lista que o adapter mostra, e as fotos já carregadas (por uid).
    private final List<Amigo> amigos = new ArrayList<>();
    private final Map<String, Bitmap> fotos = new HashMap<>();
    private ListViewAdapterAmigos adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // uid: quem é o aluno (o identificador dele no Firebase, usado nos endereços dos documentos).
        if (AuthService.getCurrentUid() == null || !SessaoHelper.sessaoValida(this)) {
            AuthService.logout();
            SessaoHelper.terminarSessao(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_amigos);
        //carregar o layout do xml


        amigosService = new AmigosService();
        inputNumero = findViewById(R.id.inputNumeroAmigo);
        //depois de estar carregado o layout do xml, usamos o findViewbyId para obter as referencias aos componentes da interface
        //pelo seu ids
        botaoAdicionar = findViewById(R.id.btnAdicionarAmigo);
        textoEstado = findViewById(R.id.textoEstadoAmigos);
        ListView listaAmigos = findViewById(R.id.listaAmigos);
        //ListView, uma lista vazia no XML.
        //o adapter diz-lhe quantas linhas deve ter cada elemento(amigo)
        //como se desenha cada linha

        // O adapter repete o item_amigo.xml por cada amigo. Os botões do cartão
        // chamam estes dois métodos da Activity.
        adapter = new ListViewAdapterAmigos(this, amigos, fotos, new ListViewAdapterAmigos.AcoesAmigo() {
            @Override
            public void remover(Amigo amigo) {
                removerAmigo(amigo);
            }

            @Override
            public void verHorario(Amigo amigo) {
                abrirHorarioAmigo(amigo);
            }

            //remover e verHorario sao ListViewAdapterAmigos.AcoesAmigo()
            //tamos a preencher oque fazer quando se carrega em remover ou verhorario
        });
        listaAmigos.setAdapter(adapter);
        ImageButton botaoMenu = findViewById(R.id.btnMenuAmigos);

        botaoMenu.setOnClickListener(ancora -> MenuConta.abrir(this, ancora));
        //o setonClickListener precisa de um objeto que diga oque fazer quando há um clique
        botaoAdicionar.setOnClickListener(v -> adicionarAmigo());
        //( ): dentro dos parênteses vai aquilo que o botão deve fazer.
        //v: o nome de uma variável. Quando clicas no botao adicionar, o Android põe lá o botão clicado.
        //mete o nome do botao no v
        //É igual a escrever View v = botaoAdicionar;.
        //adicionarAmigo(): o método que é chamado.
        //O v é a View que foi clicada. O Android passa-a sempre ao onClick, mas aqui não a usamos.
        //Como só há um método possível, o Java sabe que a lambda que lhe dás é esse onClick.

        carregarAmigos();
    }

    @Override
    protected void onResume() {
        // onResume: o Android chama-o uma vez, no momento em que este ecrã passa a estar à vista.
        //metodo da classe mãe(Activity) --> que a AmigosActivity rescreve
        super.onResume();
        // Um ecrã da app ficou visível: o detetor da biblioteca fica ligado.
        DetetorPresenca.obter(this).ecraVisivel();
    }

    // Ao sair deste ecrã, regista a hora: a app esteve em uso até agora.
    // Sem isto, o MenuPrincipalActivity só conhecia a hora em que este ecrã foi aberto
    // e, passados 2 minutos aqui, mandava o aluno para o login ao voltar.
    @Override
    protected void onPause() {
        // onPause: o Android chama-o uma vez, no momento em que este ecrã deixa de estar à vista.
        //metodo da classe mãe(Activity) --> que a AmigosActivity rescreve
        super.onPause();
        SessaoHelper.registarAtividade(this);
        // Este ecrã deixou de estar visível: se mais nenhum ficar, o detetor da biblioteca desliga-se.
        DetetorPresenca.obter(this).ecraEscondido();
    }

    private void carregarAmigos() {
        textoEstado.setText(R.string.friends_loading);

        amigosService.listarAmigos(this, (resultado, erro) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (erro != null) {
                if (erro.equals(SessaoExpiradaException.CODIGO)) {
                    MenuConta.sessaoExpirada(this);
                    return;
                }
                textoEstado.setText(R.string.friends_error);
                return;
            }

            // Põe os amigos novos na lista do adapter e manda redesenhar.
            amigos.clear();
            amigos.addAll(resultado);
            adapter.notifyDataSetChanged();

            if (amigos.isEmpty()) {
                textoEstado.setText(R.string.friends_empty);
                return;
            }

            textoEstado.setText("");
            carregarFotos();
        });
    }

    // Pede a foto de cada amigo. Quando uma chega, guarda-a e o adapter redesenha a lista.
    private void carregarFotos() {
        for (int i = 0; i < amigos.size(); i++) {
            String uid = amigos.get(i).getUid();
            if (fotos.containsKey(uid)) {
                continue;
            }

            perfilService.obterFoto(this, uid, fotoBase64 -> {
                if (fotoBase64 == null || isFinishing() || isDestroyed()) {
                    return;
                }
                // A foto está guardada no Firestore como texto Base64 (o Firestore guarda texto, não ficheiros).
                // Base64.decode: texto -> bytes do JPEG.
                // BitmapFactory.decodeByteArray: bytes -> imagem (Bitmap); 0 e bytes.length = ler os bytes todos.
                byte[] bytes = Base64.decode(fotoBase64, Base64.DEFAULT);
                Bitmap imagem = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (imagem != null) {
                    fotos.put(uid, imagem);
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    private void adicionarAmigo() {
        String numero = inputNumero.getText().toString().trim();
        //o get text devolve um editable
        //o tipo de texto do android para caixas editaveis
        botaoAdicionar.setEnabled(false);
        //quando formos fazer pedidos à firebase, nao queremos que a pessoa
        //clique outra vez no botao fazendo novamente outro pedido
        //esta linha desativa o botao

        amigosService.adicionarAmigo(this, numero, erro -> {
            //O adicionarAmigo do service pede um OperacaoCallback
            //erro é o parâmetro que o service nos passa quando acaba.
            //null = correu bem; senão traz o código do erro (ex.: "STUDENT_NOT_FOUND").
            //temos de escolher oque fazer com ele
            botaoAdicionar.setEnabled(true);

            if (erro != null) {
                mostrarErroAdicionar(erro);
                //se houver erro mostramos o erro
                // a caixa nao é limpa
                return;
            }

            inputNumero.setText("");
            Toast.makeText(this, R.string.friends_added, Toast.LENGTH_SHORT).show();
            //sao aquelas mensagens que aparecem e depois desaparecem sozinha
            //lenght short é pouco tempo

            //R.string.friends_added
            //os textos da app ficam num ficheiro xml
            //O R é uma classe que o Android gera sozinho a partir da pasta res.
            //R.string são os ids de todos os textos do strings.xml.
            //R.string.friends_added é o id do texto com o nome friends_added.
            //para ele saber onde ir ver
            carregarAmigos();
            // como conseguimos adicionar o amigo
            //queremos atualizar a lista
        });
    }

    private void removerAmigo(Amigo amigo) {
        amigosService.removerAmigo(this, amigo, erro -> {
            if (erro != null) {
                if (erro.equals(SessaoExpiradaException.CODIGO)) {
                    MenuConta.sessaoExpirada(this);
                    return;
                }
                Toast.makeText(this, R.string.friends_error, Toast.LENGTH_LONG).show();
                return;
            }

            Toast.makeText(this, R.string.friends_removed, Toast.LENGTH_SHORT).show();
            carregarAmigos();
        });
    }

    // Abre o ecrã do horário deste amigo.
    // O uid vai no Intent (putExtra) porque a Activity nova não vê as variáveis desta;
    // lá é lido com getIntent().getStringExtra("uidAmigo").
    private void abrirHorarioAmigo(Amigo amigo) {
        Intent intent = new Intent(this, HorarioAmigoActivity.class);
        intent.putExtra("uidAmigo", amigo.getUid());
        intent.putExtra("nomeAmigo", amigo.getNome());
        startActivity(intent);
    }

    private void mostrarErroAdicionar(String erro) {
        if (erro.equals(SessaoExpiradaException.CODIGO)) {
            MenuConta.sessaoExpirada(this);
        } else if (erro.equals("INVALID_STUDENT_NUMBER")) {
            inputNumero.setError(getString(R.string.friends_invalid_number));
        } else if (erro.equals("STUDENT_NOT_FOUND")) {
            inputNumero.setError(getString(R.string.friends_not_found));
        } else if (erro.equals("CANNOT_ADD_SELF")) {
            inputNumero.setError(getString(R.string.friends_self));
        } else if (erro.equals(DuplicatedDataException.CODIGO)) {
            inputNumero.setError(getString(R.string.friends_already));
        } else {
            Toast.makeText(this, R.string.friends_error, Toast.LENGTH_LONG).show();
        }
    }
}
