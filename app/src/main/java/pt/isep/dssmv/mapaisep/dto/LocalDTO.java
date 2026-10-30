package pt.isep.dssmv.mapaisep.dto;

/** Valores da coleção Firestore locais, ainda no formato da API. */
// Usado em: salas e biblioteca no mapa (coleção locais).
// O id é o nome do documento (por exemplo "B201" ou "Biblioteca").
public final class LocalDTO {
    private final String id;
    private final String nome;
    private final double latitude;
    private final double longitude;

    public LocalDTO(String id, String nome, double latitude, double longitude) {
        this.id = id;
        this.nome = nome;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
