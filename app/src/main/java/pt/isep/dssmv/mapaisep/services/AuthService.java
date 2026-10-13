package pt.isep.dssmv.mapaisep.services;

import com.parse.LogInCallback;
import com.parse.ParseUser;

public class AuthService {

    // Authenticates an existing Back4App user without blocking the screen.
    // The student's number must be stored as the account's username.
    public void login(String studentNumber, String password, LogInCallback callback) {

        // The callback receives the user on success or an error on failure.
        // The result arrives later, after the server responds.
        // AuthService passes the callback function to the Parse SDK.
        // When Back4app responds, the SDK executes the callback function we provided,
        // passing two values:
        // user receives an object containing the authenticated user's data if login succeeds.
        // error receives an object describing the error if login fails.
        ParseUser.logInInBackground(studentNumber, password, callback);
    }
}