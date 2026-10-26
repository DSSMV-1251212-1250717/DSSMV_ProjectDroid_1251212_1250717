package pt.isep.dssmv.mapaisep.mappers;

import org.json.JSONException;
import org.json.JSONObject;

import pt.isep.dssmv.mapaisep.dto.AlunoDTO;
import pt.isep.dssmv.mapaisep.dto.AmigoDTO;
import pt.isep.dssmv.mapaisep.model.Amigo;

/** Cria os objetos Amigo: a partir da lista de amigos (AmigoDTO) ou do documento do aluno (AlunoDTO). */
public class AmigoMapper {

    public AmigoDTO desserializarAmigo(JSONObject documento) throws JSONException {
        JSONObject campos = documento.getJSONObject("fields");
        String caminho = documento.getString("name");

        AmigoDTO dto = new AmigoDTO();
        dto.setUid(caminho.substring(caminho.lastIndexOf('/') + 1));
        dto.setNome(campos.getJSONObject("nome").getString("stringValue"));
        dto.setNumero(campos.getJSONObject("numero").getString("stringValue"));
        return dto;
    }

    // Da lista de amigos (alunos/{teu uid}/amigos/{uid}).
    public Amigo converterParaModelo(AmigoDTO dto) {
        return new Amigo(dto.getUid(), dto.getNome(), dto.getNumero());
    }

    // Do documento principal do aluno (alunos/{uid}): usado ao adicionar um amigo.
    public Amigo converterParaModelo(AlunoDTO dto) throws JSONException {
        if (dto.nome == null || dto.numero == null) {
            throw new JSONException("Aluno sem nome ou número");
        }
        return new Amigo(dto.uid, dto.nome, dto.numero);
    }
}
