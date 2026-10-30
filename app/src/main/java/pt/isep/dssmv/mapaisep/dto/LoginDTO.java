package pt.isep.dssmv.mapaisep.dto;

// Usado em: login. É o que a app envia ao Firebase Authentication.
// O JsonHandler.loginDTO2Json transforma-o no JSON do pedido.
public final class LoginDTO {
    private final String email;
    private final String password;

    public LoginDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
