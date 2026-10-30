package pt.isep.dssmv.mapaisep.mappers;

import java.time.Instant;

import pt.isep.dssmv.mapaisep.dto.LocalizacaoDTO;
import pt.isep.dssmv.mapaisep.model.Amigo;

// DTO -> Model da posição de um amigo (coleção localizacoes).
// Não cria nenhum Model: acrescenta a latitude e a longitude a um Amigo que já existe.
public final class LocalizacaoAmigoMapper {

    private LocalizacaoAmigoMapper() {
    }

    // Põe a latitude, a longitude e a hora do DTO no Amigo (o Amigo valida os valores).
    // A hora vem em texto ISO 8601 ("2026-10-30T13:51:00Z") e passa a Instant.
    public static void preencherPosicao(LocalizacaoDTO dto, Amigo amigo) {
        if (dto.getAtualizadoEm() == null) {
            amigo.definirPosicao(dto.getLatitude(), dto.getLongitude());
            return;
        }
        Instant hora = Instant.parse(dto.getAtualizadoEm());
        amigo.definirPosicao(dto.getLatitude(), dto.getLongitude(), hora);
    }
}
