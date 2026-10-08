package pt.isep.dssmv.mapaisep.model;

import pt.isep.dssmv.mapaisep.exceptions.InvalidDataException;

public class Aluno {
    private int numero;
    private String nome;
    private String email;
    private Turma turma;

    public Aluno(int numero, String nome, String email, Turma turma) {
        setNumero(numero);
        setNome(nome);
        setEmail(email);
        setTurma(turma);
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        if (numero < 1000000 || numero > 9999999) {
            throw new InvalidDataException(numero + " não é um número de aluno válido");
        }
        this.numero = numero;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new InvalidDataException("Nome inválido");
        }
        this.nome = nome.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new InvalidDataException(email + " não é um email válido");
        }
        this.email = email.trim();
    }

    public Turma getTurma() {
        return turma;
    }

    public void setTurma(Turma turma) {
        if (turma == null) {
            throw new InvalidDataException("O aluno tem de ter uma turma");
        }
        this.turma = turma;
    }

    @Override
    public String toString() {
        return numero + " - " + nome + " (" + turma.getCodigo() + ")";
    }
}