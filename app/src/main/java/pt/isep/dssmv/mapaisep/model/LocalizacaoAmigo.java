package pt.isep.dssmv.mapaisep.model;

/** Representa a última posição que um amigo partilhou. */
public final class LocalizacaoAmigo {
    private final String uid;
    private final String nome;
    private final double latitude;
    private final double longitude;

    public LocalizacaoAmigo(String uid, String nome, double latitude, double longitude) {
        this.uid = uid;
        this.nome = nome;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getUid() { return uid; }
    public String getNome() { return nome; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
