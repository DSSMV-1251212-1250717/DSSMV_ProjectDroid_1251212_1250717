
package pt.isep.dssmv.mapaisep;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pt.isep.dssmv.mapaisep.service.AuthService;
import pt.isep.dssmv.mapaisep.helper.SessaoHelper;
import pt.isep.dssmv.mapaisep.ui.MenuPrincipalActivity;

public class LoginActivity extends AppCompatActivity {

    // O onCreate é o método que o Android disponibiliza para prepararmos o ecrã quando ele é criado:
    // carregar o layout, ir buscar os botões e campos, e dizer o que cada botão faz.
    // É o Android que o chama quando cria o ecrã; o nosso código nunca o chama.
    //
    // savedInstanceState: o que o ecrã tinha antes de o Android o destruir e criar de novo
    // (por exemplo, ao rodar o telemóvel), como o texto escrito nos campos
    // (por exemplo, o número de estudante que já estava escrito).
    // Na primeira vez vem null. O nosso código não o lê; o super.onCreate usa-o para repor o ecrã.
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Corre primeiro o onCreate da classe-mãe, que faz a preparação interna do ecrã:
        // aplica o tema, liga o ecrã ao sistema e recupera o estado guardado, se houver.
        // Como reescrevemos o onCreate, sem esta linha essa preparação não acontecia
        // e a app ia abaixo. O Android obriga a chamá-lo.
        super.onCreate(savedInstanceState);

        // Se usamos a app há menos de 2 minutos, não pede o login outra vez.
        if (SessaoHelper.sessaoValida(this) && AuthService.restaurarSessao(this)) {
            SessaoHelper.registarAtividade(this);
            startActivity(new Intent(this, MenuPrincipalActivity.class));
            finish();
            return;
        }

        AuthService.logout();
        SessaoHelper.terminarSessao(this);

        // Carrega o layout do ecrã de login.
        setContentView(R.layout.activity_login);

        // Vai buscar os elementos do layout.
        Button btnLogin = findViewById(R.id.btnLogin);
        EditText inputUser = findViewById(R.id.inputUtilizador);
        EditText inputPassword = findViewById(R.id.inputPassword);
        TextView errorLogin = findViewById(R.id.errorLogin);

        // Serviço que faz o login no Firebase.
        AuthService authService = new AuthService();

        // Quando se carrega em Entrar.
        btnLogin.setOnClickListener(clickedView -> {

            // Elemento que tem o teclado.
            View focusedView = getCurrentFocus();

            if (focusedView != null) {

                // Gestor do teclado do Android.
                InputMethodManager keyboard =
                        (InputMethodManager) getSystemService(
                                INPUT_METHOD_SERVICE
                        );

                if (keyboard != null) {

                    // Fecha o teclado.
                    keyboard.hideSoftInputFromWindow(
                            focusedView.getWindowToken(),
                            0
                    );
                }

                // Tira o foco do campo.
                focusedView.clearFocus();
            }

            // Lê o número e a password.
            String studentNumber = inputUser.getText().toString().trim();
            String password = inputPassword.getText().toString();

            // O número tem de ter exatamente 7 dígitos.
            if (!studentNumber.matches("[0-9]{7}")) {

                errorLogin.setText(R.string.login_error_student);
                errorLogin.setVisibility(View.VISIBLE);
                return;
            }

            // A password não pode estar vazia.
            if (password.isEmpty()) {

                errorLogin.setText(
                        R.string.login_error_empty_password
                );

                errorLogin.setVisibility(View.VISIBLE);
                return;
            }

            // A password não pode ter mais de 10 caracteres.
            if (password.length() > 10) {

                errorLogin.setText(
                        R.string.login_error_password_length
                );

                errorLogin.setVisibility(View.VISIBLE);
                return;
            }

            // Esconde erros anteriores.
            errorLogin.setVisibility(View.GONE);

            // Bloqueia o formulário enquanto espera pelo Firebase.
            btnLogin.setEnabled(false);
            inputUser.setEnabled(false);
            inputPassword.setEnabled(false);
            btnLogin.setText(R.string.login_loading);

            /*
             * LoginActivity cria o objeto com onSuccess e onError
             *                         ↓
             * Passa esse objeto ao AuthService
             *                         ↓
             * AuthService faz o pedido numa thread secundária
             *                         ↓
             * Firebase responde
             *                         ↓
             * AuthService agenda a chamada através do Handler
             *                         ↓
             * Main thread executa onSuccess ou onError desse objeto
             *                         ↓
             * O código definido na LoginActivity abre o menu ou mostra o erro
             */
            authService.login(
                    studentNumber,
                    password,

                    new AuthService.LoginCallback() {
                        // O AuthService não sabe o que fazer com o resultado do login:
                        // dá uma interface (LoginCallback) que este ecrã preenche.

                        // Corre quando o login correu bem.
                        @Override
                        public void onSuccess(
                                String uid,
                                String idToken,
                                String refreshToken,
                                long idTokenExpiresAt
                        ) {

                            // Se o ecrã já fechou, não faz nada.
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            // Apaga a password do campo.
                            inputPassword.setText("");

                            // Guarda só os dados da sessão do Firebase (2 minutos);
                            // a password nunca fica guardada no telemóvel.
                            SessaoHelper.iniciarSessao(
                                    LoginActivity.this,
                                    uid,
                                    idToken,
                                    refreshToken,
                                    idTokenExpiresAt
                            );

                            // Mensagem de sucesso.
                            Toast.makeText(
                                    LoginActivity.this,
                                    R.string.login_success,
                                    Toast.LENGTH_LONG
                            ).show();

                            // Pedido para abrir o MenuPrincipalActivity.
                            Intent intent = new Intent(
                                    LoginActivity.this,
                                    MenuPrincipalActivity.class
                            );

                            // Abre o ecrã principal.
                            startActivity(intent);

                            // Fecha o ecrã de login.
                            finish();
                        }

                        // Corre quando o login falhou.
                        @Override
                        public void onError(String errorCode) {

                            // Se o ecrã já fechou, não faz nada.
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            // Volta a ativar o formulário.
                            btnLogin.setEnabled(true);
                            inputUser.setEnabled(true);
                            inputPassword.setEnabled(true);
                            btnLogin.setText(R.string.login_button);

                            // Número ou password errados.
                            if (errorCode.equals("INVALID_LOGIN_CREDENTIALS")
                                    || errorCode.equals("INVALID_PASSWORD")
                                    || errorCode.equals("EMAIL_NOT_FOUND")) {

                                errorLogin.setText(
                                        R.string.login_error_credentials
                                );

                            } else if (
                                    errorCode.equals("NETWORK_ERROR")
                            ) {

                                // Sem ligação ao servidor.
                                errorLogin.setText(
                                        R.string.login_error_connection
                                );

                            } else {

                                // Outros erros do Firebase.
                                errorLogin.setText(
                                        R.string.login_error_server
                                );
                            }

                            // Mostra a mensagem de erro.
                            errorLogin.setVisibility(View.VISIBLE);
                        }
                    }
            );
        });
    }
}
