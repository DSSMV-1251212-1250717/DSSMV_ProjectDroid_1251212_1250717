package pt.isep.dssmv.mapaisep.ui;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.exceptions.SessaoExpiradaException;
import pt.isep.dssmv.mapaisep.service.BibliotecaService;
import pt.isep.dssmv.mapaisep.service.DetetorPresenca;

// Cartão da biblioteca na página Início: mostra a lotação e o botão "Ativar localização".
// O MenuPrincipalActivity cria-o no onCreate e chama carregarLotacao().
// O detetor de presença não é do cartão: é um só para a app toda (DetetorPresenca.obter),
// ligado e desligado pelos ecrãs. O cartão só lhe pede que o avise no fim de cada ciclo.
public class CartaoBiblioteca {
    private final AppCompatActivity activity;
    private final TextView textoBiblioteca;
    private final Button btnPermissao;

    // pedirPermissao: o que fazer quando se carrega em "Ativar localização"
    // (o pedido de permissões tem de ficar na Activity, por isso vem de lá).
    public CartaoBiblioteca(AppCompatActivity activity, Runnable pedirPermissao) {
        this.activity = activity;

        // Caixa de texto e botão da biblioteca (activity_menu_principal.xml).
        textoBiblioteca = activity.findViewById(R.id.textoBiblioteca);
        btnPermissao = activity.findViewById(R.id.btnPermissaoBiblioteca);

        // Botão do cartão: pede a localização precisa.
        btnPermissao.setOnClickListener(v -> pedirPermissao.run());

        // No fim de cada ciclo do detetor, volta a pedir a lotação (o número no ecrã acompanha quem entra e sai).
        DetetorPresenca.obter(activity).definirAoTerminarCiclo(new Runnable() {
            @Override
            public void run() {
                carregarLotacao();
            }
        });
    }

    // Pede a lotação ao BibliotecaService e mostra-a no cartão.
    public void carregarLotacao() {
        // O ecrã principal já fechou: não há cartão onde mostrar.
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        // Sem localização precisa: mostra a mensagem e o botão em vez da lotação.
        // A lista das redes Wi-Fi à volta diz onde a pessoa está, por isso o Android só a mostra
        // a uma app com a permissão de localização precisa (ACCESS_FINE_LOCATION); com a aproximada,
        // a lista vem vazia e o telemóvel deste aluno não consegue contar para a lotação.
        // Escolha da app: só vê a lotação quem também contribui para ela.
        if (!LocalizacaoAluno.temLocalizacaoPrecisa(activity)) {
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
                    // Sessão expirada: igual aos outros ecrãs (mensagem e volta ao login).
                    if (erro.equals(SessaoExpiradaException.CODIGO)) {
                        MenuConta.sessaoExpirada(activity);
                        return;
                    }
                    textoBiblioteca.setText(R.string.biblioteca_indisponivel);
                    return;
                }

                // 1 pessoa no singular; qualquer outro número (0, 2, 3...) no plural.
                if (lotacaoEstimativa == 1) {
                    textoBiblioteca.setText(R.string.biblioteca_lotacao_uma);
                } else {
                    // Substitui o %1$d do texto pelo número recebido.
                    textoBiblioteca.setText(activity.getString(R.string.biblioteca_lotacao, lotacaoEstimativa));
                }
            }
        };
        // Entrega o callback ao serviço, que faz o pedido numa thread secundária.
        bibliotecaService.obterLotacao(activity, callback);
    }
}
