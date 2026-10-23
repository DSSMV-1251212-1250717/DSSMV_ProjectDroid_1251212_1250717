package pt.isep.dssmv.mapaisep.config;

// Configuração da deteção de presença na biblioteca.
// É o único sítio a alterar depois de medir o router na biblioteca.
public final class BibliotecaConfig {
    // BSSID (endereço) do router da biblioteca. Valor provisório: substituir pelo medido.
    public static final String BSSID = "aa:bb:cc:dd:ee:ff";

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
