package pt.isep.dssmv.mapaisep.model;

import pt.isep.dssmv.mapaisep.exceptions.InvalidDataException;

// Um local fixo do campus, como uma sala ou a biblioteca.
// O id é o nome do documento em locais (por exemplo "B201"); é igual ao salaId das aulas.
// Information Expert (GRASP): esta classe tem os dados, por isso é ela que os valida.
// Se algum for inválido, lança InvalidDataException e o objeto não chega a ser criado.
public final class Local {
    private String id;
    private String nome;
    private double latitude;
    private double longitude;

    public Local(String id, String nome, double latitude, double longitude) {
        setId(id);
        setNome(nome);
        setCoordenadas(latitude, longitude);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidDataException("Local sem identificador");
        }
        this.id = id.trim();
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new InvalidDataException("Nome do local inválido");
        }
        this.nome = nome.trim();
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    // As duas coordenadas validam-se juntas: latitude de -90 a 90, longitude de -180 a 180.
    public void setCoordenadas(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new InvalidDataException("Coordenadas do local inválidas");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
