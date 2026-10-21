package pt.isep.dssmv.mapaisep.services;
import pt.isep.dssmv.mapaisep.config.FirebaseConfig;
import pt.isep.dssmv.mapaisep.network.HttpClient;
import pt.isep.dssmv.mapaisep.network.HttpResponse;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;

//O AuthService e o BibliotecaService estão no mesmo pacote
//nao é preciso importar

public class BibliotecaService {

    private final HttpClient httpClient = new HttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    public interface BibliotecaCallback {
        // Integer (e não int) para poder ser null quando há erro.
        void onResult(Integer lotacaoEstimativa, String erro);
    }

    public void obterLotacao(BibliotecaCallback callback){
        //O getIdToken é static
        // idToken: prova ao Firestore que o pedido vem de um aluno autenticado.
        // Vai no cabeçalho "Authorization: Bearer"; sem ele, o Firestore recusa (401/403).
        String idToken = AuthService.getIdToken();

        // Sem token não há sessão: entrega o erro na main thread e não faz o pedido.
        if (idToken == null) {
            mainHandler.post(() -> callback.onResult(null, "NOT_AUTHENTICATED"));
            //pede para correr na main thread a funcao implementada
            return;
        }

        new Thread(() -> {
            try {
                String endereco = FirebaseConfig.FIRESTORE_URL + "/biblioteca/estado";
                // O HttpClient envia o GET com o token e espera pela resposta (código + JSON).
                // O HttpClient envia o GET com o token e espera pela resposta (código + JSON).
                HttpResponse response = httpClient.enviarPedido(endereco, "GET", idToken, null);

            } catch (IOException e) {
                // Falha de rede (sem internet, timeout...): entrega o erro na main thread.
                mainHandler.post(() -> callback.onResult(null, "NETWORK_ERROR"));
            }
        }).start();
    }
}
