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

        if (AuthService.getCurrentUid() == null || !SessaoHelper.sessaoValida(this)) {
            AuthService.logout();
            SessaoHelper.terminarSessao(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_amigos);

        amigosService = new AmigosService();
        inputNumero = findViewById(R.id.inputNumeroAmigo);
        botaoAdicionar = findViewById(R.id.btnAdicionarAmigo);
        textoEstado = findViewById(R.id.textoEstadoAmigos);
        ListView listaAmigos = findViewById(R.id.listaAmigos);

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
        });
        listaAmigos.setAdapter(adapter);
        ImageButton botaoMenu = findViewById(R.id.btnMenuAmigos);

        botaoMenu.setOnClickListener(ancora -> MenuConta.abrir(this, ancora));
        botaoAdicionar.setOnClickListener(v -> adicionarAmigo());

        carregarAmigos();
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
                // Texto Base64 -> bytes -> imagem.
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
        botaoAdicionar.setEnabled(false);

        amigosService.adicionarAmigo(this, numero, erro -> {
            botaoAdicionar.setEnabled(true);

            if (erro != null) {
                mostrarErroAdicionar(erro);
                return;
            }

            inputNumero.setText("");
            Toast.makeText(this, R.string.friends_added, Toast.LENGTH_SHORT).show();
            carregarAmigos();
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
