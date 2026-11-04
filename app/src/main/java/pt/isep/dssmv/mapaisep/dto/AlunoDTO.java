package pt.isep.dssmv.mapaisep.dto;

// Documento alunos/{uid}, tal como vem do Firestore; um campo que não exista fica null (ex.: a foto).
// Usado no horário, no perfil, ao adicionar amigo e para enviar a foto nova.
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
