package pt.isep.dssmv.mapaisep.controllers;

import com.parse.LogInCallback;
import com.parse.ParseUser;

public class AuthService {

    // Authenticates an existing Back4App user without blocking the screen.
    // The student's number must be stored as the account's username.
    public void login(String studentNumber, String password, LogInCallback callback) {
        // The callback receives the user on success or an error on failure.
        // The result arrives later, after the server responds.
        ParseUser.logInInBackground(studentNumber, password, callback);
    }
}
