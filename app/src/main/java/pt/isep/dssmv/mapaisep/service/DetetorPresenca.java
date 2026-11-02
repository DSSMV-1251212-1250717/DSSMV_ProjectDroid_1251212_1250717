package pt.isep.dssmv.mapaisep.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.core.content.ContextCompat;

import pt.isep.dssmv.mapaisep.helper.BibliotecaConfig;
import pt.isep.dssmv.mapaisep.helper.WifiHelper;

// Junta o WifiHelper e o PresencaService: faz um scan a cada 30 segundos e atualiza a presença.
// Há um só detetor para a app toda (Singleton): cada ecrã avisa no onResume (ecraVisivel) e no
// onPause (ecraEscondido). Fica ligado enquanto algum ecrã da app estiver visível, por isso o aluno
// conta para a lotação em qualquer ecrã, e desliga-se quando a app vai para fundo.
public class DetetorPresenca {

    // Um só detetor para a app toda (e não um por ecrã): o aluno conta para a lotação
    // em qualquer ecrã da app, e só deixa de contar quando a app vai para fundo.
    private static DetetorPresenca instancia;

    // Tempo de espera antes de desligar quando nenhum ecrã está visível. Ao trocar de ecrã,
    // o Android corre o onPause do antigo antes do onResume do novo; nesse instante há 0 ecrãs
    // visíveis, e esta espera evita desligar e voltar a ligar o detetor a cada troca.
    private static final long ESPERA_ANTES_DE_PARAR_MS = 2000;

    private final Context context;
    private final WifiHelper wifiScanner;
    private final PresencaService presencaService = new PresencaService();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean ativo;            // o detetor está ligado?
    private boolean presente;         // o aluno conta como presente neste momento?
    private long ultimaRenovacao;     // quando foi o último PATCH que correu bem
    private int ecrasVisiveis;        // quantos ecrãs da app estão visíveis neste momento

    // Tarefa dada pelo cartão da biblioteca, executada no fim de cada ciclo
    // (volta a pedir a lotação). Pode ser null.
    private Runnable aoTerminarCiclo;

    // A tarefa "fazer um ciclo", que o Handler executa agora ou daqui a 30 segundos.
    private final Runnable ciclo = new Runnable() {
        @Override
        public void run() {
            executarCiclo();
        }
    };

    // A tarefa "desligar se a app foi para fundo", agendada pelo ecraEscondido.
    private final Runnable pararSeEmFundo = new Runnable() {
        @Override
        public void run() {
            if (ecrasVisiveis == 0) {
                parar();
            }
        }
    };

    // Devolve o detetor da app; cria-o na primeira vez (Singleton).
    // Guarda o Context da app (não o de um ecrã), porque o detetor vive mais do que cada ecrã.
    public static DetetorPresenca obter(Context context) {
        if (instancia == null) {
            instancia = new DetetorPresenca(context.getApplicationContext());
        }
        return instancia;
    }

    // Privado: só o obter() cria o detetor, para nunca haver dois.
    private DetetorPresenca(Context context) {
        this.context = context;
        this.wifiScanner = new WifiHelper(context);
    }

    public void definirAoTerminarCiclo(Runnable tarefa) {
        this.aoTerminarCiclo = tarefa;
    }

    // Chamado no onResume de cada ecrã: um ecrã da app ficou visível.
    public void ecraVisivel() {
        ecrasVisiveis++;
        mainHandler.removeCallbacks(pararSeEmFundo);   // cancela um "desligar" que estava à espera
        iniciar();
    }

    // Chamado no onPause de cada ecrã: se mais nenhum ficar visível, desliga daqui a 2 segundos.
    public void ecraEscondido() {
        if (ecrasVisiveis > 0) {
            ecrasVisiveis--;
        }
        if (ecrasVisiveis == 0) {
            mainHandler.postDelayed(pararSeEmFundo, ESPERA_ANTES_DE_PARAR_MS);
        }
    }

    // Liga o detetor: o primeiro ciclo começa já.
    // Só com localização precisa: sem ela, o Android não mostra as redes Wi-Fi à volta.
    public void iniciar() {
        if (ativo) {
            return;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ativo = true;
        mainHandler.removeCallbacks(ciclo);
        mainHandler.post(ciclo);
    }

    // Desliga o detetor: cancela o próximo ciclo que estava agendado.
    // Esquece também a presença: ao voltar, o primeiro ciclo marca-a outra vez se o router estiver perto.
    public void parar() {
        ativo = false;
        presente = false;
        mainHandler.removeCallbacks(ciclo);
    }

    // 1. Pede o sinal do router ao WifiHelper.
    private void executarCiclo() {
        if (!ativo) {
            return;
        }

        WifiHelper.WifiCallback callbackWifi = new WifiHelper.WifiCallback() {
            @Override
            public void onResult(Integer rssi, String erro) {
                decidir(rssi);
            }
        };
        wifiScanner.procurarRouterBiblioteca(callbackWifi);
    }

    // 2. Com o sinal e os limiares, decide o que fazer.
    private void decidir(Integer rssi) {
        if (!ativo) {
            return;
        }

        long agora = SystemClock.elapsedRealtime();
        boolean sinalForte = rssi != null && rssi >= BibliotecaConfig.LIMIAR_ENTRADA;
        boolean sinalFraco = rssi == null || rssi < BibliotecaConfig.LIMIAR_SAIDA;

        if (!presente && sinalForte) {
            marcar(agora);                                   // entrou
        } else if (presente && sinalFraco) {
            remover();                                       // saiu
        } else if (presente && agora - ultimaRenovacao >= BibliotecaConfig.RENOVAR_PRESENCA_MS) {
            marcar(agora);                                   // continua lá: renova a hora
        } else {
            agendarProximo();                                // nada mudou
        }
    }

    // 3a. PATCH: marca (ou renova) a presença.
    private void marcar(long agora) {
        PresencaService.PresencaCallback callbackPresenca = new PresencaService.PresencaCallback() {
            @Override
            public void onResult(String erro) {
                if (erro == null) {
                    presente = true;
                    ultimaRenovacao = agora;
                }
                agendarProximo();
            }
        };
        presencaService.marcarPresenca(context, callbackPresenca);
    }

    // 3b. DELETE: remove a presença.
    private void remover() {
        PresencaService.PresencaCallback callbackPresenca = new PresencaService.PresencaCallback() {
            @Override
            public void onResult(String erro) {
                if (erro == null) {
                    presente = false;
                }
                agendarProximo();
            }
        };
        presencaService.removerPresenca(context, callbackPresenca);
    }

    // 4. Avisa o cartão da biblioteca (se houver) e agenda o próximo ciclo para daqui a 30 segundos.
    private void agendarProximo() {
        if (!ativo) {
            return;
        }
        if (aoTerminarCiclo != null) {
            aoTerminarCiclo.run();
        }
        // removeCallbacks antes: garante que fica só um ciclo agendado.
        mainHandler.removeCallbacks(ciclo);
        mainHandler.postDelayed(ciclo, BibliotecaConfig.INTERVALO_SCAN_MS);
    }
}
