package pt.isep.dssmv.mapaisep.controllers;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.services.AuthService;
import pt.isep.dssmv.mapaisep.services.PerfilService;
import pt.isep.dssmv.mapaisep.services.SessionManager;

public class PerfilActivity extends AppCompatActivity {
    // Lado (em píxeis) da foto guardada no Firestore: pequena para caber no documento.
    private static final int TAMANHO_FOTO = 200;

    private ImageView imagemPerfil;
    private ActivityResultLauncher<String[]> seletorFoto;
    private final PerfilService perfilService = new PerfilService();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);
        imagemPerfil = findViewById(R.id.imagemPerfil);
        configurarSeletorFoto();
        ImageButton botaoMenu = findViewById(R.id.btnMenuPerfil);
        botaoMenu.setOnClickListener(this::abrirMenu);
        MaterialButton botaoFoto = findViewById(R.id.btnAlterarFoto);
        botaoFoto.setOnClickListener(v -> seletorFoto.launch(new String[]{"image/*"}));
        TextView numero = findViewById(R.id.textoNumeroPerfil);
        TextView turma = findViewById(R.id.textoTurmaPerfil);
        perfilService.obterPerfil(this, (numeroAluno, turmaAluno, fotoBase64, erro) -> {
            if (erro != null) {
                numero.setText(R.string.profile_error);
                turma.setText("");
                return;
            }
            numero.setText(numeroAluno);
            turma.setText(turmaAluno);

            // A foto vem do Firestore (campo fotoBase64), se o aluno já tiver escolhido uma.
            if (fotoBase64 != null) {
                mostrarFoto(fotoBase64);
            }
        });
    }

    private void configurarSeletorFoto() {
        seletorFoto = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) return;

            String base64;
            try {
                // 1. Imagem escolhida -> foto pequena em texto Base64.
                base64 = converterParaBase64(uri);
            } catch (IOException e) {
                Toast.makeText(this, R.string.profile_photo_read_error, Toast.LENGTH_SHORT).show();
                return;
            }
            if (base64 == null) {
                Toast.makeText(this, R.string.profile_photo_read_error, Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Mostrar já no ecrã.
            mostrarFoto(base64);

            // 3. Guardar no Firestore (PATCH ao documento alunos/{uid}).
            perfilService.guardarFoto(this, base64, erro -> {
                if (erro != null) {
                    Toast.makeText(this, R.string.profile_photo_save_error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Lê a imagem do telemóvel, corta-a em quadrado, reduz para 200x200,
    // comprime em JPEG e devolve os bytes escritos em texto Base64.
    // Devolve null se o ficheiro não for uma imagem válida.
    private String converterParaBase64(Uri uri) throws IOException {
        // a) Ler só as dimensões (sem carregar a imagem toda para a memória).
        BitmapFactory.Options dimensoes = new BitmapFactory.Options();
        dimensoes.inJustDecodeBounds = true;
        InputStream entrada = getContentResolver().openInputStream(uri);
        if (entrada == null) return null;
        BitmapFactory.decodeStream(entrada, null, dimensoes);
        entrada.close();

        // b) Escolher quanto reduzir ao ler (1, 2, 4, 8...), para uma foto de
        //    12 megapíxeis não ocupar dezenas de MB de memória.
        int reducao = 1;
        while (dimensoes.outWidth / (reducao * 2) >= TAMANHO_FOTO
                && dimensoes.outHeight / (reducao * 2) >= TAMANHO_FOTO) {
            reducao = reducao * 2;
        }

        // c) Ler a imagem já reduzida.
        BitmapFactory.Options leitura = new BitmapFactory.Options();
        leitura.inSampleSize = reducao;
        entrada = getContentResolver().openInputStream(uri);
        if (entrada == null) return null;
        Bitmap original = BitmapFactory.decodeStream(entrada, null, leitura);
        entrada.close();
        if (original == null) return null;

        // d) Cortar o quadrado do centro e reduzir para 200x200 (sem deformar).
        int lado = Math.min(original.getWidth(), original.getHeight());
        int x = (original.getWidth() - lado) / 2;
        int y = (original.getHeight() - lado) / 2;
        Bitmap quadrado = Bitmap.createBitmap(original, x, y, lado, lado);
        Bitmap pequena = Bitmap.createScaledBitmap(quadrado, TAMANHO_FOTO, TAMANHO_FOTO, true);

        // e) Comprimir em JPEG (qualidade 70) -> bytes.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        pequena.compress(Bitmap.CompressFormat.JPEG, 70, bytes);

        // f) Bytes -> texto Base64 (NO_WRAP: tudo numa linha, sem quebras).
        return Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP);
    }

    // Texto Base64 -> bytes -> imagem, e mostra-a no quadrado do perfil.
    private void mostrarFoto(String base64) {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        Bitmap imagem = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (imagem != null) {
            imagemPerfil.setImageBitmap(imagem);
            // Com foto, tira o círculo de fundo (bg_profile_photo) e o espaço à volta,
            // para a foto quadrada ocupar o quadrado todo.
            imagemPerfil.setBackground(null);
            imagemPerfil.setPadding(0, 0, 0, 0);
        }
    }

    private void abrirMenu(android.view.View ancora) {
        PopupMenu menu = new PopupMenu(this, ancora);
        menu.getMenuInflater().inflate(R.menu.menu_account, menu.getMenu());
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_home) {
                Intent inicio = new Intent(this, MenuPrincipal.class);
                inicio.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(inicio);
                finish();
                return true;
            }
            if (item.getItemId() == R.id.action_profile) {
                return true;
            }
            if (item.getItemId() == R.id.action_friends) {
                startActivity(new Intent(this, AmigosActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_logout) {
                AuthService.logout();
                SessionManager.terminarSessao(this);
                Intent login = new Intent(this, LoginActivity.class);
                login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(login);
                finish();
                return true;
            }
            return false;
        });
        menu.show();
    }
}
