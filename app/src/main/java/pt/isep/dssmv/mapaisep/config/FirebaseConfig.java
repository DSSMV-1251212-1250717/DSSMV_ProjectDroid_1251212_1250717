package pt.isep.dssmv.mapaisep.config;

// Reúne a configuração Firebase usada pelos serviços.
public final class FirebaseConfig {
    // Este identificador é utilizado no endereço dos pedidos HTTP
    // para indicar ao Firestore a que projeto pretendemos aceder.
    public static final String PROJECT_ID = "mapa-isep";

    // Identifica a base de dados dentro do projeto Firebase.
    public static final String DATABASE_ID = "(default)";

    // Identifica o projeto nos pedidos de autenticação; não substitui o token do utilizador.
    public static final String API_KEY = "AIzaSyAIGfLx9oC9oncQ3TCyuzQX3CTqckxnYzE";

    // Tem de corresponder ao domínio dos emails criados no Firebase Authentication.
    public static final String EMAIL_DOMAIN = "@alunos.example.com";

    public static final String AUTH_URL =
    "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="
    + API_KEY;

    // Endpoint que troca um refreshToken por um idToken novo.
    public static final String TOKEN_REFRESH_URL =
            "https://securetoken.googleapis.com/v1/token?key=" + API_KEY;

    public static final String FIRESTORE_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID
            + "/databases/" + DATABASE_ID + "/documents";

    private FirebaseConfig() {
    }
}
