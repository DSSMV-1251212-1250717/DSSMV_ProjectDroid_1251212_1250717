package pt.isep.dssmv.mapaisep.model;

/** Representa um aluno que pertence à lista de amigos do utilizador atual. */
public class Amigo {

    private final String uid;
    private final String nome;
    private final String numero;

    public Amigo(String uid, String nome, String numero) {
        this.uid = uid;
        this.nome = nome;
        this.numero = numero;
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
}
