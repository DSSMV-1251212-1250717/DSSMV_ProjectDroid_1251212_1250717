package pt.isep.dssmv.mapaisep.controllers;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.services.BibliotecaService;
import pt.isep.dssmv.mapaisep.services.DetetorPresenca;

// Cartão da biblioteca na página Início: mostra a lotação e liga o detetor de presença por Wi-Fi.
// O MenuPrincipal cria-o no onCreate e chama iniciar() / parar() / carregarLotacao().
public class CartaoBiblioteca {
    private final AppCompatActivity activity;
    private final TextView textoBiblioteca;
    private final Button btnPermissao;

    // Deteta, pelo Wi-Fi, se o aluno está na biblioteca e atualiza a presença no Firestore.
    private final DetetorPresenca detetorPresenca;

    // pedirPermissao: o que fazer quando se carrega em "Ativar localização"
    // (o pedido de permissões tem de ficar na Activity, por isso vem de lá).
    public CartaoBiblioteca(AppCompatActivity activity, Runnable pedirPermissao) {
        this.activity = activity;

        // Caixa de texto e botão da biblioteca (menu_principal.xml).
        textoBiblioteca = activity.findViewById(R.id.textoBiblioteca);
        btnPermissao = activity.findViewById(R.id.btnPermissaoBiblioteca);

        // Botão do cartão: pede a localização precisa.
        btnPermissao.setOnClickListener(v -> pedirPermissao.run());

        // No fim de cada ciclo do detetor, volta a pedir a lotação (o número no ecrã acompanha quem entra e sai).
        detetorPresenca = new DetetorPresenca(activity, new Runnable() {
            @Override
            public void run() {
                carregarLotacao();
            }
        });
    }

    // App aberta: começa os scans de Wi-Fi (ver as redes precisa da localização precisa).
    public void iniciar() {
        if (LocalizacaoUtilizador.temLocalizacaoPrecisa(activity)) {
            detetorPresenca.iniciar();
        }
    }

    // App em fundo: pára os scans de Wi-Fi.
    public void parar() {
        detetorPresenca.parar();
    }

    // Pede a lotação ao BibliotecaService e mostra-a no cartão.
    public void carregarLotacao() {
        // Sem localização precisa: mostra a mensagem e o botão em vez da lotação.
        if (!LocalizacaoUtilizador.temLocalizacaoPrecisa(activity)) {
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
            @Override
            public void onResult(Integer lotacaoEstimativa, String erro) {
                // Se o ecrã já foi fechado, não há onde mostrar o resultado.
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                if (erro != null) {
                    textoBiblioteca.setText(R.string.biblioteca_indisponivel);
                    return;
                }

                // Substitui o %1$d do texto pelo número recebido.
                textoBiblioteca.setText(activity.getString(R.string.biblioteca_lotacao, lotacaoEstimativa));
            }
        };
        // Entrega o callback ao serviço, que faz o pedido numa thread secundária.
        bibliotecaService.obterLotacao(callback);
    }
}
