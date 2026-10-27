package pt.isep.dssmv.mapaisep.mappers;

import org.json.JSONException;
import org.json.JSONObject;

import pt.isep.dssmv.mapaisep.dto.LocalDTO;
import pt.isep.dssmv.mapaisep.model.Local;

/** Converte cada documento Firestore de locais para dados usados pelo mapa. */
public final class LocalCampusMapper {
    public LocalDTO desserializar(JSONObject documento) throws JSONException {
        JSONObject campos = documento.getJSONObject("fields");
        String caminho = documento.getString("name");
        String id = caminho.substring(caminho.lastIndexOf('/') + 1);

        return new LocalDTO(
                id,
                campos.getJSONObject("nome").getString("stringValue"),
                campos.getJSONObject("tipo").getString("stringValue"),
                lerEdificio(campos),
                lerNumero(campos.getJSONObject("latitude")),
                lerNumero(campos.getJSONObject("longitude"))
        );
    }

    public Local converterParaModelo(LocalDTO dto) {
        return new Local(dto.id, dto.nome, dto.tipo, dto.edificio,
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

    // Alguns documentos foram criados no Firebase com "edifício" e outros
    // com "edificio". Aceitamos ambas as formas para não ignorar a sala.
    private String lerEdificio(JSONObject campos) throws JSONException {
        if (campos.has("edificio")) {
            return campos.getJSONObject("edificio").getString("stringValue");
        }
        return campos.getJSONObject("edifício").getString("stringValue");
    }
}
