package pt.isep.dssmv.mapaisep.mappers;

import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalTime;
import pt.isep.dssmv.mapaisep.dto.AulaDTO;
import pt.isep.dssmv.mapaisep.model.Aula;
import pt.isep.dssmv.mapaisep.model.Sala;

// Converte JSON -> DTO -> modelo. Não faz pedidos HTTP nem atualiza o ecrã.
public class AulaMapper {
    // Converts Firestore JSON fields into an AulaDTO.
    public AulaDTO desserializarAula(JSONObject campos) throws JSONException {
        AulaDTO dto = new AulaDTO();

        dto.setNome(campos.getJSONObject("nome").getString("stringValue"));
        dto.setDiaSemana(Integer.parseInt(campos.getJSONObject("diaSemana").getString("integerValue")));
        dto.setHoraInicio(campos.getJSONObject("horaInicio").getString("stringValue"));
        dto.setHoraFim(campos.getJSONObject("horaFim").getString("stringValue"));
        dto.setSalaId(campos.getJSONObject("salaId").getString("stringValue"));
        dto.setTurmaId(campos.getJSONObject("turmaId").getString("stringValue"));

        return dto;
    }

    // Converts an AulaDTO into a validated Aula Model object.
    public Aula converterParaModelo(AulaDTO dto) {
        // Convert API strings into Java LocalTime objects.
        LocalTime inicio = LocalTime.parse(dto.getHoraInicio());
        LocalTime fim = LocalTime.parse(dto.getHoraFim());

        // Create a Sala object using its room code.
        // Geographic coordinates will be added later.
        Sala sala = new Sala(dto.getSalaId());

        // Create the Model object.
        // The constructor validates the lesson information.
        return new Aula(dto.getNome(), dto.getDiaSemana(), inicio, fim, sala);
    }
}
