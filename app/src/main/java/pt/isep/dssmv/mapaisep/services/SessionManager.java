package pt.isep.dssmv.mapaisep.services;

import android.content.Context;
import android.content.SharedPreferences;

/** Guarda no dispositivo apenas o estado e o tempo da sessão atual. */
public final class SessionManager {

    private static final String PREFS_NAME = "mapa_isep_session";
    private static final String KEY_SESSION_ACTIVE = "session_active";
    private static final String KEY_LAST_ACTIVE_AT = "last_active_at";
    private static final String KEY_LAST_PAGE_IS_MAP = "last_page_is_map";

    // Dois minutos em milissegundos.
    private static final long SESSION_TIMEOUT_MS = 2 * 60 * 1000L;

    private SessionManager() {
        // Esta classe só tem métodos estáticos.
    }

    private static SharedPreferences preferencias(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Cria uma sessão depois de o login ter sido aceite. */
    public static void iniciarSessao(Context context) {
        preferencias(context).edit()
                .putBoolean(KEY_SESSION_ACTIVE, true)
                .putLong(KEY_LAST_ACTIVE_AT, System.currentTimeMillis())
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

    /** Elimina a informação local da sessão. */
    public static void terminarSessao(Context context) {
        preferencias(context).edit().clear().apply();
    }
}
