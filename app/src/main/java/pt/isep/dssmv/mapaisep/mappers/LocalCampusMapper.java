package pt.isep.dssmv.mapaisep.mappers;

import pt.isep.dssmv.mapaisep.dto.LocalDTO;
import pt.isep.dssmv.mapaisep.model.Local;

// DTO -> Model dos locais do campus (salas e biblioteca, coleção locais).
public final class LocalCampusMapper {

    private LocalCampusMapper() {
    }

    // Converte um LocalDTO num Local do Model. O construtor do Local valida os dados.
    public static Local localDTO2Local(LocalDTO dto) {
        return new Local(dto.getId(), dto.getNome(), dto.getLatitude(), dto.getLongitude());
    }
}
