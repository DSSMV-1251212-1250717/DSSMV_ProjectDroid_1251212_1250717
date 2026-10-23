package pt.isep.dssmv.mapaisep.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import pt.isep.dssmv.mapaisep.config.BibliotecaConfig;

// Junta o WifiScanner e o PresencaService: faz um scan a cada 30 segundos e atualiza a presença.
// O MenuPrincipal liga-o no onResume e desliga-o no onPause.
public class DetetorPresenca {

    private final WifiScanner wifiScanner;
    private final PresencaService presencaService = new PresencaService();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean ativo;            // o detetor está ligado?
    private boolean presente;         // o aluno conta como presente neste momento?
    private long ultimaRenovacao;     // quando foi o último PATCH que correu bem

    // A tarefa "fazer um ciclo", que o Handler executa agora ou daqui a 30 segundos.
    private final Runnable ciclo = new Runnable() {
        @Override
        public void run() {
            executarCiclo();
        }
    };

    // Tarefa dada por quem cria o detetor, executada no fim de cada ciclo
    // (o MenuPrincipal usa-a para voltar a pedir a lotação).
    private final Runnable aoTerminarCiclo;

    public DetetorPresenca(Context context, Runnable aoTerminarCiclo) {
        this.wifiScanner = new WifiScanner(context);
        this.aoTerminarCiclo = aoTerminarCiclo;
    }

    // Liga o detetor: o primeiro ciclo começa já.
    public void iniciar() {
        if (ativo) {
            return;
        }
        ativo = true;
        mainHandler.post(ciclo);
    }

    // Desliga o detetor: cancela o próximo ciclo que estava agendado.
    public void parar() {
        ativo = false;
        mainHandler.removeCallbacks(ciclo);
    }

    // 1. Pede o sinal do router ao WifiScanner.
    private void executarCiclo() {
        if (!ativo) {
            return;
        }

        WifiScanner.WifiCallback callbackWifi = new WifiScanner.WifiCallback() {
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
        presencaService.marcarPresenca(callbackPresenca);
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
        presencaService.removerPresenca(callbackPresenca);
    }

    // 4. Avisa quem criou o detetor e agenda o próximo ciclo para daqui a 30 segundos.
    private void agendarProximo() {
        if (ativo) {
            aoTerminarCiclo.run();
            mainHandler.postDelayed(ciclo, BibliotecaConfig.INTERVALO_SCAN_MS);
        }
    }
}
