package pt.isep.dssmv.mapaisep.mappers;

import pt.isep.dssmv.mapaisep.dto.AlunoDTO;
import pt.isep.dssmv.mapaisep.dto.AmigoDTO;
import pt.isep.dssmv.mapaisep.model.Amigo;

// DTO <-> Model do amigo. Todos os objetos Amigo são criados aqui:
// a partir da lista de amigos (AmigoDTO) ou do documento do aluno (AlunoDTO).
// Não valida nada: quem valida é o próprio Amigo (Information Expert).
public final class AmigoMapper {

    private AmigoMapper() {
    }

    // Da lista de amigos (alunos/{teu uid}/amigos/{uid}).
    public static Amigo amigoDTO2Amigo(AmigoDTO dto) {
        return new Amigo(dto.getUid(), dto.getNome(), dto.getNumero());
    }

    // Do documento principal do aluno (alunos/{uid}): usado ao adicionar um amigo.
    public static Amigo alunoDTO2Amigo(AlunoDTO dto) {
        return new Amigo(dto.getUid(), dto.getNome(), dto.getNumero());
    }

    // Model -> DTO: usado para gravar a amizade (o JsonHandler transforma-o em JSON).
    public static AmigoDTO amigo2AmigoDTO(Amigo amigo) {
        AmigoDTO dto = new AmigoDTO();
        dto.setUid(amigo.getUid());
        dto.setNome(amigo.getNome());
        dto.setNumero(amigo.getNumero());
        return dto;
    }
}
