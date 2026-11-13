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
    //LocationManager: é o serviço de localização do Android
    //que fala com o GPS.
    private final LocationListener ouvinteLocalizacao;
    //LocationListener: é uma interface do Android.
    // Somos nós que a implementamos (linha 54), a dizer o que fazer quando chega uma posição nova:

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
                //Sempre que o GPS tem uma posição nova,
                // o Android chama este método e passa-nos a posição no parâmetro localizacao
                atualizarPontoLocalizacao(localizacao);
                //Quando o LocalizacaoAluno é criado, preparamos um LocationListener
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
            //estes tres metodos sao obrigatorios
            //quando o gps é ligado por exemplo
            //nao precisamos deles;
        };
    }

    // Chamado no onMapReady do MenuPrincipalActivity, quando o Google Maps está pronto.
    public void definirMapa(GoogleMap mapa) {
        this.mapa = mapa;
        carregarIconeMinhaFoto();
        //CARREGA A NOSSA FOTO
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
            // Desliga o ponto azul que o Google Maps desenha sozinho para mostrar a posição:
            // a posição é desenhada com a foto do perfil.
            mapa.setMyLocationEnabled(false);
            // Esconde o botão "centrar em mim", que só existe junto com esse ponto azul.
            mapa.getUiSettings().setMyLocationButtonEnabled(false);

            iniciarAtualizacoesLocalizacao();
            //Desliga o ponto azul da Google, porque usamos a nossa foto, e pede posições ao GPS.
            //pede a loc
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
                    //O gestorLocalizacao é o LocationManager, o serviço de
                    // localização do Android. Dizemos-lhe: “dá-me posições do GPS e entrega-as ao ouvinteLocalizacao”.
                    LocationManager.GPS_PROVIDER,
                    1000,
                    1f,
                    ouvinteLocalizacao,
                    //ENTREGA A LOCALIZACAO AO OUVINTE
                    Looper.getMainLooper()
                    //Dá-me uma posição no máximo a cada 1 s, se me mexi pelo menos 1 m,
                    // e entrega-a ao ouvinteLocalizacao na main thread.”
                    //para isso preciso do looper
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
        //recebemos a localizacao do gps do android
        //onLocationChanged
        //temos de atualizar no mapa para a nossa visualizacao e temos
        //de meter na firebase para os outros;

        if (mapa == null) {
            return;
        }

        ultimaPosicaoMinha = new Location(localizacao);
        //ultima posicao do gps serve para desenhar o pin
        //guarda uma cópia da posição nova (é usada também quando a foto chegar)
        desenharMinhaLocalizacao();
        //porque ja temos a loc que veio do gps;
        //põe o pin com a nossa foto nesta posição, ou move-o se já existir

        // A mesma posição que move a foto é partilhada com os amigos.
        if (mapaAberto && deveEnviarLocalizacao(localizacao)) {
            localizacaoService.atualizarMinhaLocalizacao(
                    //temos de mandar para a firebase
                    //Isto chama o LocalizacaoService.java, linha 38,
                    // que faz o PATCH em localizacoes/{uid} (o passo 8 que vimos).
                    context, localizacao.getLatitude(), localizacao.getLongitude()
            );
            ultimaLocalizacaoPartilhada = new Location(localizacao);
            //ultima loc partilha, serve para vermos se vale a pena enviar outra
            //deveEnviarLoc trata disso
            ultimoEnvioLocalizacao = System.currentTimeMillis();
        }
    }

    // Pede a foto do aluno ao Firestore e transforma-a no ícone do marcador (contorno azul).
    private void carregarIconeMinhaFoto() {
        //Pede a nossa foto (Base64) à Firebase
        // uid: quem é o aluno (o identificador dele no Firebase, usado nos endereços dos documentos).
        new PerfilService().obterFoto(context, AuthService.getCurrentUid(), fotoBase64 -> {
            iconeMinhaFoto = IconeFotoHelper.criar(fotoBase64, Color.rgb(25, 118, 210));
            //transforma a foto num ícone redondo com contorno azul
            desenharMinhaLocalizacao();
            //se a posição já chegou, desenha o pin agora
        });
    }

    // Cria o marcador com a foto do aluno ou move-o para a última posição conhecida.
    private void desenharMinhaLocalizacao() {
        if (mapa == null || ultimaPosicaoMinha == null || iconeMinhaFoto == null) {
            //só desenha quando já há mapa, posição e foto
            //se faltar um nao desenhamos
            return;
        }
        LatLng posicao = new LatLng(ultimaPosicaoMinha.getLatitude(), ultimaPosicaoMinha.getLongitude());
        //vem do atualizarPontoLoc
        if (marcadorMinhaLocalizacao == null) {
            //1.ª vez: cria o pin com a foto
            marcadorMinhaLocalizacao = mapa.addMarker(new MarkerOptions()
                    //chamamos o mapa porque foi entrege aqui pelo mapaOnReady
                    .position(posicao).title("A tua localização").icon(iconeMinhaFoto));
            //position: onde fica o pin; title: texto ao tocar; icon: troca o pin normal pela nossa foto (contorno azul)
        } else {
            //das outras vezes: só move o pin que já existe
            marcadorMinhaLocalizacao.setPosition(posicao);
            marcadorMinhaLocalizacao.setIcon(iconeMinhaFoto);
        }
    }

    // Evita enviar um PATCH por cada pequena variação do GPS.
    private boolean deveEnviarLocalizacao(Location localizacao) {
        return ultimaLocalizacaoPartilhada == null
                || ultimaLocalizacaoPartilhada.distanceTo(localizacao) >= 3f
                //mexemo-nos 3 metros ou mais desde a ultima loc
                || System.currentTimeMillis() - ultimoEnvioLocalizacao >= 5000;
                //se passaram 5 segundos
    }
}
