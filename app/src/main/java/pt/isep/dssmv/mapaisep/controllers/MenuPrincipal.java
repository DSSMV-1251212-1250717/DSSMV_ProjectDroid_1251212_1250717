package pt.isep.dssmv.mapaisep.controllers;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import java.util.Arrays;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import com.parse.ParseObject;
import com.parse.ParseUser;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.services.HorarioService;
import pt.isep.dssmv.mapaisep.services.SessionManager;

public class MenuPrincipal extends AppCompatActivity implements OnMapReadyCallback {
    private static final LatLng ISEP = new LatLng(41.178194, -8.607083);
    private static final LatLngBounds CAMPUS_ISEP = new LatLngBounds(
            new LatLng(41.1770955, -8.6091907),
            new LatLng(41.1795948, -8.6056156));
    private static final List<LatLng> CONTORNO_ISEP = Arrays.asList(
            new LatLng(41.1788413, -8.6091907), new LatLng(41.1787122, -8.6091844),
            new LatLng(41.1781764, -8.6091645), new LatLng(41.1775921, -8.6091508),
            new LatLng(41.1774962, -8.6091237), new LatLng(41.1774235, -8.60907),
            new LatLng(41.1773791, -8.608952), new LatLng(41.1772868, -8.6086727),
            new LatLng(41.177153, -8.6083029), new LatLng(41.1771157, -8.6081637),
            new LatLng(41.1770955, -8.6080457), new LatLng(41.1771197, -8.6079492),
            new LatLng(41.1771682, -8.6078687), new LatLng(41.1775591, -8.6074688),
            new LatLng(41.1778014, -8.6072181), new LatLng(41.1778975, -8.6071315),
            new LatLng(41.1782186, -8.6067965), new LatLng(41.1782727, -8.606743),
            new LatLng(41.1783187, -8.6066959), new LatLng(41.1787766, -8.6062276),
            new LatLng(41.1788154, -8.6061922), new LatLng(41.1788431, -8.6061662),
            new LatLng(41.1788603, -8.6061505), new LatLng(41.1790416, -8.6059858),
            new LatLng(41.1793091, -8.6057709), new LatLng(41.1795948, -8.6056156),
            new LatLng(41.1795477, -8.607945), new LatLng(41.1795443, -8.6081136),
            new LatLng(41.1795441, -8.608126), new LatLng(41.1795261, -8.6088933),
            new LatLng(41.1791749, -8.6088826), new LatLng(41.1788569, -8.6087911));

    private GoogleMap mapa;
    private ActivityResultLauncher<String[]> permissaoLocalizacao;
    private LocationManager gestorLocalizacao;
    private LocationListener ouvinteLocalizacao;
    private Circle pontoLocalizacao;
    private boolean atualizacoesLocalizacaoAtivas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gestorLocalizacao = (LocationManager) getSystemService(LOCATION_SERVICE);
        ouvinteLocalizacao = new LocationListener() {
            @Override
            public void onLocationChanged(Location localizacao) {
                atualizarPontoLocalizacao(localizacao);
            }
        };
        setContentView(R.layout.menu_principal);

        // Finds the text area that displays the loading state, lessons or an error.
        TextView textoAulas = findViewById(R.id.textoAulas);

        HorarioService horarioService = new HorarioService();

        HorarioService.HorarioCallback callback = new HorarioService.HorarioCallback() {

            @Override
            public void onResult(List<ParseObject> aulas, String erro) {

                // Do not update a screen that has already been closed.
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // Check if the request failed.
                if (erro != null) {
                    String mensagemErro = getString(R.string.horario_error, erro);
                    textoAulas.setText(mensagemErro);
                    return;
                }

                // A successful request may return no lessons for today.
                if (aulas.isEmpty()) {
                    textoAulas.setText(R.string.horario_empty);
                    return;
                }

                // Build one text containing all lessons, separated by a blank line.
                StringBuilder horario = new StringBuilder();
                for (int i = 0; i < aulas.size(); i++) {
                    ParseObject aula = aulas.get(i);

                    if (horario.length() > 0) {
                        horario.append("\n\n");
                    }

                    String nome = aula.getString("nome");
                    String horaInicio = aula.getString("horaInicio");
                    String horaFim = aula.getString("horaFim");
                    String textoAula = getString(R.string.horario_lesson, nome, horaInicio, horaFim);

                    horario.append(textoAula);
                }

                // Display the result in the layout instead of Logcat.
                String textoHorario = horario.toString();
                textoAulas.setText(textoHorario);
            }
        };

        horarioService.obterAulasHoje(callback);

