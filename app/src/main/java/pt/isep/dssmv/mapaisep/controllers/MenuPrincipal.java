package pt.isep.dssmv.mapaisep.controllers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import pt.isep.dssmv.mapaisep.R;

public class MenuPrincipal extends AppCompatActivity {
    private static final String MAP_URL = "https://www.openstreetmap.org/";
    private static final String ISEP_MAP_HTML = ""
            + "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>"
            + "<style>html,body,#map{height:100%;margin:0}#status{position:absolute;z-index:1000;bottom:12px;left:12px;right:12px;"
            + "padding:10px;background:#fff;border-radius:8px;font:14px sans-serif;box-shadow:0 1px 5px #777}"
            + "#localizar{position:absolute;z-index:1000;top:12px;right:12px;padding:10px;background:#fff;border:0;"
            + "border-radius:6px;font:14px sans-serif;box-shadow:0 1px 5px #777}</style></head>"
            + "<body><div id='map'></div><button id='localizar'>A minha localização</button><div id='status'>A obter a localização…</div>"
            + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>"
            + "const isep=[41.178194,-8.607083];"
            + "const campus=[[41.1788413,-8.6091907],[41.1787122,-8.6091844],[41.1781764,-8.6091645],"
            + "[41.1775921,-8.6091508],[41.1774962,-8.6091237],[41.1774235,-8.60907],[41.1773791,-8.608952],"
            + "[41.1772868,-8.6086727],[41.177153,-8.6083029],[41.1771157,-8.6081637],[41.1770955,-8.6080457],"
            + "[41.1771197,-8.6079492],[41.1771682,-8.6078687],[41.1775591,-8.6074688],[41.1778014,-8.6072181],"
            + "[41.1778975,-8.6071315],[41.1782186,-8.6067965],[41.1782727,-8.606743],[41.1783187,-8.6066959],"
            + "[41.1787766,-8.6062276],[41.1788154,-8.6061922],[41.1788431,-8.6061662],[41.1788603,-8.6061505],"
            + "[41.1790416,-8.6059858],[41.1793091,-8.6057709],[41.1795948,-8.6056156],[41.1795477,-8.607945],"
            + "[41.1795443,-8.6081136],[41.1795441,-8.608126],[41.1795261,-8.6088933],[41.1791749,-8.6088826],"
            + "[41.1788569,-8.6087911]];"
            + "const areaIsep=L.latLngBounds(campus);const campusShape=L.polygon(campus);"
            + "window.map=L.map('map',{maxBounds:areaIsep,maxBoundsViscosity:1,minZoom:18,maxZoom:20})"
            + ".setView([41.17827,-8.60727],19);"
            + "L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxNativeZoom:19,maxZoom:20,"
            + "attribution:'&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap contributors</a>'}).addTo(map);"
            + "map.createPane('mascaraISEP');map.getPane('mascaraISEP').style.zIndex=650;"
            + "const exterior=[[85,-180],[85,180],[-85,180],[-85,-180]];"
            + "L.polygon([exterior,campus],{pane:'mascaraISEP',stroke:false,fillColor:'#fff',fillOpacity:1,fillRule:'evenodd',interactive:false}).addTo(map);"
            + "L.marker(isep).addTo(map).bindPopup('ISEP').openPopup();window.userPosition=null;"
            + "document.getElementById('localizar').onclick=()=>{if(window.userPosition&&campusShape.getBounds().contains(window.userPosition))"
            + "{map.setView(window.userPosition,19)}else{document.getElementById('status').textContent='A localização está fora da área do mapa do ISEP.'}};"
            + "if(navigator.geolocation){navigator.geolocation.watchPosition(p=>{const pos=[p.coords.latitude,p.coords.longitude];"
            + "window.userPosition=pos;"
            + "if(campusShape.getBounds().contains(pos)){if(window.userMarker){window.userMarker.setLatLng(pos)}else{window.userMarker=L.circleMarker(pos,{radius:9,color:'#1769aa',"
            + "fillColor:'#2196f3',fillOpacity:1}).addTo(map).bindPopup('A minha localização')}"
            + "document.getElementById('status').textContent='Mapa do ISEP — localização obtida';}else{if(window.userMarker){map.removeLayer(window.userMarker);window.userMarker=null}"
            + "document.getElementById('status').textContent='Estás fora da área visível do ISEP.'}},e=>{"
            + "document.getElementById('status').textContent='Não foi possível obter a localização. Verifica a permissão e o GPS.';},"
            + "{enableHighAccuracy:true,maximumAge:5000,timeout:15000})}else{document.getElementById('status').textContent='Localização indisponível'}"
            + "</script></body></html>";

    private WebView mapaWebView;
    private boolean mapaCarregado;
    private ActivityResultLauncher<String[]> permissaoLocalizacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_principal);
        permissaoLocalizacao = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), resultados -> carregarMapa());

        View paginaInicio = findViewById(R.id.paginaInicio);
        View paginaMapa = findViewById(R.id.paginaMapa);
        mapaWebView = findViewById(R.id.mapaWebView);
        configurarMapa();

        findViewById(R.id.btnInicio).setOnClickListener(v -> {
            paginaInicio.setVisibility(View.VISIBLE);
            paginaMapa.setVisibility(View.GONE);
        });
        findViewById(R.id.btnMapa).setOnClickListener(v -> {
            paginaInicio.setVisibility(View.GONE);
            paginaMapa.setVisibility(View.VISIBLE);
            if (temPermissaoLocalizacao()) {
                carregarMapa();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissaoLocalizacao.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION});
            } else {
                carregarMapa();
            }
        });
    }

    private void configurarMapa() {
        mapaWebView.getSettings().setJavaScriptEnabled(true);
        mapaWebView.getSettings().setDomStorageEnabled(true);
        mapaWebView.getSettings().setGeolocationEnabled(true);
        mapaWebView.getSettings().setUserAgentString(
                mapaWebView.getSettings().getUserAgentString() + " MapaISEP/1.0");
        mapaWebView.setWebViewClient(new WebViewClient());
        mapaWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, temPermissaoLocalizacao(), false);
            }
        });
    }

    private boolean temPermissaoLocalizacao() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void carregarMapa() {
        if (!mapaCarregado) {
            mapaWebView.loadDataWithBaseURL(MAP_URL, ISEP_MAP_HTML, "text/html", "UTF-8", null);
            mapaCarregado = true;
        } else if (temPermissaoLocalizacao()) {
            mapaWebView.reload();
        }
    }

    @Override
    protected void onDestroy() {
        mapaWebView.destroy();
        super.onDestroy();
    }
}
