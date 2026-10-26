package pt.isep.dssmv.mapaisep.dto;

/** Valores da coleção Firestore locais, ainda no formato da API. */
public final class LocalDTO {
    public final String id;
    public final String nome;
    public final String tipo;
    public final String edificio;
    public final double latitude;
    public final double longitude;

    public LocalDTO(String id, String nome, String tipo, String edificio,
                         double latitude, double longitude) {
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.edificio = edificio;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
