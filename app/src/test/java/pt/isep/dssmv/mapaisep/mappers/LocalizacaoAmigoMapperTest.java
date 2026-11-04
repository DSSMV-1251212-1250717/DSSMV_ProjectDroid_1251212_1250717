package pt.isep.dssmv.mapaisep.mappers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import pt.isep.dssmv.mapaisep.dto.LocalizacaoDTO;
import pt.isep.dssmv.mapaisep.model.Amigo;

// Testes unitários (JUnit): correm no computador; verde = passou, vermelho = falhou.
// Testa que a posição (LocalizacaoDTO) fica guardada no Amigo a que pertence.
public class LocalizacaoAmigoMapperTest {

    @Test
    public void preencherPosicaoPoeLatitudeELongitudeNoAmigo() {
        Amigo amigo = new Amigo("uid123", "Marco", "1251212");
        LocalizacaoDTO dto = new LocalizacaoDTO(41.178, -8.607);

        LocalizacaoAmigoMapper.preencherPosicao(dto, amigo);

        assertTrue(amigo.temPosicao());
        assertEquals(41.178, amigo.getLatitude(), 0.0001);
        assertEquals(-8.607, amigo.getLongitude(), 0.0001);
    }
}
