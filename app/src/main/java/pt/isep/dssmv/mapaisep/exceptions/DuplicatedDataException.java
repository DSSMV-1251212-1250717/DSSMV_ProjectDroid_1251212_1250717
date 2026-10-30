package pt.isep.dssmv.mapaisep.exceptions;

// Lançada quando se tenta guardar uma coisa que já existe.
// Na app: adicionar um aluno que já é amigo. Os dados estão certos, só que repetidos,
// por isso o ecrã mostra uma mensagem própria ("Este aluno já é teu amigo.").
// Tem o mesmo nome da exceção dos projetos dos professores (SchoolApplication e SchoolDiskAPI).
public class DuplicatedDataException extends RuntimeException {

    // Código que o service manda ao ecrã pelo callback.
    public static final String CODIGO = "ALREADY_FRIEND";

    public DuplicatedDataException(String mensagem) {
        super(mensagem);
    }
}
