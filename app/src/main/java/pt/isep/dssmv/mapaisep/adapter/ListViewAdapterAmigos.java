package pt.isep.dssmv.mapaisep.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import pt.isep.dssmv.mapaisep.R;
import pt.isep.dssmv.mapaisep.model.Amigo;

// Ponte entre a lista de amigos (List<Amigo>) e a ListView do ecrã Amigos.
// Para cada posição, pega no molde item_amigo.xml e põe lá a foto, o nome e o número.
// Não chama serviços: quando se carrega num botão, avisa a Activity (AcoesAmigo),
// que é quem remove o amigo ou abre o horário. Como o ListViewAdapterInstructor do professor.
public class ListViewAdapterAmigos extends BaseAdapter {

    // O que a Activity faz quando se carrega nos botões de um cartão.
    public interface AcoesAmigo {
        void remover(Amigo amigo);

        void verHorario(Amigo amigo);
    }

    private final Context context;
    private final List<Amigo> amigos;
    // Fotos já carregadas, por uid. A Activity vai preenchendo à medida que chegam.
    private final Map<String, Bitmap> fotos;
    private final AcoesAmigo acoes;

    public ListViewAdapterAmigos(Context context, List<Amigo> amigos, Map<String, Bitmap> fotos, AcoesAmigo acoes) {
        this.context = context;
        this.amigos = amigos;
        this.fotos = fotos;
        this.acoes = acoes;
    }

    // Quantas linhas tem a lista.
    @Override
    public int getCount() {
        return amigos.size();
    }

    @Override
    public Object getItem(int position) {
        return amigos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // Cria (ou reaproveita) o cartão da posição pedida e preenche-o com os dados desse amigo.
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View cartao = convertView;
        // A ListView reaproveita cartões que saíram do ecrã (convertView); só cria um novo se não houver.
        if (cartao == null) {
            cartao = LayoutInflater.from(context).inflate(R.layout.item_amigo, parent, false);
        }

        Amigo amigo = amigos.get(position);

        TextView nome = cartao.findViewById(R.id.nomeAmigo);
        TextView numero = cartao.findViewById(R.id.numeroAmigo);
        ImageView foto = cartao.findViewById(R.id.fotoAmigo);

        nome.setText(amigo.getNome());
        numero.setText(amigo.getNumero());

        // Foto do amigo, se já chegou; senão, o ícone por defeito.
        Bitmap imagem = fotos.get(amigo.getUid());
        if (imagem != null) {
            foto.setImageBitmap(imagem);
        } else {
            foto.setImageResource(R.drawable.ic_campus_pin);
        }

        cartao.findViewById(R.id.btnRemoverAmigo).setOnClickListener(v -> acoes.remover(amigo));
        cartao.findViewById(R.id.btnVerHorarioAmigo).setOnClickListener(v -> acoes.verHorario(amigo));

        return cartao;
    }
}
