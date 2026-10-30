package pt.isep.dssmv.mapaisep.helper;

import android.content.Context;
import android.content.SharedPreferences;

/** Guarda no dispositivo apenas o estado e o tempo da sessão atual. */
public final class SessaoHelper {

    // Nome do ficheiro de preferências onde a sessão fica guardada no dispositivo.
// Ainda não abre nada: o ficheiro só é aberto em preferencias(), ao passar este nome ao Android.
    private static final String PREFS_NAME = "mapa_isep_session";

    // Cada constante é o nome de um valor guardado nesse ficheiro.
// O mesmo nome é usado para gravar (put) e para ler (get) o valor.
    private static final String KEY_SESSION_ACTIVE = "session_active";
    private static final String KEY_LAST_ACTIVE_AT = "last_active_at";
    private static final String KEY_LAST_PAGE_IS_MAP = "last_page_is_map";
    private static final String KEY_UID = "uid";
    private static final String KEY_ID_TOKEN = "id_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_ID_TOKEN_EXPIRES_AT = "id_token_expires_at";

    // Dois minutos em milissegundos.
    private static final long SESSION_TIMEOUT_MS = 2 * 60 * 1000L;

    private SessaoHelper() {
        // Esta classe só tem métodos estáticos.
    }

    private static SharedPreferences preferencias(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Cria uma sessão depois de o login ter sido aceite. */
    public static void iniciarSessao(
            Context context,
            String uid,
            String idToken,
            String refreshToken,
            long idTokenExpiresAt
    ) {
        preferencias(context).edit()
                .putBoolean(KEY_SESSION_ACTIVE, true)
                .putLong(KEY_LAST_ACTIVE_AT, System.currentTimeMillis())
                .putString(KEY_UID, uid)
                .putString(KEY_ID_TOKEN, idToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_ID_TOKEN_EXPIRES_AT, idTokenExpiresAt)
                .apply();
    }

    /** Atualiza a hora da última utilização da app. */
    public static void registarAtividade(Context context) {
        preferencias(context).edit()
                .putLong(KEY_LAST_ACTIVE_AT, System.currentTimeMillis())
                .apply();
    }

    /** Devolve true apenas enquanto os dois minutos ainda não terminaram. */
    public static boolean sessaoValida(Context context) {
        SharedPreferences prefs = preferencias(context);

        if (!prefs.getBoolean(KEY_SESSION_ACTIVE, false)) {
            return false;
        }

        long ultimaAtividade = prefs.getLong(KEY_LAST_ACTIVE_AT, 0L);
        long tempoPassado = System.currentTimeMillis() - ultimaAtividade;

        return ultimaAtividade > 0L && tempoPassado >= 0L
                && tempoPassado < SESSION_TIMEOUT_MS;
    }

    public static void guardarPaginaMapa(Context context, boolean paginaMapa) {
        preferencias(context).edit()
                .putBoolean(KEY_LAST_PAGE_IS_MAP, paginaMapa)
                .apply();
    }

    public static boolean ultimaPaginaEraMapa(Context context) {
        return preferencias(context).getBoolean(KEY_LAST_PAGE_IS_MAP, false);
    }

    public static String obterUid(Context context) {
        return preferencias(context).getString(KEY_UID, null);
    }

    public static String obterIdToken(Context context) {
        return preferencias(context).getString(KEY_ID_TOKEN, null);
    }

    public static String obterRefreshToken(Context context) {
        return preferencias(context).getString(KEY_REFRESH_TOKEN, null);
    }

    public static long obterExpiracaoIdToken(Context context) {
        return preferencias(context).getLong(KEY_ID_TOKEN_EXPIRES_AT, 0L);
    }

    /** Atualiza os tokens depois de o Firebase aceitar o refreshToken. */
    public static void atualizarTokens(
            Context context,
            String idToken,
            String refreshToken,
            long idTokenExpiresAt
    ) {
        preferencias(context).edit()
                .putString(KEY_ID_TOKEN, idToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_ID_TOKEN_EXPIRES_AT, idTokenExpiresAt)
                .apply();
    }

    /** Elimina a informação local da sessão. */
    public static void terminarSessao(Context context) {
        preferencias(context).edit().clear().apply();
    }
}
