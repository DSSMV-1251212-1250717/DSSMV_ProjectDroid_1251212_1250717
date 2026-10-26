package pt.isep.dssmv.mapaisep.mappers;

import org.json.JSONException;
import org.json.JSONObject;

import pt.isep.dssmv.mapaisep.dto.LocalMapaDTO;
import pt.isep.dssmv.mapaisep.model.LocalMapa;

/** Converte cada documento Firestore de locais para dados usados pelo mapa. */
public final class LocalCampusMapper {
    public LocalMapaDTO desserializar(JSONObject documento) throws JSONException {
        JSONObject campos = documento.getJSONObject("fields");
        String caminho = documento.getString("name");
        String id = caminho.substring(caminho.lastIndexOf('/') + 1);

        return new LocalMapaDTO(
                id,
                campos.getJSONObject("nome").getString("stringValue"),
                campos.getJSONObject("tipo").getString("stringValue"),
                campos.getJSONObject("edificio").getString("stringValue"),
                lerNumero(campos.getJSONObject("latitude")),
                lerNumero(campos.getJSONObject("longitude"))
        );
    }

    public LocalMapa converterParaModelo(LocalMapaDTO dto) {
        return new LocalMapa(dto.id, dto.nome, dto.tipo, dto.edificio,
                dto.latitude, dto.longitude);
    }

    private double lerNumero(JSONObject campo) throws JSONException {
        if (campo.has("doubleValue")) {
            return campo.getDouble("doubleValue");
        }
        if (campo.has("integerValue")) {
            return Double.parseDouble(campo.getString("integerValue"));
        }

        // O Firebase Console também permite guardar coordenadas como texto.
        // Aceitar esse formato impede que um marcador válido seja ignorado.
        if (campo.has("stringValue")) {
            return Double.parseDouble(campo.getString("stringValue"));
        }

        throw new JSONException("Coordenada sem valor numérico");
    }
}
