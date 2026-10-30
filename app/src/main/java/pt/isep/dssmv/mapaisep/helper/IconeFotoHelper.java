package pt.isep.dssmv.mapaisep.helper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Base64;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

// Transforma a foto de perfil (texto Base64) num ícone redondo para um marcador do Google Maps.
// Usado pela LocalizacaoAluno (posição do aluno) e pela AmigosNoMapa (os amigos).
public final class IconeFotoHelper {

    // Lado do ícone redondo, em píxeis.
    private static final int LADO = 104;

    private IconeFotoHelper() {
        // Só tem métodos estáticos.
    }

    // Foto a cores e sem legenda (é o caso normal).
    public static BitmapDescriptor criar(String fotoBase64, int corContorno) {
        return criar(fotoBase64, corContorno, false, null);
    }

    // cinzento = true: a foto é desenhada só em tons de cinzento (posição antiga de um amigo).
    // legenda: texto por cima do ícone (por exemplo "visto há 7 min"), ou null para não ter.
    // Sem foto e sem legenda: devolve o marcador normal do Google Maps, azul-claro.
    public static BitmapDescriptor criar(String fotoBase64, int corContorno, boolean cinzento, String legenda) {
        Bitmap circulo = desenharFoto(fotoBase64, corContorno, cinzento);

        if (circulo == null) {
            if (legenda == null) {
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
            }
            // Sem foto mas com legenda: um círculo só com a cor do contorno, para a legenda ter onde ficar.
            circulo = Bitmap.createBitmap(LADO, LADO, Bitmap.Config.ARGB_8888);
            Paint pintura = new Paint(Paint.ANTI_ALIAS_FLAG);
            pintura.setColor(corContorno);
            new Canvas(circulo).drawCircle(LADO / 2f, LADO / 2f, LADO / 2f, pintura);
        }

        if (legenda == null) {
            return BitmapDescriptorFactory.fromBitmap(circulo);
        }
        return BitmapDescriptorFactory.fromBitmap(juntarLegenda(circulo, legenda));
    }

    // Desenha um círculo da cor do contorno e, por cima, a foto cortada em círculo.
    // Devolve null se não houver foto ou se a foto for inválida.
    private static Bitmap desenharFoto(String fotoBase64, int corContorno, boolean cinzento) {
        if (fotoBase64 == null) {
            return null;
        }
        try {
            // Texto Base64 -> bytes -> imagem, reduzida para 96x96.
            byte[] bytes = Base64.decode(fotoBase64, Base64.DEFAULT);
            Bitmap original = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (original == null) {
                return null;
            }
            Bitmap imagem = Bitmap.createScaledBitmap(original, 96, 96, true);

            Bitmap resultado = Bitmap.createBitmap(LADO, LADO, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(resultado);
            Paint pintura = new Paint(Paint.ANTI_ALIAS_FLAG);
            pintura.setColor(corContorno);
            canvas.drawCircle(52, 52, 52, pintura);
            if (cinzento) {
                // Saturação 0 = sem cor: cada píxel fica só com o seu tom de cinzento.
                ColorMatrix matriz = new ColorMatrix();
                matriz.setSaturation(0);
                pintura.setColorFilter(new ColorMatrixColorFilter(matriz));
            }
            canvas.save();
            Path circulo = new Path();
            circulo.addCircle(52, 52, 46, Path.Direction.CW);
            canvas.clipPath(circulo);
            canvas.drawBitmap(imagem, null, new RectF(4, 4, 100, 100), pintura);
            canvas.restore();
            return resultado;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Põe a legenda numa caixa branca por cima do ícone redondo.
    // O marcador fica preso ao mapa pelo fundo do ícone, por isso a legenda não muda o sítio da posição.
    private static Bitmap juntarLegenda(Bitmap circulo, String legenda) {
        Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        texto.setColor(Color.DKGRAY);
        texto.setTextSize(28);
        texto.setTextAlign(Paint.Align.CENTER);

        int larguraTexto = (int) Math.ceil(texto.measureText(legenda));
        int largura = Math.max(LADO, larguraTexto + 24);
        int alturaCaixa = 44;
        int espaco = 6;

        Bitmap resultado = Bitmap.createBitmap(largura, alturaCaixa + espaco + LADO, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultado);

        // Caixa branca com cantos redondos e o texto ao centro.
        Paint caixa = new Paint(Paint.ANTI_ALIAS_FLAG);
        caixa.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(0, 0, largura, alturaCaixa), 14, 14, caixa);
        canvas.drawText(legenda, largura / 2f, alturaCaixa / 2f + 10, texto);

        // Ícone redondo centrado por baixo da caixa.
        canvas.drawBitmap(circulo, (largura - LADO) / 2f, alturaCaixa + espaco, null);
        return resultado;
    }
}
