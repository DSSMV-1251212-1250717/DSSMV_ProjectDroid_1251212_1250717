package pt.isep.dssmv.mapaisep.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.model.Amigo;
import pt.isep.dssmv.mapaisep.services.AmigosService;
import pt.isep.dssmv.mapaisep.services.AuthService;
import pt.isep.dssmv.mapaisep.services.SessionManager;

public class AmigosActivity extends AppCompatActivity {

    private AmigosService amigosService;
    private EditText inputNumero;
    private MaterialButton botaoAdicionar;
    private TextView textoEstado;
    private LinearLayout listaAmigos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AuthService.getCurrentUid() == null || !SessionManager.sessaoValida(this)) {
            AuthService.logout();
            SessionManager.terminarSessao(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_amigos);

        amigosService = new AmigosService();
        inputNumero = findViewById(R.id.inputNumeroAmigo);
        botaoAdicionar = findViewById(R.id.btnAdicionarAmigo);
        textoEstado = findViewById(R.id.textoEstadoAmigos);
        listaAmigos = findViewById(R.id.listaAmigos);
        ImageButton botaoMenu = findViewById(R.id.btnMenuAmigos);

        botaoMenu.setOnClickListener(this::abrirMenu);
        botaoAdicionar.setOnClickListener(v -> adicionarAmigo());

        carregarAmigos();
    }

    // Abre as mesmas opções principais disponíveis no ecrã inicial.
    private void abrirMenu(android.view.View ancora) {
        PopupMenu menu = new PopupMenu(this, ancora);
        menu.getMenuInflater().inflate(R.menu.menu_account, menu.getMenu());

        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_home) {
                Intent inicio = new Intent(this, MenuPrincipal.class);
                inicio.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(inicio);
                finish();
                return true;
            }

            if (item.getItemId() == R.id.action_profile) {
                startActivity(new Intent(this, PerfilActivity.class));
                return true;
            }

            if (item.getItemId() == R.id.action_friends) {
                return true;
            }

            if (item.getItemId() == R.id.action_logout) {
                AuthService.logout();
                SessionManager.terminarSessao(this);

                Intent irParaLogin = new Intent(this, LoginActivity.class);
                irParaLogin.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(irParaLogin);
                finish();
                return true;
            }

            return false;
        });

        menu.show();
    }

    private void carregarAmigos() {
        textoEstado.setText(R.string.friends_loading);
        listaAmigos.removeAllViews();

        amigosService.listarAmigos(this, (amigos, erro) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (erro != null) {
                textoEstado.setText(R.string.friends_error);
                return;
            }

            if (amigos.isEmpty()) {
                textoEstado.setText(R.string.friends_empty);
                return;
            }

            textoEstado.setText("");
            mostrarAmigos(amigos);
        });
    }

    private void mostrarAmigos(List<Amigo> amigos) {
        for (Amigo amigo : amigos) {
            LinearLayout cartao = new LinearLayout(this);
            cartao.setOrientation(LinearLayout.HORIZONTAL);
            cartao.setGravity(android.view.Gravity.CENTER_VERTICAL);
            cartao.setPadding(20, 20, 20, 20);
            cartao.setBackgroundResource(R.drawable.bg_campus_card);

            LinearLayout.LayoutParams cartaoParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cartaoParams.bottomMargin = 12;
            listaAmigos.addView(cartao, cartaoParams);

            LinearLayout texto = new LinearLayout(this);
            texto.setOrientation(LinearLayout.VERTICAL);
            cartao.addView(texto, new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));

            TextView nome = new TextView(this);
            nome.setText(amigo.getNome());
            nome.setTextSize(18);
            nome.setTextColor(getColor(R.color.campus_text));
            texto.addView(nome);

            TextView numero = new TextView(this);
            numero.setText(amigo.getNumero());
            numero.setTextSize(14);
            numero.setTextColor(getColor(R.color.campus_muted));
            texto.addView(numero);

            MaterialButton remover = new MaterialButton(this);
            remover.setText(R.string.friends_remove);
            remover.setTextColor(getColor(R.color.campus_button_red));
            remover.setOnClickListener(v -> removerAmigo(amigo));
            cartao.addView(remover);
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
                Toast.makeText(this, R.string.friends_error, Toast.LENGTH_LONG).show();
                return;
            }

            Toast.makeText(this, R.string.friends_removed, Toast.LENGTH_SHORT).show();
            carregarAmigos();
        });
    }

    private void mostrarErroAdicionar(String erro) {
        if (erro.equals("INVALID_STUDENT_NUMBER")) {
            inputNumero.setError(getString(R.string.friends_invalid_number));
        } else if (erro.equals("STUDENT_NOT_FOUND")) {
            inputNumero.setError(getString(R.string.friends_not_found));
        } else if (erro.equals("CANNOT_ADD_SELF")) {
            inputNumero.setError(getString(R.string.friends_self));
        } else {
            Toast.makeText(this, R.string.friends_error, Toast.LENGTH_LONG).show();
        }
    }
}
