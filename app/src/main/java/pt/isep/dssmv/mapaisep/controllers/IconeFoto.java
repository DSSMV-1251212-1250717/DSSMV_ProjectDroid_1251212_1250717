package pt.isep.dssmv.mapaisep.controllers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Base64;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

// Transforma a foto de perfil (texto Base64) num ícone redondo para um marcador do Google Maps.
// Usado pela LocalizacaoUtilizador (a tua posição) e pela AmigosNoMapa (os amigos).
public final class IconeFoto {

    private IconeFoto() {
        // Só tem um método estático.
    }

    // Sem foto (ou foto inválida): devolve o marcador normal do Google Maps, azul-claro.
    public static BitmapDescriptor criar(String fotoBase64, int corContorno) {
        if (fotoBase64 == null) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
        }
        try {
            // Texto Base64 -> bytes -> imagem, reduzida para 96x96.
            byte[] bytes = Base64.decode(fotoBase64, Base64.DEFAULT);
            Bitmap original = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (original == null) {
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
            }
            Bitmap imagem = Bitmap.createScaledBitmap(original, 96, 96, true);

            // Desenha um círculo da cor do contorno e, por cima, a foto cortada em círculo.
            Bitmap resultado = Bitmap.createBitmap(104, 104, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(resultado);
            Paint pintura = new Paint(Paint.ANTI_ALIAS_FLAG);
            pintura.setColor(corContorno);
            canvas.drawCircle(52, 52, 52, pintura);
            canvas.save();
            Path circulo = new Path();
            circulo.addCircle(52, 52, 46, Path.Direction.CW);
            canvas.clipPath(circulo);
            canvas.drawBitmap(imagem, null, new RectF(4, 4, 100, 100), pintura);
            canvas.restore();

            return BitmapDescriptorFactory.fromBitmap(resultado);
        } catch (IllegalArgumentException e) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
        }
    }
}
