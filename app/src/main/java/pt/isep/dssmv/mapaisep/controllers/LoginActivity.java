
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

public class LoginActivity extends AppCompatActivity {

    // Called when Android creates this Activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Load the login screen layout.
        setContentView(R.layout.activity_login);

        // Get references to the views defined in the XML layout.
        Button btnLogin = findViewById(R.id.btnLogin);
        EditText inputUser = findViewById(R.id.inputUtilizador);
        EditText inputPassword = findViewById(R.id.inputPassword);
        TextView errorLogin = findViewById(R.id.errorLogin);

        // Create the service responsible for Firebase authentication.
        AuthService authService = new AuthService();

        // Execute this code when the login button is clicked.
        btnLogin.setOnClickListener(clickedView -> {

            // Get the view currently receiving keyboard input.
            View focusedView = getCurrentFocus();

            if (focusedView != null) {

                // Get Android's virtual keyboard manager.
                InputMethodManager keyboard =
                        (InputMethodManager) getSystemService(
                                INPUT_METHOD_SERVICE
                        );

                if (keyboard != null) {

                    // Request the virtual keyboard to close.
                    keyboard.hideSoftInputFromWindow(
                            focusedView.getWindowToken(),
                            0
                    );
                }

                // Remove focus from the input field.
                focusedView.clearFocus();
            }

            // Read the student's credentials.
            String studentNumber = inputUser.getText().toString().trim();
            String password = inputPassword.getText().toString();

            // Validate the student number (exactly 7 digits).
            if (!studentNumber.matches("[0-9]{7}")) {

                errorLogin.setText(R.string.login_error_student);
                errorLogin.setVisibility(View.VISIBLE);
                return;
            }

            // Check if the password is empty.
            if (password.isEmpty()) {

                errorLogin.setText(
                        R.string.login_error_empty_password
                );

                errorLogin.setVisibility(View.VISIBLE);
                return;
            }

            // Check if the password exceeds 10 characters.
            if (password.length() > 10) {

                errorLogin.setText(
                        R.string.login_error_password_length
                );

                errorLogin.setVisibility(View.VISIBLE);
                return;
            }

            // Hide previous errors.
            errorLogin.setVisibility(View.GONE);

            // Disable the form while waiting for Firebase.
            btnLogin.setEnabled(false);
            inputUser.setEnabled(false);
            inputPassword.setEnabled(false);
            btnLogin.setText(R.string.login_loading);

            // Send the credentials to AuthService.
            // The callback communicates the authentication result.
            authService.login(
                    studentNumber,
                    password,

                    new AuthService.LoginCallback() {

                        // Implementation executed after successful login.
                        @Override
                        public void onSuccess(
                                String uid,
                                String idToken,
                                String refreshToken
                        ) {

                            // Avoid updating an Activity that was closed.
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            // Remove the password from the input field.
                            inputPassword.setText("");

                            // Display a success message.
                            Toast.makeText(
                                    LoginActivity.this,
                                    R.string.login_success,
                                    Toast.LENGTH_LONG
                            ).show();

                            // Create an Intent to open MenuPrincipal.
                            Intent intent = new Intent(
                                    LoginActivity.this,
                                    MenuPrincipal.class
                            );

                            // Open the main screen.
                            startActivity(intent);

                            // Close the login screen.
                            finish();
                        }

                        // Implementation executed when login fails.
                        @Override
                        public void onError(String errorCode) {

                            // Avoid updating an Activity that was closed.
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            // Re-enable the login form.
                            btnLogin.setEnabled(true);
                            inputUser.setEnabled(true);
                            inputPassword.setEnabled(true);
                            btnLogin.setText(R.string.login_button);

                            // Handle invalid credentials.
                            if (errorCode.equals("INVALID_LOGIN_CREDENTIALS")
                                    || errorCode.equals("INVALID_PASSWORD")
                                    || errorCode.equals("EMAIL_NOT_FOUND")) {

                                errorLogin.setText(
                                        R.string.login_error_credentials
                                );

                            } else if (
                                    errorCode.equals("NETWORK_ERROR")
                            ) {

                                // Handle connection problems.
                                errorLogin.setText(
                                        R.string.login_error_connection
                                );

                            } else {

                                // Handle other Firebase errors.
                                errorLogin.setText(
                                        R.string.login_error_server
                                );
                            }

                            // Display the error message.
                            errorLogin.setVisibility(View.VISIBLE);
                        }
                    }
            );
        });
    }
}
