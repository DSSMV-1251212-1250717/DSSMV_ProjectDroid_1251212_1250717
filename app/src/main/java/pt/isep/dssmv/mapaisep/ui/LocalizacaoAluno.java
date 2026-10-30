package pt.isep.dssmv.mapaisep.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import pt.isep.dssmv.mapaisep.service.AuthService;
import pt.isep.dssmv.mapaisep.service.LocalizacaoService;
import pt.isep.dssmv.mapaisep.service.PerfilService;
import pt.isep.dssmv.mapaisep.helper.IconeFotoHelper;

// A localização do aluno: a foto dele no mapa, na posição do GPS, e o envio da posição para o Firestore.
// O MenuPrincipalActivity cria-a no onCreate, entrega-lhe o mapa e chama ativar() / parar().
public class LocalizacaoAluno {
    // Uma "última posição conhecida" mais antiga do que isto já não é usada (pode ser de outro sítio).
    private static final long IDADE_MAXIMA_POSICAO_MS = 2 * 60_000L;

    private final Context context;
    private final LocalizacaoService localizacaoService = new LocalizacaoService();
    private final LocationManager gestorLocalizacao;
    private final LocationListener ouvinteLocalizacao;

    private GoogleMap mapa;
    // A posição do aluno é desenhada com a foto de perfil (num círculo azul), e não com o ponto azul.
    private Marker marcadorMinhaLocalizacao;
    private Location ultimaPosicaoMinha;
    private BitmapDescriptor iconeMinhaFoto;
    private boolean atualizacoesLocalizacaoAtivas;

    // Só se partilha a posição com os amigos enquanto o mapa está aberto.
    private boolean mapaAberto;
    private Location ultimaLocalizacaoPartilhada;
    private long ultimoEnvioLocalizacao;

    public LocalizacaoAluno(Context context) {
        this.context = context;
        gestorLocalizacao = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Chamado pelo Android quando há novidades do GPS.
        ouvinteLocalizacao = new LocationListener() {
            // Posição nova: move a foto do aluno no mapa e partilha a posição.
            @Override
            public void onLocationChanged(Location localizacao) {
                atualizarPontoLocalizacao(localizacao);
            }

            // O Android 8 a 10 chama estas três funções; sem elas, a app vai abaixo.
            // Não precisam de fazer nada.
            @Override
            public void onProviderEnabled(String fornecedor) {
            }

            @Override
            public void onProviderDisabled(String fornecedor) {
            }

            @Override
            public void onStatusChanged(String fornecedor, int estado, Bundle extras) {
            }
        };
    }

    // Chamado no onMapReady do MenuPrincipalActivity, quando o Google Maps está pronto.
    public void definirMapa(GoogleMap mapa) {
        this.mapa = mapa;
        carregarIconeMinhaFoto();
    }

    // O MenuPrincipalActivity avisa quando troca entre Início e Mapa.
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

        // Só usa a última posição conhecida se for recente; senão espera pela próxima do GPS.
        if (ultimaPosicao != null) {
            long idade = System.currentTimeMillis() - ultimaPosicao.getTime();
            if (idade <= IDADE_MAXIMA_POSICAO_MS) {
                atualizarPontoLocalizacao(ultimaPosicao);
            }
        }
    }

    // Move a foto do aluno e, se o mapa estiver aberto, envia a posição para o Firestore.
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

    // Pede a foto do aluno ao Firestore e transforma-a no ícone do marcador (contorno azul).
    private void carregarIconeMinhaFoto() {
        new PerfilService().obterFoto(context, AuthService.getCurrentUid(), fotoBase64 -> {
            iconeMinhaFoto = IconeFotoHelper.criar(fotoBase64, Color.rgb(25, 118, 210));
            desenharMinhaLocalizacao();
        });
    }

    // Cria o marcador com a foto do aluno ou move-o para a última posição conhecida.
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
