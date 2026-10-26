package pt.isep.dssmv.mapaisep.mappers;

import org.json.JSONException;
import org.json.JSONObject;

import pt.isep.dssmv.mapaisep.dto.AlunoDTO;

/**
 * Lê o documento principal de um aluno (alunos/{uid}).
 * Só faz JSON -> AlunoDTO; cada sítio usa o DTO diretamente:
 * o horário (turma), o perfil (número, turma e foto) e o AmigosService,
 * que entrega o DTO ao AmigoMapper para criar o Amigo.
 */
public final class AlunoMapper {

    // JSON -> AlunoDTO. O uid vem do caminho do documento (.../alunos/{uid}).
    public AlunoDTO desserializar(JSONObject documento) throws JSONException {
        JSONObject campos = documento.getJSONObject("fields");
        String caminho = documento.getString("name");

        return new AlunoDTO(
                caminho.substring(caminho.lastIndexOf('/') + 1),
                lerTexto(campos, "nome"),
                lerTexto(campos, "numero"),
                lerTexto(campos, "turmaId"),
                lerTexto(campos, "fotoBase64")
        );
    }

    // Valor de um campo de texto; aceita também números (integerValue). null se não existir.
    private String lerTexto(JSONObject campos, String nome) {
        JSONObject campo = campos.optJSONObject(nome);
        if (campo == null) {
            return null;
        }
        if (campo.has("stringValue")) {
            return campo.optString("stringValue");
        }
        if (campo.has("integerValue")) {
            return campo.optString("integerValue");
        }
        return null;
    }
}
