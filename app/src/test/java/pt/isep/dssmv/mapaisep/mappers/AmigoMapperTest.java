package pt.isep.dssmv.mapaisep.mappers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pt.isep.dssmv.mapaisep.dto.AlunoDTO;
import pt.isep.dssmv.mapaisep.dto.AmigoDTO;
import pt.isep.dssmv.mapaisep.model.Amigo;

// Testes unitários: correm no computador (JUnit), sem abrir a app nem o emulador.
// Cada método @Test usa o nosso código e confirma o resultado com assert:
// verde = passou, vermelho = falhou (e o JUnit diz o que esperava e o que recebeu).
//
// Testa as conversões do AmigoMapper: AmigoDTO -> Amigo, AlunoDTO -> Amigo e Amigo -> AmigoDTO.
public class AmigoMapperTest {

    @Test
    public void amigoDTOParaAmigo() {
        AmigoDTO dto = new AmigoDTO();
        dto.setUid("uid123");
        dto.setNome("Marco");
        dto.setNumero("1251212");

        Amigo amigo = AmigoMapper.amigoDTO2Amigo(dto);

        assertEquals("uid123", amigo.getUid());
        assertEquals("Marco", amigo.getNome());
        assertEquals("1251212", amigo.getNumero());
    }

    @Test
    public void alunoDTOParaAmigo() {
        // A turma e a foto não fazem parte do Amigo: são ignoradas.
        AlunoDTO dto = new AlunoDTO("uid123", "Marco", "1251212", "TurmaB", null);

        Amigo amigo = AmigoMapper.alunoDTO2Amigo(dto);

        assertEquals("uid123", amigo.getUid());
        assertEquals("Marco", amigo.getNome());
        assertEquals("1251212", amigo.getNumero());
    }

    @Test
    public void amigoParaAmigoDTOMantemOsDados() {
        Amigo amigo = new Amigo("uid123", "Marco", "1251212");

        AmigoDTO dto = AmigoMapper.amigo2AmigoDTO(amigo);

        assertEquals("uid123", dto.getUid());
        assertEquals("Marco", dto.getNome());
        assertEquals("1251212", dto.getNumero());
    }
}
