package pt.isep.dssmv.mapaisep.mappers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.DateTimeException;
import java.time.LocalTime;

import org.junit.Test;

import pt.isep.dssmv.mapaisep.dto.AulaDTO;
import pt.isep.dssmv.mapaisep.model.Aula;

// Testes unitários: correm no computador (JUnit), sem abrir a app nem o emulador.
// Cada método @Test usa o nosso código e confirma o resultado com assert:
// verde = passou, vermelho = falhou (e o JUnit diz o que esperava e o que recebeu).
// O assertThrows está explicado no AulaTest.
//
// Testa a conversão AulaDTO -> Aula (DTO -> Model).
public class AulaMapperTest {

    // Cria um AulaDTO como o que o JsonHandler devolve (horas em texto).
    private AulaDTO criarDTO(String inicio, String fim) {
        AulaDTO dto = new AulaDTO();
        dto.setNome("DSSMV");
        dto.setDiaSemana(2);
        dto.setHoraInicio(inicio);
        dto.setHoraFim(fim);
        dto.setSalaId("B201");
        dto.setTurmaId("TurmaB");
        return dto;
    }

    @Test
    public void converteHorasDeTextoEmLocalTime() {
        Aula aula = AulaMapper.aulaDTO2Aula(criarDTO("09:00", "11:00"));

        assertEquals("DSSMV", aula.getDisciplina());
        assertEquals(2, aula.getDiaSemana());
        assertEquals(LocalTime.of(9, 0), aula.getHoraInicio());
        assertEquals(LocalTime.of(11, 0), aula.getHoraFim());
        assertEquals("B201", aula.getSalaId());
    }

    @Test
    public void horaMalEscritaDaErro() {
        // "9h" não é uma hora no formato HH:mm: o LocalTime.parse recusa-a.
        assertThrows(DateTimeException.class, () -> AulaMapper.aulaDTO2Aula(criarDTO("9h", "11:00")));
    }
}
