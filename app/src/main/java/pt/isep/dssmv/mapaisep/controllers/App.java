package pt.isep.dssmv.mapaisep.controllers;

import android.app.Application;
import com.parse.Parse;

public class App extends Application {
    //Application --> Allows initialization code to run when the application process starts.

    @Override
    public void onCreate() {
        //o codigo que queremos executar no momento
        super.onCreate();
        //executamos primeiro o funcionamento da classe herdada

        // Initializes the Parse SDK before any Activity is created.
        // These values identify the Back4App app, not a student's account.
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("HYCCuuPN1z0NXawDHBbztGgYisguDgBGg4ylL8jS")
                .clientKey("oak1CmJwX5zppo6njtmz2Xh9Va4F3gQhd1o4yBVW")
                .server("https://parseapi.back4app.com/")
                .build());
    }
}
