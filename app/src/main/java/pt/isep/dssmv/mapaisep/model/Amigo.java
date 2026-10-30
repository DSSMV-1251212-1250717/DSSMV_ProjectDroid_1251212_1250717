package pt.isep.dssmv.mapaisep.model;

import pt.isep.dssmv.mapaisep.exceptions.InvalidDataException;

// Um aluno da lista de amigos do aluno com sessão iniciada.
// A posição (latitude e longitude) fica null enquanto o amigo não a partilhar.
// Information Expert (GRASP): esta classe tem os dados, por isso é ela que os valida.
// Se algum for inválido, lança InvalidDataException e o objeto não chega a ser criado.
public class Amigo {

    private String uid;
    private String nome;
    private String numero;
    private Double latitude;
    private Double longitude;

    public Amigo(String uid, String nome, String numero) {
        setUid(uid);
        setNome(nome);
        setNumero(numero);
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new InvalidDataException("Amigo sem identificador");
        }
        this.uid = uid.trim();
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new InvalidDataException("Nome do amigo inválido");
        }
        this.nome = nome.trim();
    }

    public String getNumero() {
        return numero;
    }

    // O número de estudante tem exatamente 7 dígitos (por exemplo 1250717).
    public void setNumero(String numero) {
        if (numero == null || !numero.trim().matches("[0-9]{7}")) {
            throw new InvalidDataException("Número de estudante inválido");
        }
        this.numero = numero.trim();
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    // Chamado quando chega a posição do amigo (coleção localizacoes).
    // A latitude vai de -90 a 90 e a longitude de -180 a 180.
    public void definirPosicao(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new InvalidDataException("Posição inválida");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // true se o amigo já partilhou a posição.
    public boolean temPosicao() {
        return latitude != null && longitude != null;
    }
}