        permissaoLocalizacao = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), resultados -> {
                    if (temPermissaoLocalizacao()) {
                        ativarLocalizacao();
                    } else {
                        Toast.makeText(this, "A localização precisa de permissão.", Toast.LENGTH_LONG).show();
                    }
                });

        View paginaInicio = findViewById(R.id.paginaInicio);
        View paginaMapa = findViewById(R.id.paginaMapa);
        mostrarPagina(SessionManager.ultimaPaginaEraMapa(this), paginaInicio, paginaMapa);

        SupportMapFragment fragmentoMapa = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapaGoogle);
        if (fragmentoMapa != null) {
            fragmentoMapa.getMapAsync(this);
        }

        findViewById(R.id.btnInicio).setOnClickListener(v -> {
            mostrarPagina(false, paginaInicio, paginaMapa);
        });
        findViewById(R.id.btnMapa).setOnClickListener(v -> {
            mostrarPagina(true, paginaInicio, paginaMapa);
            pedirPermissaoOuAtivarLocalizacao();
        });

        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(anchor -> {
            PopupMenu menuConta = new PopupMenu(this, anchor);
            menuConta.getMenuInflater().inflate(R.menu.menu_account, menuConta.getMenu());
            menuConta.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_logout) {
                    terminarSessao();
                    return true;
                }
                return false;
            });
            menuConta.show();
        });
    }

    private void terminarSessao() {
        ParseUser.logOut();
        SessionManager.terminarSessao(this);

        Intent irParaLogin = new Intent(this, LoginActivity.class);
        irParaLogin.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(irParaLogin);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;
        mapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mapa.setBuildingsEnabled(true);
        mapa.setLatLngBoundsForCameraTarget(CAMPUS_ISEP);
        mapa.setMinZoomPreference(16f);
        mapa.setMaxZoomPreference(21f);
        mapa.getUiSettings().setZoomControlsEnabled(true);
        adicionarMascaraExterior();
        mapa.addMarker(new MarkerOptions().position(ISEP).title("ISEP"));
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.17827, -8.60727), 19f));

        if (temPermissaoLocalizacao()) {
            ativarLocalizacao();
        }
    }

    private void adicionarMascaraExterior() {
        List<LatLng> limiteExterior = Arrays.asList(
                new LatLng(41.25, -8.70), new LatLng(41.25, -8.50),
                new LatLng(41.10, -8.50), new LatLng(41.10, -8.70));
        mapa.addPolygon(new PolygonOptions()
                .addAll(limiteExterior)
                .addHole(CONTORNO_ISEP)
                .strokeWidth(0f)
                .fillColor(Color.WHITE)
                .clickable(false));
    }

    private void pedirPermissaoOuAtivarLocalizacao() {
        if (temPermissaoLocalizacao()) {
            ativarLocalizacao();
        } else {
            permissaoLocalizacao.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private boolean temPermissaoLocalizacao() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("MissingPermission")
    private void ativarLocalizacao() {
        if (mapa != null && temPermissaoLocalizacao()) {
            mapa.setMyLocationEnabled(true);
            mapa.getUiSettings().setMyLocationButtonEnabled(true);
            iniciarAtualizacoesLocalizacao();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void iniciarAtualizacoesLocalizacao() {
        if (gestorLocalizacao == null || atualizacoesLocalizacaoAtivas
                || !temPermissaoLocalizacao()) {
            return;
        }

        boolean fornecedorAtivo = false;

        if (gestorLocalizacao.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gestorLocalizacao.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 1f,
                    ouvinteLocalizacao, Looper.getMainLooper());
            fornecedorAtivo = true;
        }

        if (gestorLocalizacao.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            gestorLocalizacao.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 1000, 1f,
                    ouvinteLocalizacao, Looper.getMainLooper());
            fornecedorAtivo = true;
        }

        atualizacoesLocalizacaoAtivas = fornecedorAtivo;

        Location ultimaPosicao = gestorLocalizacao.getLastKnownLocation(
                LocationManager.GPS_PROVIDER);
        if (ultimaPosicao == null) {
            ultimaPosicao = gestorLocalizacao.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER);
        }
        if (ultimaPosicao != null) {
            atualizarPontoLocalizacao(ultimaPosicao);
        }
    }

    private void atualizarPontoLocalizacao(Location localizacao) {
        if (mapa == null) {
            return;
        }

        LatLng posicao = new LatLng(
                localizacao.getLatitude(), localizacao.getLongitude());

        if (pontoLocalizacao == null) {
            pontoLocalizacao = mapa.addCircle(new CircleOptions()
                    .center(posicao)
                    .radius(3d)
                    .strokeColor(Color.rgb(25, 118, 210))
                    .strokeWidth(3f)
                    .fillColor(Color.argb(110, 33, 150, 243))
                    .zIndex(10f));
        } else {
            pontoLocalizacao.setCenter(posicao);
        }
    }

    @Override
    protected void onPause() {
        // Saves the moment when the student leaves the app.
        SessionManager.registarAtividade(this);
        super.onPause();
        pararAtualizacoesLocalizacao();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // The timeout is also checked when the app returns from the background.
        if (ParseUser.getCurrentUser() == null
                || !SessionManager.sessaoValida(this)) {
            terminarSessao();
            return;
        }

        SessionManager.registarAtividade(this);
        ativarLocalizacao();
    }

    private void pararAtualizacoesLocalizacao() {
        if (gestorLocalizacao != null && atualizacoesLocalizacaoAtivas) {
            gestorLocalizacao.removeUpdates(ouvinteLocalizacao);
            atualizacoesLocalizacaoAtivas = false;
        }
    }

    private void mostrarPagina(boolean mostrarMapa, View paginaInicio, View paginaMapa) {
        paginaInicio.setVisibility(mostrarMapa ? View.GONE : View.VISIBLE);
        paginaMapa.setVisibility(mostrarMapa ? View.VISIBLE : View.GONE);
        SessionManager.guardarPaginaMapa(this, mostrarMapa);
    }
}
