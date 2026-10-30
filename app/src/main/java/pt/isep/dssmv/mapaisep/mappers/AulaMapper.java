package pt.isep.dssmv.mapaisep.mappers;

import java.time.LocalTime;

import pt.isep.dssmv.mapaisep.dto.AulaDTO;
import pt.isep.dssmv.mapaisep.model.Aula;

// DTO -> Model da aula. Não sabe o que é JSON (isso é o JsonHandler),
// não faz pedidos HTTP nem mexe no ecrã.
public final class AulaMapper {

    private AulaMapper() {
    }

    // Converte um AulaDTO numa Aula do Model.
    public static Aula aulaDTO2Aula(AulaDTO dto) {
        // Converte as horas (texto, por exemplo "09:00") em LocalTime.
        LocalTime inicio = LocalTime.parse(dto.getHoraInicio());
        LocalTime fim = LocalTime.parse(dto.getHoraFim());

        // Cria o objeto do Model. O construtor valida os dados da aula.
        // A sala fica só com o código (salaId); as coordenadas estão em locais.
        return new Aula(dto.getNome(), dto.getDiaSemana(), inicio, fim, dto.getSalaId());
    }
}
