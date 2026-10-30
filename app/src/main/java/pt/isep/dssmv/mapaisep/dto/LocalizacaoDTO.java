package pt.isep.dssmv.mapaisep.dto;

/** Posição de um aluno, tal como vem do documento localizacoes/{uid} do Firestore. */
// Usado em: posição dos amigos no mapa (coleção localizacoes) e envio da posição do aluno.
// Só tem latitude e longitude. Não há Model Localizacao: o LocalizacaoAmigoMapper
// acrescenta estes dois valores ao Amigo a que a posição pertence.
public final class LocalizacaoDTO {
    private final double latitude;
    private final double longitude;

    public LocalizacaoDTO(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
