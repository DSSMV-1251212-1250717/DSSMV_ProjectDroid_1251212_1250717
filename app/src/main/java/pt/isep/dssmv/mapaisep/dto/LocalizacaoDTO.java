package pt.isep.dssmv.mapaisep.dto;

/** Dados da localização no formato que chega do JSON do Firestore. */
public final class LocalizacaoDTO {
    public final String uid;
    public final String nome;
    public final double latitude;
    public final double longitude;

    public LocalizacaoDTO(String uid, String nome, double latitude, double longitude) {
        this.uid = uid;
        this.nome = nome;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
