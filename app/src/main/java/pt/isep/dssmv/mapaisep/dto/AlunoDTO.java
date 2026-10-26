package pt.isep.dssmv.mapaisep.dto;

/**
 * Documento alunos/{uid}, tal como vem do Firestore.
 * Um campo que não exista no documento fica null (por exemplo, a foto).
 */
public final class AlunoDTO {
    public final String uid;
    public final String nome;
    public final String numero;
    public final String turmaId;
    public final String fotoBase64;

    public AlunoDTO(String uid, String nome, String numero, String turmaId, String fotoBase64) {
        this.uid = uid;
        this.nome = nome;
        this.numero = numero;
        this.turmaId = turmaId;
        this.fotoBase64 = fotoBase64;
    }
}
