package pt.isep.dssmv.mapaisep.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;

import org.junit.Test;

import pt.isep.dssmv.mapaisep.exceptions.InvalidDataException;

// Testes unitários: correm no computador (JUnit), sem abrir a app nem o emulador.
// Cada método @Test usa o nosso código e confirma o resultado com assert:
// verde = passou, vermelho = falhou (e o JUnit diz o que esperava e o que recebeu).
//
// Testa as validações da Aula (Information Expert: é a Aula que valida os seus dados).
public class AulaTest {

    private static final LocalTime NOVE = LocalTime.of(9, 0);
    private static final LocalTime ONZE = LocalTime.of(11, 0);

    @Test
    public void aulaValidaECriada() {
        Aula aula = new Aula("DSSMV", 2, NOVE, ONZE, "B201");

        assertEquals("DSSMV", aula.getDisciplina());
        assertEquals(2, aula.getDiaSemana());
        assertEquals(NOVE, aula.getHoraInicio());
        assertEquals(ONZE, aula.getHoraFim());
        assertEquals("B201", aula.getSalaId());
    }

    @Test
    public void disciplinaVaziaLancaExcecao() {
        // Disciplina só com espaços: tem de ser recusada.
        assertThrows(InvalidDataException.class, () -> new Aula("  ", 2, NOVE, ONZE, "B201"));
    }

    @Test
    public void diaForaDe1a7LancaExcecao() {
        // O dia 0 e o dia 8 não existem: têm de ser recusados.
        assertThrows(InvalidDataException.class, () -> new Aula("DSSMV", 0, NOVE, ONZE, "B201"));
        assertThrows(InvalidDataException.class, () -> new Aula("DSSMV", 8, NOVE, ONZE, "B201"));
    }

    // COMO FUNCIONA O assertThrows
    //
    // O assertThrows verifica se um código lança uma exceção.
    // Por dentro é só um try/catch, mais ou menos assim:
    //
    //     try {
    //         corre o código recebido;      <- a exceção tem de acontecer AQUI
    //     } catch (exceção) {
    //         se for do tipo esperado: VERDE (passou)
    //     }
    //     se não houve exceção: VERMELHO (falhou)
    //
    // Um try/catch só apanha exceções que acontecem DENTRO dele.
    // Por isso a Aula não pode ser criada nesta linha do teste:
    //
    //     assertThrows(InvalidDataException.class, new Aula(...));        // ERRADO
    //
    // Aqui o Java criava a Aula ANTES de entrar no assertThrows (fora do try),
    // a exceção saltava para fora e o teste ficava vermelho.
    //
    // Em vez disso, entrega-se a "receita" para criar a Aula: () -> new Aula(...).
    // A Aula ainda não é criada. Só é criada quando o assertThrows corre a receita,
    // já DENTRO do try dele, e aí a exceção é apanhada.
    //
    //     assertThrows(InvalidDataException.class, () -> new Aula(...));  // CERTO
    @Test
    public void fimAntesDoInicioLancaExcecao() {
        // Aula das 11h às 9h: o fim é antes do início, por isso tem de ser recusada.
        assertThrows(InvalidDataException.class, () -> new Aula("DSSMV", 2, ONZE, NOVE, "B201"));
    }

    @Test
    public void semSalaLancaExcecao() {
        // Uma aula tem de ter sala.
        assertThrows(InvalidDataException.class, () -> new Aula("DSSMV", 2, NOVE, ONZE, ""));
    }

    @Test
    public void estaADecorrerSoDentroDaAula() {
        Aula aula = new Aula("DSSMV", 2, NOVE, ONZE, "B201");

        assertTrue(aula.estaADecorrer(LocalTime.of(10, 0)));    // a meio da aula
        assertTrue(aula.estaADecorrer(NOVE));                   // no minuto em que começa
        assertFalse(aula.estaADecorrer(ONZE));                  // no minuto em que acaba, já não
        assertFalse(aula.estaADecorrer(LocalTime.of(8, 59)));   // antes de começar
    }
}
