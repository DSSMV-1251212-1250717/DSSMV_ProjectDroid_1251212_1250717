package pt.isep.dssmv.mapaisep.controllers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import pt.isep.dssmv.mapaisep.services.LocalizacaoService;

// A localização do utilizador: o ponto azul no mapa (GPS) e o envio da posição para o Firestore.
// O MenuPrincipal cria-a no onCreate, entrega-lhe o mapa e chama ativar() / parar().
public class LocalizacaoUtilizador {
    private final Context context;
    private final LocalizacaoService localizacaoService = new LocalizacaoService();
    private final LocationManager gestorLocalizacao;
    private final LocationListener ouvinteLocalizacao;

    private GoogleMap mapa;
    private Circle pontoLocalizacao;
    private boolean atualizacoesLocalizacaoAtivas;

    // Só se partilha a posição com os amigos enquanto o mapa está aberto.
    private boolean mapaAberto;
    private Location ultimaLocalizacaoPartilhada;
    private long ultimoEnvioLocalizacao;

    public LocalizacaoUtilizador(Context context) {
        this.context = context;
        gestorLocalizacao = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Chamado pelo Android sempre que o GPS tem uma posição nova.
        ouvinteLocalizacao = new LocationListener() {
            @Override
            public void onLocationChanged(Location localizacao) {
                atualizarPontoLocalizacao(localizacao);
            }
        };
    }

    // Chamado no onMapReady do MenuPrincipal, quando o Google Maps está pronto.
    public void definirMapa(GoogleMap mapa) {
        this.mapa = mapa;
    }

    // O MenuPrincipal avisa quando troca entre Início e Mapa.
    public void definirMapaAberto(boolean mapaAberto) {
        this.mapaAberto = mapaAberto;
    }

    // Tem localização precisa (FINE)? Só esta deixa ver as redes Wi-Fi.
    public static boolean temLocalizacaoPrecisa(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Tem alguma permissão de localização (precisa ou aproximada)?
    public static boolean temPermissaoLocalizacao(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Mostra a localização no Google Maps e começa a receber posições do GPS.
    @SuppressWarnings("MissingPermission")
    public void ativar() {
        if (mapa != null && temPermissaoLocalizacao(context)) {
            mapa.setMyLocationEnabled(true);

            mapa.getUiSettings()
                    .setMyLocationButtonEnabled(true);

            iniciarAtualizacoesLocalizacao();
        }
    }

    // Deixa de receber posições do GPS (app em fundo).
    public void parar() {
        if (gestorLocalizacao != null && atualizacoesLocalizacaoAtivas) {
            gestorLocalizacao.removeUpdates(ouvinteLocalizacao);
            atualizacoesLocalizacaoAtivas = false;
        }
    }

    @SuppressWarnings("MissingPermission")
    private void iniciarAtualizacoesLocalizacao() {

        if (gestorLocalizacao == null
                || atualizacoesLocalizacaoAtivas
                || !temPermissaoLocalizacao(context)) {
            return;
        }

        boolean fornecedorAtivo = false;

        if (gestorLocalizacao.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gestorLocalizacao.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    1f,
                    ouvinteLocalizacao,
                    Looper.getMainLooper()
            );
            fornecedorAtivo = true;
        }

        if (gestorLocalizacao.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            gestorLocalizacao.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000,
                    1f,
                    ouvinteLocalizacao,
                    Looper.getMainLooper()
            );
            fornecedorAtivo = true;
        }

        atualizacoesLocalizacaoAtivas = fornecedorAtivo;

        Location ultimaPosicao = gestorLocalizacao.getLastKnownLocation(
                LocationManager.GPS_PROVIDER
        );

        if (ultimaPosicao == null) {
            ultimaPosicao = gestorLocalizacao.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER
            );
        }

        if (ultimaPosicao != null) {
            atualizarPontoLocalizacao(ultimaPosicao);
        }
    }

    // Move o ponto azul e, se o mapa estiver aberto, envia a posição para o Firestore.
    private void atualizarPontoLocalizacao(Location localizacao) {

        if (mapa == null) {
            return;
        }

        LatLng posicao = new LatLng(
                localizacao.getLatitude(),
                localizacao.getLongitude()
        );

        if (pontoLocalizacao == null) {
            pontoLocalizacao = mapa.addCircle(
                    new CircleOptions()
                            .center(posicao)
                            .radius(3d)
                            .strokeColor(Color.rgb(25, 118, 210))
                            .strokeWidth(3f)
                            .fillColor(Color.argb(110, 33, 150, 243))
                            .zIndex(10f)
            );
        } else {
            pontoLocalizacao.setCenter(posicao);
        }

        // O mesmo GPS que move o ponto azul atualiza a posição partilhada.
        if (mapaAberto && deveEnviarLocalizacao(localizacao)) {
            localizacaoService.atualizarMinhaLocalizacao(
                    context, localizacao.getLatitude(), localizacao.getLongitude()
            );
            ultimaLocalizacaoPartilhada = new Location(localizacao);
            ultimoEnvioLocalizacao = System.currentTimeMillis();
        }
    }

    // Evita enviar um PATCH por cada pequena variação do GPS.
    private boolean deveEnviarLocalizacao(Location localizacao) {
        return ultimaLocalizacaoPartilhada == null
                || ultimaLocalizacaoPartilhada.distanceTo(localizacao) >= 3f
                || System.currentTimeMillis() - ultimoEnvioLocalizacao >= 5000;
    }
}
