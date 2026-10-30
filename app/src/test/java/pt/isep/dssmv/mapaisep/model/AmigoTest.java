package pt.isep.dssmv.mapaisep.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import pt.isep.dssmv.mapaisep.exceptions.InvalidDataException;

// Testes unitários: correm no computador (JUnit), sem abrir a app nem o emulador.
// Cada método @Test usa o nosso código e confirma o resultado com assert:
// verde = passou, vermelho = falhou (e o JUnit diz o que esperava e o que recebeu).
// O assertThrows está explicado no AulaTest.
//
// Testa as validações do Amigo (Information Expert: é o Amigo que valida os seus dados).
public class AmigoTest {

    @Test
    public void amigoValidoECriado() {
        Amigo amigo = new Amigo("uid123", "Marco", "1251212");

        assertEquals("uid123", amigo.getUid());
        assertEquals("Marco", amigo.getNome());
        assertEquals("1251212", amigo.getNumero());
    }

    @Test
    public void uidVazioLancaExcecao() {
        assertThrows(InvalidDataException.class, () -> new Amigo("", "Marco", "1251212"));
    }

    @Test
    public void nomeVazioLancaExcecao() {
        assertThrows(InvalidDataException.class, () -> new Amigo("uid123", " ", "1251212"));
    }

    @Test
    public void numeroSemSeteDigitosLancaExcecao() {
        // 6 dígitos e letras: os dois têm de ser recusados.
        assertThrows(InvalidDataException.class, () -> new Amigo("uid123", "Marco", "125121"));
        assertThrows(InvalidDataException.class, () -> new Amigo("uid123", "Marco", "12512AB"));
    }

    @Test
    public void posicaoForaDosLimitesLancaExcecao() {
        Amigo amigo = new Amigo("uid123", "Marco", "1251212");

        // Latitude acima de 90 e longitude abaixo de -180 não existem.
        assertThrows(InvalidDataException.class, () -> amigo.definirPosicao(91, -8.6));
        assertThrows(InvalidDataException.class, () -> amigo.definirPosicao(41.17, -181));
    }

    @Test
    public void temPosicaoSoDepoisDeDefinida() {
        Amigo amigo = new Amigo("uid123", "Marco", "1251212");
        assertFalse(amigo.temPosicao());

        amigo.definirPosicao(41.178, -8.607);

        assertTrue(amigo.temPosicao());
        assertEquals(41.178, amigo.getLatitude(), 0.0001);
        assertEquals(-8.607, amigo.getLongitude(), 0.0001);
    }
}
