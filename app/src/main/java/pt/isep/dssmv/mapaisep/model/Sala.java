package pt.isep.dssmv.mapaisep.model;

import pt.isep.dssmv.mapaisep.exceptions.InvalidDataException;

public class Sala {
    // Room identifier, for example "B201".
    private String codigo;

    // Building where the room is located.
    // Null until the building is configured.
    private String edificio;

    // Floor where the room is located.
    // Null until configured. Negative values can represent basements.
    private Integer piso;

    // Geographic coordinates for the mapping system.
    // Null until the location is configured.
    private Double latitude;
    private Double longitude;

    // Creates a room using only its identifier.
    // Other information can be added later.
    public Sala(String codigo) {
        setCodigo(codigo);

        this.edificio = null;
        this.piso = null;
        this.latitude = null;
        this.longitude = null;
    }

    // Creates a room with its building and floor.
    public Sala(String codigo, String edificio, int piso) {
        this(codigo);
        setEdificio(edificio);
        setPiso(piso);
    }

    // Creates a room with geographic coordinates.
    public Sala(String codigo, double latitude, double longitude) {
        this(codigo);
        setLocalizacao(latitude, longitude);
    }

    // Creates a room with all available information.
    public Sala(String codigo, String edificio, int piso, double latitude, double longitude) {
        this(codigo, edificio, piso);
        setLocalizacao(latitude, longitude);
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new InvalidDataException("Invalid room code");
        }

        this.codigo = codigo.trim();
    }

    public String getEdificio() {
        return edificio;
    }

    public void setEdificio(String edificio) {
        if (edificio == null || edificio.trim().isEmpty()) {
            throw new InvalidDataException("Invalid building");
        }

        this.edificio = edificio.trim();
    }

    public Integer getPiso() {
        return piso;
    }

    public void setPiso(int piso) {
        this.piso = piso;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    // Checks whether the room has geographic coordinates.
    public boolean temLocalizacao() {
        return latitude != null && longitude != null;
    }

    // TODO: The mapping system will define the room's coordinates.
    // Latitude and longitude identify geographic positions.
    // Building and floor identify the room within the campus.
    public void setLocalizacao(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new InvalidDataException("Invalid room coordinates");
        }

        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Removes the previously configured geographic location.
    public void removerLocalizacao() {
        latitude = null;
        longitude = null;
    }

    @Override

    public String toString() {
        return codigo;
    }
}
