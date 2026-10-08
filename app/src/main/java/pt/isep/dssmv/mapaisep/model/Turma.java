package pt.isep.dssmv.mapaisep.model;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import pt.isep.dssmv.mapaisep.exceptions.InvalidDataException;

public class Turma {
    private String codigo;
    private ArrayList<Aula> aulas;

    public Turma(String codigo) {
        setCodigo(codigo);
        this.aulas = new ArrayList<Aula>();
    }

    public String getCodigo() { return codigo; }

    public void setCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new InvalidDataException("Código da turma inválido");
        }
        this.codigo = codigo.trim().toUpperCase();
    }

    public void addAula(Aula aula) {
        if (aula == null) {
            throw new InvalidDataException("Aula inválida");
        }
        aulas.add(aula);
    }

    /** Devolve uma cópia, para ninguém alterar a lista por fora (encapsulamento) */
    public ArrayList<Aula> getAulas() {
        return new ArrayList<Aula>(aulas);
    }

    /** Aulas de um dia, ordenadas pela hora de início */
    public ArrayList<Aula> getAulasDoDia(int diaSemana) {
        ArrayList<Aula> doDia = new ArrayList<Aula>();
        for (int i = 0; i < aulas.size(); i++) {
            if (aulas.get(i).getDiaSemana() == diaSemana) {
                doDia.add(aulas.get(i));
            }
        }
        Collections.sort(doDia, new Comparator<Aula>() {
            @Override
            public int compare(Aula a1, Aula a2) {
                return a1.getHoraInicio().compareTo(a2.getHoraInicio());
            }
        });
        return doDia;
    }

    /**
     * Aula a decorrer agora ou, se não houver, a próxima do dia.
     * Devolve null se já não houver mais aulas nesse dia.
     */
    public Aula getProximaAula(int diaSemana, LocalTime agora) {
        ArrayList<Aula> doDia = getAulasDoDia(diaSemana);
        for (int i = 0; i < doDia.size(); i++) {
            Aula aula = doDia.get(i);
            if (aula.estaADecorrer(agora) || aula.getHoraInicio().isAfter(agora)) {
                return aula;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Turma " + codigo + " (" + aulas.size() + " aulas)";
    }
}