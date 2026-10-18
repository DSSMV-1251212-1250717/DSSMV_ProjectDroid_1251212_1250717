
package pt.isep.dssmv.mapaisep.dto;

public class AulaDTO {

    // Data received from the Firestore API.
    // DTOs transport data without implementing business logic.

    private String nome;
    private int diaSemana;
    private String horaInicio;
    private String horaFim;
    private String salaId;
    private String turmaId;

    // Empty constructor used to create a DTO before filling its fields.
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
