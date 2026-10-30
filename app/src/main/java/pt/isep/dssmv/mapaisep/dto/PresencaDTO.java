package pt.isep.dssmv.mapaisep.dto;

// Usado em: marcar presença na biblioteca (coleção presencas).
// Só tem a hora da última deteção, em texto ISO (por exemplo "2026-10-30T10:00:00Z").
// Não tem Model: a presença só existe no Firebase, para a contagem da biblioteca.
public final class PresencaDTO {
    private final String atualizadoEm;

    public PresencaDTO(String atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public String getAtualizadoEm() {
        return atualizadoEm;
    }
}
