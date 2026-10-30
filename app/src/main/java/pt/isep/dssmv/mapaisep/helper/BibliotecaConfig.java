package pt.isep.dssmv.mapaisep.helper;

import java.util.Arrays;
import java.util.List;

// Configuração da deteção de presença na biblioteca.
// É o único sítio a alterar depois de medir os routers na biblioteca.
public final class BibliotecaConfig {
    // BSSIDs (endereços) dos routers da biblioteca, medidos a 26/10, em minúsculas.
    // Cada router emite com 3 endereços.
    public static final List<String> BSSIDS = Arrays.asList(
            // Router do rés do chão / 1.º andar (-51 a -62 dBm lá dentro)
            "60:de:f3:11:f4:44", "60:de:f3:11:f4:45", "60:de:f3:11:f4:46",
            // Router do 2.º andar (-57 dBm lá dentro)
            "60:de:f3:58:63:f1", "60:de:f3:58:63:f3", "60:de:f3:58:63:f5"
    );

    // Força do sinal (RSSI, em dBm) acima da qual o aluno conta como presente.
    public static final int LIMIAR_ENTRADA = -65;

    // Força do sinal abaixo da qual o aluno deixa de contar como presente.
    public static final int LIMIAR_SAIDA = -75;

    // Tempo entre scans. O Android só deixa fazer 4 scans a cada 2 minutos.
    public static final long INTERVALO_SCAN_MS = 30_000L;

    // De quanto em quanto tempo um aluno presente renova a hora da presença.
    public static final long RENOVAR_PRESENCA_MS = 2 * 60_000L;

    // Uma presença mais antiga do que isto já não conta para a lotação.
    public static final long VALIDADE_PRESENCA_MS = 5 * 60_000L;

    private BibliotecaConfig() {
        // Só tem constantes.
    }
}
