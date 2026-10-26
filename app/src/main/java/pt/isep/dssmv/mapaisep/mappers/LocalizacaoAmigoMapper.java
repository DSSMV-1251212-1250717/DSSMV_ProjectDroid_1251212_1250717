package pt.isep.dssmv.mapaisep.mappers;

import org.json.JSONException;
import org.json.JSONObject;

import pt.isep.dssmv.mapaisep.dto.LocalizacaoDTO;
import pt.isep.dssmv.mapaisep.model.Amigo;

/** Converte o JSON de localizacoes/{uid} em DTO e põe essa posição no Amigo. */
public final class LocalizacaoAmigoMapper {
    public LocalizacaoDTO desserializar(JSONObject documento) throws JSONException {
        JSONObject campos = documento.getJSONObject("fields");
        return new LocalizacaoDTO(
                lerNumero(campos.getJSONObject("latitude")),
                lerNumero(campos.getJSONObject("longitude"))
        );
    }

    // DTO -> Model: a posição passa a fazer parte do Amigo.
    public void preencherPosicao(LocalizacaoDTO dto, Amigo amigo) {
        amigo.definirPosicao(dto.latitude, dto.longitude);
    }

    private double lerNumero(JSONObject campo) throws JSONException {
        if (campo.has("doubleValue")) {
            return campo.getDouble("doubleValue");
        }
        return Double.parseDouble(campo.getString("integerValue"));
    }
}
