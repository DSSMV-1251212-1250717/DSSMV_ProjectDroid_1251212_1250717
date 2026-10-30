package pt.isep.dssmv.mapaisep.mappers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import pt.isep.dssmv.mapaisep.dto.LocalizacaoDTO;
import pt.isep.dssmv.mapaisep.model.Amigo;

// Testes unitários: correm no computador (JUnit), sem abrir a app nem o emulador.
// Cada método @Test usa o nosso código e confirma o resultado com assert:
// verde = passou, vermelho = falhou (e o JUnit diz o que esperava e o que recebeu).
//
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
