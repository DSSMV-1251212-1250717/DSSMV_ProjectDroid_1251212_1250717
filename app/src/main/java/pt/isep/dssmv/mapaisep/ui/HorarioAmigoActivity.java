package pt.isep.dssmv.mapaisep.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.exceptions.SessaoExpiradaException;
import pt.isep.dssmv.mapaisep.model.Aula;
import pt.isep.dssmv.mapaisep.service.HorarioService;

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
        botaoMenu.setOnClickListener(ancora -> MenuConta.abrir(this, ancora));

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
                    if (erro.equals(SessaoExpiradaException.CODIGO)) {
                        MenuConta.sessaoExpirada(HorarioAmigoActivity.this);
                        return;
                    }
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
                            aula.getSalaId()
                    ));
                }

                texto.setText(horario.toString());
            }
        });
    }

}
