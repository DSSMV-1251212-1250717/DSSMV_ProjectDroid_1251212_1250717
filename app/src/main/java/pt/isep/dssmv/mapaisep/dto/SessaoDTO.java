package pt.isep.dssmv.mapaisep.dto;

// Usado em: resposta do login e da renovação do token.
// É o que o Firebase devolve: quem é o aluno (uid), os dois tokens e quantos segundos
// faltam para o idToken expirar. Não tem Model: o AuthService guarda estes dados
// em memória e o SessaoHelper guarda-os no telemóvel.
public final class SessaoDTO {
    private final String uid;
    private final String idToken;
    private final String refreshToken;
    private final long expiraEmSegundos;

    public SessaoDTO(String uid, String idToken, String refreshToken, long expiraEmSegundos) {
        this.uid = uid;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.expiraEmSegundos = expiraEmSegundos;
    }

    public String getUid() {
        return uid;
    }

    public String getIdToken() {
        return idToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiraEmSegundos() {
        return expiraEmSegundos;
    }
}
