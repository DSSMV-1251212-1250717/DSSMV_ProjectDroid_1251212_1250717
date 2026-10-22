package pt.isep.dssmv.mapaisep.model;

/** Um local fixo do campus, como uma sala ou a biblioteca. */
public final class LocalMapa {
    private final String id;
    private final String nome;
    private final String tipo;
    private final String edificio;
    private final double latitude;
    private final double longitude;

    public LocalMapa(String id, String nome, String tipo, String edificio,
                     double latitude, double longitude) {
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.edificio = edificio;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getTipo() { return tipo; }
    public String getEdificio() { return edificio; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
