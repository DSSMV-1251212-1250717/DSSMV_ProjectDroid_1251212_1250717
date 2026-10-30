package pt.isep.dssmv.mapaisep.dto;

/** Posição de um aluno, tal como vem do documento localizacoes/{uid} do Firestore. */
// Usado em: posição dos amigos no mapa (coleção localizacoes) e envio da posição do aluno.
// Tem a latitude, a longitude e a hora em que a posição foi enviada (atualizadoEm).
// Não há Model Localizacao: o LocalizacaoAmigoMapper acrescenta estes valores
// ao Amigo a que a posição pertence.
public final class LocalizacaoDTO {
    private final double latitude;
    private final double longitude;

    // Hora do envio em texto ISO 8601, por exemplo "2026-10-30T13:51:00Z".
    // null se o documento for antigo e não a tiver.
    private final String atualizadoEm;

    public LocalizacaoDTO(double latitude, double longitude) {
        this(latitude, longitude, null);
    }

    public LocalizacaoDTO(double latitude, double longitude, String atualizadoEm) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.atualizadoEm = atualizadoEm;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getAtualizadoEm() {
        return atualizadoEm;
    }
}
