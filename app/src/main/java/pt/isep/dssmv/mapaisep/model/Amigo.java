package pt.isep.dssmv.mapaisep.model;

/**
 * Um aluno da lista de amigos do utilizador atual.
 * A posição (latitude e longitude) fica null enquanto o amigo não a partilhar.
 */
public class Amigo {

    private final String uid;
    private final String nome;
    private final String numero;
    private Double latitude;
    private Double longitude;

    public Amigo(String uid, String nome, String numero) {
        this.uid = uid;
        this.nome = nome;
        this.numero = numero;
    }

    public String getUid() {
        return uid;
    }

    public String getNome() {
        return nome;
    }

    public String getNumero() {
        return numero;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    // Chamado quando chega a posição do amigo (coleção localizacoes).
    public void definirPosicao(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // true se o amigo já partilhou a posição.
    public boolean temPosicao() {
        return latitude != null && longitude != null;
    }
}
