package pt.isep.dssmv.mapaisep.mappers;

import org.json.JSONException;
import org.json.JSONObject;

import pt.isep.dssmv.mapaisep.dto.LocalizacaoDTO;
import pt.isep.dssmv.mapaisep.model.Amigo;
import pt.isep.dssmv.mapaisep.model.LocalizacaoAmigo;

/** Converte JSON Firestore em DTO e DTO em Model. */
public final class LocalizacaoMapper {
    public LocalizacaoDTO desserializar(JSONObject documento, Amigo amigo) throws JSONException {
        JSONObject campos = documento.getJSONObject("fields");
        return new LocalizacaoDTO(
                amigo.getUid(), amigo.getNome(),
                lerNumero(campos.getJSONObject("latitude")),
                lerNumero(campos.getJSONObject("longitude"))
        );
    }

    public LocalizacaoAmigo converterParaModelo(LocalizacaoDTO dto) {
        return new LocalizacaoAmigo(dto.uid, dto.nome, dto.latitude, dto.longitude);
    }

    private double lerNumero(JSONObject campo) throws JSONException {
        if (campo.has("doubleValue")) {
            return campo.getDouble("doubleValue");
        }
        return Double.parseDouble(campo.getString("integerValue"));
    }
}
