package pt.isep.dssmv.mapaisep.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.model.Aula;
import pt.isep.dssmv.mapaisep.services.AuthService;
import pt.isep.dssmv.mapaisep.services.HorarioService;
import pt.isep.dssmv.mapaisep.services.SessionManager;

// Mostra as aulas de hoje de um amigo. É aberta pela AmigosActivity (botão Ver horário).
public class HorarioAmigoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_horario_amigo);

        // Seta: finish() fecha este ecrã e o Android volta à lista de amigos.
        ImageButton botaoVoltar = findViewById(R.id.btnVoltarHorario);
        botaoVoltar.setOnClickListener(v -> finish());

        // Três barras: o mesmo menu dos outros ecrãs.
        ImageButton botaoMenu = findViewById(R.id.btnMenuHorarioAmigo);
        botaoMenu.setOnClickListener(this::abrirMenu);

        // Dados que a AmigosActivity meteu no Intent com putExtra.
        String uidAmigo = getIntent().getStringExtra("uidAmigo");
        String nomeAmigo = getIntent().getStringExtra("nomeAmigo");

        TextView titulo = findViewById(R.id.tituloHorarioAmigo);
        TextView texto = findViewById(R.id.textoHorarioAmigo);

        titulo.setText(nomeAmigo);
        texto.setText(R.string.horario_loading);

        // O serviço faz o pedido numa thread secundária e chama este callback
        // na main thread, por isso aqui pode mexer no ecrã.
        new HorarioService().obterAulasHoje(this, uidAmigo, new HorarioService.HorarioCallback() {
            @Override
            public void onResult(List<Aula> aulas, String erro) {
                // A Activity pode ter sido fechada enquanto o pedido decorria.
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (erro != null) {
                    texto.setText(R.string.horario_error_generic);
                    return;
                }

                if (aulas.isEmpty()) {
                    texto.setText(R.string.friend_schedule_empty);
                    return;
                }

                // Uma aula por bloco: hora de início, hora de fim, disciplina e sala.
                StringBuilder horario = new StringBuilder();
                for (int i = 0; i < aulas.size(); i++) {
                    Aula aula = aulas.get(i);

                    if (horario.length() > 0) {
                        horario.append("\n\n");
                    }

                    horario.append(getString(
                            R.string.horario_lesson_details,
                            aula.getHoraInicio().toString(),
                            aula.getHoraFim().toString(),
                            aula.getDisciplina(),
                            aula.getSala().getCodigo()
                    ));
                }

                texto.setText(horario.toString());
            }
        });
    }

    // Menu da conta (igual ao da AmigosActivity): Início, Perfil, Amigos e Terminar sessão.
    private void abrirMenu(View ancora) {
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
                finish();
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
}
