package pt.isep.dssmv.mapaisep.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import androidx.core.content.ContextCompat;

import java.util.List;

import pt.isep.dssmv.mapaisep.config.BibliotecaConfig;

// Faz um scan do Wi-Fi e devolve a força do sinal (RSSI) do router da biblioteca.
public class WifiScanner {

    // rssi: força do sinal do router da biblioteca, ou null se não foi encontrado.
    // erro: motivo se não foi possível fazer o scan (ex.: Wi-Fi desligado).
    public interface WifiCallback {
        void onResult(Integer rssi, String erro);
    }

    private final Context context;
    private final WifiManager wifiManager;

    public WifiScanner(Context context) {
        // Guarda o Context da app (não o ecrã), para não ficar preso a uma Activity que pode fechar.
        this.context = context.getApplicationContext();
        // Cast: o getSystemService devolve um tipo genérico; aqui é o WifiManager.
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
    }

    // Prepara e arranca o scan. O resultado chega depois, pelo callback.
    @SuppressWarnings("MissingPermission")
    public void procurarRouterBiblioteca(WifiCallback callback) {
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            callback.onResult(null, "WIFI_DESLIGADO");
            return;
        }

        // (A) O que fazer quando o Android avisar que o scan acabou.
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                // (D) Sai da lista de quem ouve avisos: só interessa este scan.
                context.unregisterReceiver(this);
                // (E) Lê as redes encontradas e entrega o RSSI do router (ou null).
                callback.onResult(procurarBssid(wifiManager.getScanResults()), null);
            }
        };

        // (B) Regista o receiver antes do scan, para não perder o aviso.
        ContextCompat.registerReceiver(context, receiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED);

        // (C) Pede o scan. Devolve false se o Android recusar (limite de 4 scans a cada 2 minutos):
        // nesse caso nenhum aviso vai chegar, e usam-se os últimos resultados que o telemóvel tem.
        if (!wifiManager.startScan()) {
            context.unregisterReceiver(receiver);
            callback.onResult(procurarBssid(wifiManager.getScanResults()), null);
        }
    }

    // Procura na lista o router da biblioteca e devolve o RSSI dele, ou null.
    private Integer procurarBssid(List<ScanResult> redes) {
        for (ScanResult rede : redes) {
            if (rede.BSSID != null && rede.BSSID.equalsIgnoreCase(BibliotecaConfig.BSSID)) {
                return rede.level;
            }
        }
        return null;
    }
}
