package pt.isep.dssmv.mapaisep.mappers;

import pt.isep.dssmv.mapaisep.dto.LocalizacaoDTO;
import pt.isep.dssmv.mapaisep.model.Amigo;

// DTO -> Model da posição de um amigo (coleção localizacoes).
// Não cria nenhum Model: acrescenta a latitude e a longitude a um Amigo que já existe.
public final class LocalizacaoAmigoMapper {

    private LocalizacaoAmigoMapper() {
    }

    // Põe a latitude e a longitude do DTO no Amigo (o Amigo valida os valores).
    public static void preencherPosicao(LocalizacaoDTO dto, Amigo amigo) {
        amigo.definirPosicao(dto.getLatitude(), dto.getLongitude());
    }
}
