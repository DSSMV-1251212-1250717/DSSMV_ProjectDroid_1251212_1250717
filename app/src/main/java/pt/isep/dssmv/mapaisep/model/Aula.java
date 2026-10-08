package pt.isep.dssmv.mapaisep.model;

import java.time.LocalTime;

import pt.isep.dssmv.mapaisep.exceptions.InvalidDataException;

public class Aula {
    private String disciplina;
    private int diaSemana;          // 1 = segunda ... 7 = domingo
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private Sala sala;

    public Aula(String disciplina, int diaSemana, LocalTime horaInicio, LocalTime horaFim, Sala sala) {
        setDisciplina(disciplina);
        setDiaSemana(diaSemana);
        setHoras(horaInicio, horaFim);
        setSala(sala);
    }

    public String getDisciplina() { return disciplina; }

    public void setDisciplina(String disciplina) {
        if (disciplina == null || disciplina.trim().isEmpty()) {
            throw new InvalidDataException("Disciplina inválida");
        }
        this.disciplina = disciplina.trim();
    }

    public int getDiaSemana() { return diaSemana; }

    public void setDiaSemana(int diaSemana) {
        if (diaSemana < 1 || diaSemana > 7) {
            throw new InvalidDataException(diaSemana + " não é um dia da semana válido");
        }
        this.diaSemana = diaSemana;
    }

    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }

    // As duas horas validam-se juntas: o fim tem de ser depois do início
    public void setHoras(LocalTime horaInicio, LocalTime horaFim) {
        if (horaInicio == null || horaFim == null || !horaFim.isAfter(horaInicio)) {
            throw new InvalidDataException("Horas da aula inválidas");
        }
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
    }

    public Sala getSala() { return sala; }

    public void setSala(Sala sala) {
        if (sala == null) {
            throw new InvalidDataException("A aula tem de ter uma sala");
        }
        this.sala = sala;
    }

    /** true se a aula está a decorrer à hora indicada */
    public boolean estaADecorrer(LocalTime hora) {
        return !hora.isBefore(horaInicio) && hora.isBefore(horaFim);
    }

    @Override
    public String toString() {
        return horaInicio + "-" + horaFim + " " + disciplina + " (" + sala.getCodigo() + ")";
    }
}