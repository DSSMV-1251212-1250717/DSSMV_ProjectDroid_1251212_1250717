package pt.isep.dssmv.mapaisep.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.services.AuthService;
import pt.isep.dssmv.mapaisep.services.SessionManager;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Se a sessão local ainda é válida (menos de 2 min), entra diretamente no menu.
        if (SessionManager.sessaoValida(this) && AuthService.restaurarSessao(this)) {

            SessionManager.registarAtividade(this);
            startActivity(new Intent(this, MenuPrincipal.class));
            finish();
            return;

        }

        AuthService.logout();
        SessionManager.terminarSessao(this);

        setContentView(R.layout.activity_login);

        Button btnLogin = findViewById(R.id.btnLogin);
        EditText inputUser = findViewById(R.id.inputUtilizador);
        EditText inputPassword = findViewById(R.id.inputPassword);
        TextView errorLogin = findViewById(R.id.errorLogin);

        AuthService authService = new AuthService();

        btnLogin.setOnClickListener(clickedView -> {
            // Fecha o teclado e tira o foco do campo.
            View focusedView = getCurrentFocus();
            if (focusedView != null) {
                InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (keyboard != null) {
                    keyboard.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                }
                focusedView.clearFocus();
            }

            String studentNumber = inputUser.getText().toString().trim();
            String password = inputPassword.getText().toString();

            // Validação local: número com 7 dígitos e password entre 1 e 10 caracteres.
            if (!studentNumber.matches("[0-9]{7}")) {
                errorLogin.setText(R.string.login_error_student);
                errorLogin.setVisibility(View.VISIBLE);
                return;
            }
            if (password.isEmpty()) {
                errorLogin.setText(R.string.login_error_empty_password);
                errorLogin.setVisibility(View.VISIBLE);
                return;
            }
            if (password.length() > 10) {
                errorLogin.setText(R.string.login_error_password_length);
                errorLogin.setVisibility(View.VISIBLE);
                return;
            }

            errorLogin.setVisibility(View.GONE);

            // Desativa o formulário enquanto espera pelo Firebase.
            btnLogin.setEnabled(false);
            inputUser.setEnabled(false);
            inputPassword.setEnabled(false);
            btnLogin.setText(R.string.login_loading);

            // O AuthService faz o pedido numa thread secundária e chama
            // onSuccess ou onError na main thread, através do Handler.
            authService.login(studentNumber, password, new AuthService.LoginCallback() {
                @Override
                public void onSuccess(String uid, String idToken, String refreshToken) {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    inputPassword.setText("");

                    // Guarda só o uid e o token; a password nunca fica no dispositivo.
                    SessionManager.iniciarSessao(LoginActivity.this, uid, idToken);

                    Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(LoginActivity.this, MenuPrincipal.class));
                    finish();
                }

                @Override
                public void onError(String errorCode) {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    btnLogin.setEnabled(true);
                    inputUser.setEnabled(true);
                    inputPassword.setEnabled(true);
                    btnLogin.setText(R.string.login_button);

                    if (errorCode.equals("INVALID_LOGIN_CREDENTIALS")
                            || errorCode.equals("INVALID_PASSWORD")
                            || errorCode.equals("EMAIL_NOT_FOUND")) {
                        errorLogin.setText(R.string.login_error_credentials);
                    } else if (errorCode.equals("NETWORK_ERROR")) {
                        errorLogin.setText(R.string.login_error_connection);
                    } else {
                        errorLogin.setText(R.string.login_error_server);
                    }
                    errorLogin.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}
