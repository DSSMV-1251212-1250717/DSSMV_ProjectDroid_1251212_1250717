package pt.isep.dssmv.mapaisep.model;

import pt.isep.dssmv.mapaisep.exceptions.InvalidDataException;

public class Sala {
    private String codigo;
    private String edificio;
    private int piso;
    private int x;
    private int y;

    public Sala(String codigo, String edificio, int piso, int x, int y) {
        setCodigo(codigo);
        setEdificio(edificio);
        setPiso(piso);
        this.x = x;
        this.y = y;
    }

    public String getCodigo() { return codigo; }

    public void setCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new InvalidDataException("Código da sala inválido");
        }
        this.codigo = codigo.trim().toUpperCase();
    }

    public String getEdificio() { return edificio; }

    public void setEdificio(String edificio) {
        if (edificio == null || edificio.trim().isEmpty()) {
            throw new InvalidDataException("Edifício inválido");
        }
        this.edificio = edificio.trim().toUpperCase();
    }

    public int getPiso() { return piso; }

    public void setPiso(int piso) {
        if (piso < -1 || piso > 10) {
            throw new InvalidDataException(piso + " não é um piso válido");
        }
        this.piso = piso;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public String toString() {
        return "Sala " + codigo + " (Edifício " + edificio + ", piso " + piso + ")";
    }
}