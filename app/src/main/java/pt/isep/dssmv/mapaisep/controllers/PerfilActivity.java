package pt.isep.dssmv.mapaisep.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.services.AuthService;
import pt.isep.dssmv.mapaisep.services.PerfilService;
import pt.isep.dssmv.mapaisep.services.SessionManager;

public class PerfilActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);
        ImageButton botaoMenu = findViewById(R.id.btnMenuPerfil);
        botaoMenu.setOnClickListener(this::abrirMenu);
        TextView numero = findViewById(R.id.textoNumeroPerfil);
        TextView turma = findViewById(R.id.textoTurmaPerfil);
        new PerfilService().obterPerfil(this, (numeroAluno, turmaAluno, erro) -> {
            if (erro != null) {
                numero.setText(R.string.profile_error);
                turma.setText("");
                return;
            }
            numero.setText(numeroAluno);
            turma.setText(turmaAluno);
        });
    }

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
                return true;
            }
            if (item.getItemId() == R.id.action_friends) {
                startActivity(new Intent(this, AmigosActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_logout) {
                AuthService.logout();
                SessionManager.terminarSessao(this);
                Intent login = new Intent(this, LoginActivity.class);
                login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(login);
                finish();
                return true;
            }
            return false;
        });
        menu.show();
    }
}
