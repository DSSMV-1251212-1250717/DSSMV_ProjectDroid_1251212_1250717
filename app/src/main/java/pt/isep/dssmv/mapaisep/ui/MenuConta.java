package pt.isep.dssmv.mapaisep.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.service.AuthService;
import pt.isep.dssmv.mapaisep.helper.SessaoHelper;
import pt.isep.dssmv.mapaisep.LoginActivity;

// Menu da conta (botão ≡) dos ecrãs Perfil, Amigos e Horário do amigo.
// Tem também o sessaoExpirada, usado por todos os ecrãs quando a sessão acaba.
// Estava copiado nos três ecrãs; agora está só aqui.
// O MenuPrincipalActivity tem o seu próprio menu, porque lá o "Início" só troca de página.
public class MenuConta {

    // Mostra o menu por baixo do botão (ancora) e trata a opção escolhida.
    public static void abrir(Activity activity, View ancora) {
        PopupMenu menu = new PopupMenu(activity, ancora);
        menu.getMenuInflater().inflate(R.menu.menu_account, menu.getMenu());

        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_home) {
                irPara(activity, MenuPrincipalActivity.class);
                return true;
            }

            if (item.getItemId() == R.id.action_profile) {
                irPara(activity, PerfilActivity.class);
                return true;
            }

            if (item.getItemId() == R.id.action_friends) {
                irPara(activity, AmigosActivity.class);
                return true;
            }

            if (item.getItemId() == R.id.action_logout) {
                terminarSessao(activity);
                return true;
            }

            return false;
        });

        menu.show();
    }

    // Abre o ecrã escolhido. Se já estás nele, não faz nada.
    // CLEAR_TOP: se o ecrã já está aberto por baixo, volta a ele (fecha os de cima)
    // em vez de abrir outro igual. SINGLE_TOP: reaproveita-o, não o cria de novo.
    private static void irPara(Activity activity, Class<?> ecra) {
        if (activity.getClass() == ecra) {
            return;
        }

        Intent intent = new Intent(activity, ecra);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    // Chamado por qualquer ecrã que receba o código SessaoExpiradaException.CODIGO:
    // avisa o aluno e volta ao login. Fica num só sítio para não ser copiado em cada ecrã.
    public static void sessaoExpirada(Activity activity) {
        Toast.makeText(activity, R.string.session_expired, Toast.LENGTH_LONG).show();
        terminarSessao(activity);
    }

    // Apaga a sessão e volta ao login, sem deixar ecrãs para trás.
    private static void terminarSessao(Activity activity) {
        AuthService.logout();
        SessaoHelper.terminarSessao(activity);

        Intent irParaLogin = new Intent(activity, LoginActivity.class);
        irParaLogin.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(irParaLogin);
        activity.finish();
    }
}
