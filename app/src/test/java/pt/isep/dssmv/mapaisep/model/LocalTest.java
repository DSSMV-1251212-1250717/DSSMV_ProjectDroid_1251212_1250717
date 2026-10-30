package pt.isep.dssmv.mapaisep.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import pt.isep.dssmv.mapaisep.exceptions.InvalidDataException;

// Testes unitários: correm no computador (JUnit), sem abrir a app nem o emulador.
// Cada método @Test usa o nosso código e confirma o resultado com assert:
// verde = passou, vermelho = falhou (e o JUnit diz o que esperava e o que recebeu).
// O assertThrows está explicado no AulaTest.
//
// Testa as validações do Local (Information Expert: é o Local que valida os seus dados).
public class LocalTest {

    @Test
    public void localValidoECriado() {
        Local local = new Local("B201", "Sala B201", 41.178, -8.607);

        assertEquals("B201", local.getId());
        assertEquals("Sala B201", local.getNome());
        assertEquals(41.178, local.getLatitude(), 0.0001);
        assertEquals(-8.607, local.getLongitude(), 0.0001);
    }

    @Test
    public void idVazioLancaExcecao() {
        assertThrows(InvalidDataException.class, () -> new Local("", "Sala B201", 41.178, -8.607));
    }

    @Test
    public void nomeVazioLancaExcecao() {
        assertThrows(InvalidDataException.class, () -> new Local("B201", "", 41.178, -8.607));
    }

    @Test
    public void coordenadasForaDosLimitesLancamExcecao() {
        assertThrows(InvalidDataException.class, () -> new Local("B201", "Sala B201", -91, -8.607));
        assertThrows(InvalidDataException.class, () -> new Local("B201", "Sala B201", 41.178, 181));
    }
}
