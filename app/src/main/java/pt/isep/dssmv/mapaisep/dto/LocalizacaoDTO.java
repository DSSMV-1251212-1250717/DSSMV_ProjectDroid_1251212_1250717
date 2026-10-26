package pt.isep.dssmv.mapaisep.dto;

/** Posição de um aluno, tal como vem do documento localizacoes/{uid} do Firestore. */
public final class LocalizacaoDTO {
    public final double latitude;
    public final double longitude;

    public LocalizacaoDTO(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
