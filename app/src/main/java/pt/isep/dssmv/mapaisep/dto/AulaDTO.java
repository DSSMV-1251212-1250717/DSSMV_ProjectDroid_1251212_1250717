package pt.isep.dssmv.mapaisep.dto;

// Usado em: horário do aluno e horário de um amigo (coleção aulas).
public class AulaDTO {
    // Dados recebidos da API do Firestore.
    // O DTO só transporta dados; não tem lógica.

    private String nome;
    private int diaSemana;
    private String horaInicio;
    private String horaFim;
    private String salaId;
    private String turmaId;

    // Construtor vazio: cria o DTO antes de preencher os campos.
    public AulaDTO() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(int diaSemana) {
        this.diaSemana = diaSemana;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(String horaFim) {
        this.horaFim = horaFim;
    }

    public String getSalaId() {
        return salaId;
    }

    public void setSalaId(String salaId) {
        this.salaId = salaId;
    }

    public String getTurmaId() {
        return turmaId;
    }

    public void setTurmaId(String turmaId) {
        this.turmaId = turmaId;
    }
}
