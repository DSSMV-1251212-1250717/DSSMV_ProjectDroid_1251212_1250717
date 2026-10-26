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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import pt.isep.dssmv.mapaisep.services.AuthService;
import pt.isep.dssmv.mapaisep.services.LocalizacaoService;
import pt.isep.dssmv.mapaisep.services.PerfilService;

// A localização do utilizador: a tua foto no mapa, na posição do GPS, e o envio da posição para o Firestore.
// O MenuPrincipal cria-a no onCreate, entrega-lhe o mapa e chama ativar() / parar().
public class LocalizacaoUtilizador {
    private final Context context;
    private final LocalizacaoService localizacaoService = new LocalizacaoService();
    private final LocationManager gestorLocalizacao;
    private final LocationListener ouvinteLocalizacao;

    private GoogleMap mapa;
    // A tua posição é desenhada com a tua foto de perfil (num círculo azul), e não com o ponto azul.
    private Marker marcadorMinhaLocalizacao;
    private Location ultimaPosicaoMinha;
    private BitmapDescriptor iconeMinhaFoto;
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
        carregarIconeMinhaFoto();
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
            // A posição é desenhada com a foto do perfil, não com o ponto azul nativo.
            mapa.setMyLocationEnabled(false);
            mapa.getUiSettings().setMyLocationButtonEnabled(false);

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

        ultimaPosicaoMinha = new Location(localizacao);
        desenharMinhaLocalizacao();

        // A mesma posição que move a foto é partilhada com os amigos.
        if (mapaAberto && deveEnviarLocalizacao(localizacao)) {
            localizacaoService.atualizarMinhaLocalizacao(
                    context, localizacao.getLatitude(), localizacao.getLongitude()
            );
            ultimaLocalizacaoPartilhada = new Location(localizacao);
            ultimoEnvioLocalizacao = System.currentTimeMillis();
        }
    }

    // Pede a tua foto ao Firestore e transforma-a no ícone do marcador (contorno azul).
    private void carregarIconeMinhaFoto() {
        new PerfilService().obterFoto(context, AuthService.getCurrentUid(), fotoBase64 -> {
            iconeMinhaFoto = IconeFoto.criar(fotoBase64, Color.rgb(25, 118, 210));
            desenharMinhaLocalizacao();
        });
    }

    // Cria o marcador com a tua foto ou move-o para a última posição conhecida.
    private void desenharMinhaLocalizacao() {
        if (mapa == null || ultimaPosicaoMinha == null || iconeMinhaFoto == null) {
            return;
        }
        LatLng posicao = new LatLng(ultimaPosicaoMinha.getLatitude(), ultimaPosicaoMinha.getLongitude());
        if (marcadorMinhaLocalizacao == null) {
            marcadorMinhaLocalizacao = mapa.addMarker(new MarkerOptions()
                    .position(posicao).title("A tua localização").icon(iconeMinhaFoto));
        } else {
            marcadorMinhaLocalizacao.setPosition(posicao);
            marcadorMinhaLocalizacao.setIcon(iconeMinhaFoto);
        }
    }

    // Evita enviar um PATCH por cada pequena variação do GPS.
    private boolean deveEnviarLocalizacao(Location localizacao) {
        return ultimaLocalizacaoPartilhada == null
                || ultimaLocalizacaoPartilhada.distanceTo(localizacao) >= 3f
                || System.currentTimeMillis() - ultimoEnvioLocalizacao >= 5000;
    }
}
