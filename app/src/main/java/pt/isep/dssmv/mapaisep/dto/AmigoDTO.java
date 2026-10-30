package pt.isep.dssmv.mapaisep.dto;

/** Dados do amigo no formato recebido do Firestore. */
// Usado em: lista de amigos (alunos/{uid}/amigos).
public class AmigoDTO {

    private String uid;
    private String nome;
    private String numero;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }
}
