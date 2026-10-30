package pt.isep.dssmv.mapaisep.dto;

/**
 * Documento alunos/{uid}, tal como vem do Firestore.
 * Um campo que não exista no documento fica null (por exemplo, a foto).
 */
// Usado em: horário (turma), perfil (número, turma e foto) e adicionar amigo (nome e número).
// Também serve para enviar a foto nova (o JsonHandler escreve só o campo fotoBase64).
public final class AlunoDTO {
    private final String uid;
    private final String nome;
    private final String numero;
    private final String turmaId;
    private final String fotoBase64;

    public AlunoDTO(String uid, String nome, String numero, String turmaId, String fotoBase64) {
        this.uid = uid;
        this.nome = nome;
        this.numero = numero;
        this.turmaId = turmaId;
        this.fotoBase64 = fotoBase64;
    }

    public String getUid() {
        return uid;
    }

    public String getNome() {
        return nome;
    }

    public String getNumero() {
        return numero;
    }

    public String getTurmaId() {
        return turmaId;
    }

    public String getFotoBase64() {
        return fotoBase64;
    }
}
