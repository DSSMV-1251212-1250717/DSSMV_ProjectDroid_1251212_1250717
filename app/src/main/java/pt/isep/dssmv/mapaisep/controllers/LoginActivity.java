package pt.isep.dssmv.mapaisep.controllers;
// Package: organizes classes under a unique name to avoid naming conflicts.

import android.os.Bundle;                    // Stores key-value data, such as a saved Activity state.
import androidx.appcompat.app.AppCompatActivity; // Base class providing Android Activity functionality.
import pt.isep.dssmv.mapaisep.R;             // Provides IDs for application resources, such as XML layouts.
import pt.isep.dssmv.mapaisep.services.AuthService;
import android.content.Intent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;

public class LoginActivity extends AppCompatActivity {
    // Inherits methods for managing an Android Activity.

    @Override
    // Replaces the inherited onCreate() implementation with our own.
    protected void onCreate(Bundle savedInstanceState) {
        // Called by Android when this Activity is created.
        // savedInstanceState contains previously saved state, or null if none exists.

        super.onCreate(savedInstanceState);
        // Executes the parent class's onCreate() initialization.

        setContentView(R.layout.activity_login);
        // Loads res/layout/activity_login.xml and displays its interface.


        // Get references to the views created from the XML.
        Button btnLogin = findViewById(R.id.btnLogin);
        EditText inputUser = findViewById(R.id.inputUtilizador);
        EditText inputPassword = findViewById(R.id.inputPassword);
        TextView errorLogin = findViewById(R.id.errorLogin);

        // Creates the object responsible for contacting Back4App.
        AuthService authService = new AuthService();


        // setOnClickListener() registers a function to execute when the button is clicked.
        // The lambda contains the instructions to execute when the button is clicked.
        btnLogin.setOnClickListener(clickedView -> {
            // Instructions executed when the button is clicked.


            // Get the View currently receiving keyboard input.
            View focusedView = getCurrentFocus();

            if (focusedView != null) {
                // Get Android's virtual keyboard manager.
                InputMethodManager keyboard =
                        (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

                // Request the virtual keyboard to close.
                keyboard.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);

                // Remove focus from the input field.
                focusedView.clearFocus();
            }


            // Read the current text from both input fields.
            String studentNumber = inputUser.getText().toString();
            String password = inputPassword.getText().toString();


            // matches() checks if the entire string follows the given Regex pattern.
            // [0-9] matches one digit from 0 to 9.
            // {7} requires exactly 7 occurrences of the previous pattern.
            // ! negates the result (true becomes false and vice versa).
            if (!studentNumber.matches("[0-9]{7}")) {
                errorLogin.setText(R.string.login_error_student);
                errorLogin.setVisibility(View.VISIBLE);
                return;
            }


            // Check if the password is empty.
            if (password.isEmpty()) {
                errorLogin.setText(R.string.login_error_empty_password);
                errorLogin.setVisibility(View.VISIBLE);
                return;
            }


            // Check if the password exceeds 10 characters.
            if (password.length() > 10) {
                errorLogin.setText(R.string.login_error_password_length);
                errorLogin.setVisibility(View.VISIBLE);
                return;
            }


            // Hide previous errors if the input format is valid.
            errorLogin.setVisibility(View.GONE);

            // Prevents repeated requests and changes to the submitted credentials.
            btnLogin.setEnabled(false);
            inputUser.setEnabled(false);
            inputPassword.setEnabled(false);
            btnLogin.setText(R.string.login_loading);


            // This callback runs on the UI thread when the login request finishes.
            authService.login(studentNumber, password, (user, error) -> {

                // Ignore UI updates if this Activity has already been closed.
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // Restore the form after receiving the response.
                btnLogin.setEnabled(true);
                inputUser.setEnabled(true);
                inputPassword.setEnabled(true);
                btnLogin.setText(R.string.login_button);

                // Success requires an authenticated user and no error.
                if (error == null && user != null) {
                    inputPassword.setText("");

                    // Toast displays a temporary message on the screen.
                    // LoginActivity.this provides the current Activity's context.
                    // R.string.login_success identifies the message stored in strings.xml.
                    // Toast.LENGTH_LONG specifies how long the message is displayed.
                    // show() tells Android to display the message.
                    Toast successMessage = Toast.makeText(
                            LoginActivity.this,
                            R.string.login_success,
                            Toast.LENGTH_LONG
                    );
                    successMessage.show();

                    // Intent is an object used to tell Android what action we want to perform.
                    // Here, we specify that we want to open MenuPrincipal from LoginActivity.
                    // Creating the Intent does not open the new screen yet.
                    Intent intent = new Intent(
                            LoginActivity.this,
                            MenuPrincipal.class
                    );

                    // Requests Android to start the Activity specified in the Intent.
                    // Android starts MenuPrincipal and calls its lifecycle methods,
                    // including onCreate() when the Activity is created.
                    startActivity(intent);

                    // Requests Android to close LoginActivity,
                    // preventing the Back button from returning to the login screen.
                    finish();

                    // Stops executing the current callback function.
                    return;
                }

                // Show a useful message without exposing server error details.
                if (error != null &&
                        error.getCode() == ParseException.OBJECT_NOT_FOUND) {

                    errorLogin.setText(R.string.login_error_credentials);

                } else if (error != null &&
                        error.getCode() == ParseException.CONNECTION_FAILED) {

                    errorLogin.setText(R.string.login_error_connection);

                } else {

                    errorLogin.setText(R.string.login_error_server);
                }

                errorLogin.setVisibility(View.VISIBLE);
            });
        });
    }
}
