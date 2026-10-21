package pt.isep.dssmv.mapaisep.mappers;

import org.json.JSONException;
import org.json.JSONObject;

import pt.isep.dssmv.mapaisep.dto.AmigoDTO;
import pt.isep.dssmv.mapaisep.model.Amigo;

/** Converte um documento Firestore em DTO e depois em modelo Amigo. */
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

    public Amigo converterParaModelo(AmigoDTO dto) {
        return new Amigo(dto.getUid(), dto.getNome(), dto.getNumero());
    }
}
