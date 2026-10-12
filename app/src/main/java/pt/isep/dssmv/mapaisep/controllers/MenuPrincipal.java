package pt.isep.dssmv.mapaisep.controllers;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import pt.isep.dssmv.mapaisep.R;

public class MenuPrincipal extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Loads the main menu XML layout.
        setContentView(R.layout.menu_principal);

        // Get references to the two content sections.
        View paginaInicio = findViewById(R.id.paginaInicio);
        View paginaMapa = findViewById(R.id.paginaMapa);

        // Get references to the bottom navigation buttons.
        View btnInicio = findViewById(R.id.btnInicio);
        View btnMapa = findViewById(R.id.btnMapa);

        // Show the main menu when the Inicio button is clicked.
        btnInicio.setOnClickListener(v -> {
            paginaInicio.setVisibility(View.VISIBLE);
            paginaMapa.setVisibility(View.GONE);
        });

        // Show the map section when the Mapa button is clicked.
        btnMapa.setOnClickListener(v -> {
            paginaInicio.setVisibility(View.GONE);
            paginaMapa.setVisibility(View.VISIBLE);
        });
    }
}