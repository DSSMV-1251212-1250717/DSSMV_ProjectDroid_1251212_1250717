package pt.isep.dssmv.mapaisep.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.isep.dssmv.mapaisep.dto.AlunoDTO;
import pt.isep.dssmv.mapaisep.dto.AmigoDTO;
import pt.isep.dssmv.mapaisep.dto.AulaDTO;
import pt.isep.dssmv.mapaisep.dto.LocalDTO;
import pt.isep.dssmv.mapaisep.dto.LocalizacaoDTO;
import pt.isep.dssmv.mapaisep.dto.LoginDTO;
import pt.isep.dssmv.mapaisep.dto.PresencaDTO;
import pt.isep.dssmv.mapaisep.dto.SessaoDTO;

// Testes que correm no emulador ou no telemóvel, e não no computador.
// O JsonHandler usa o org.json (JSONObject, JSONArray), que faz parte do Android:
// no computador só existe uma versão "falsa" que dá erro ("Method ... not mocked").
// O Android Studio instala a app e uma app de testes, corre os métodos @Test dentro
// da nossa app (sem abrir ecrãs) e mostra o resultado: verde = passou, vermelho = falhou.
//
// Os exemplos de JSON são iguais aos que o Firebase manda.
@RunWith(AndroidJUnit4.class)
public class JsonHandlerTest {

    // ------------------------------- JSON -> DTO -------------------------------

    @Test
    public void lerAula() throws Exception {
        JSONObject documento = new JSONObject("{"
                + "\"name\":\"projects/p/databases/(default)/documents/aulas/a1\","
                + "\"fields\":{"
                + "\"nome\":{\"stringValue\":\"DSSMV\"},"
                + "\"diaSemana\":{\"integerValue\":\"2\"},"
                + "\"horaInicio\":{\"stringValue\":\"09:00\"},"
                + "\"horaFim\":{\"stringValue\":\"11:00\"},"
                + "\"salaId\":{\"stringValue\":\"B201\"},"
                + "\"turmaId\":{\"stringValue\":\"TurmaB\"}}}");

        AulaDTO dto = JsonHandler.json2AulaDTO(documento);

        assertEquals("DSSMV", dto.getNome());
        assertEquals(2, dto.getDiaSemana());
        assertEquals("09:00", dto.getHoraInicio());
        assertEquals("11:00", dto.getHoraFim());
        assertEquals("B201", dto.getSalaId());
        assertEquals("TurmaB", dto.getTurmaId());
    }

    @Test
    public void lerAlunoSemFoto() throws Exception {
        JSONObject documento = new JSONObject("{"
                + "\"name\":\"projects/p/databases/(default)/documents/alunos/uid123\","
                + "\"fields\":{"
                + "\"nome\":{\"stringValue\":\"Marco\"},"
                + "\"numero\":{\"stringValue\":\"1251212\"},"
                + "\"turmaId\":{\"stringValue\":\"TurmaB\"}}}");

        AlunoDTO dto = JsonHandler.json2AlunoDTO(documento);

        assertEquals("uid123", dto.getUid());      // vem do fim do caminho do documento
        assertEquals("Marco", dto.getNome());
        assertEquals("1251212", dto.getNumero());
        assertEquals("TurmaB", dto.getTurmaId());
        assertNull(dto.getFotoBase64());           // a foto não existe: fica null
    }

    @Test
    public void lerAmigo() throws Exception {
        JSONObject documento = new JSONObject("{"
                + "\"name\":\"projects/p/databases/(default)/documents/alunos/eu/amigos/uid123\","
                + "\"fields\":{"
                + "\"nome\":{\"stringValue\":\"Marco\"},"
                + "\"numero\":{\"stringValue\":\"1251212\"}}}");

        AmigoDTO dto = JsonHandler.json2AmigoDTO(documento);

        assertEquals("uid123", dto.getUid());
        assertEquals("Marco", dto.getNome());
        assertEquals("1251212", dto.getNumero());
    }

    @Test
    public void lerLocalComCoordenadasEmDecimalInteiroOuTexto() throws Exception {
        // O Firebase Console deixa guardar números de três formas: as três têm de funcionar.
        LocalDTO decimal = JsonHandler.json2LocalDTO(documentoLocal("{\"doubleValue\":41.178}"));
        LocalDTO inteiro = JsonHandler.json2LocalDTO(documentoLocal("{\"integerValue\":\"41\"}"));
        LocalDTO texto = JsonHandler.json2LocalDTO(documentoLocal("{\"stringValue\":\"41.178\"}"));

        assertEquals(41.178, decimal.getLatitude(), 0.0001);
        assertEquals(41.0, inteiro.getLatitude(), 0.0001);
        assertEquals(41.178, texto.getLatitude(), 0.0001);
        assertEquals("B201", decimal.getId());
        assertEquals("Sala B201", decimal.getNome());
    }

    @Test
    public void lerLocalSemNomeUsaOId() throws Exception {
        JSONObject documento = new JSONObject("{"
                + "\"name\":\"projects/p/databases/(default)/documents/locais/Biblioteca\","
                + "\"fields\":{"
                + "\"latitude\":{\"doubleValue\":41.178},"
                + "\"longitude\":{\"doubleValue\":-8.607}}}");

        LocalDTO dto = JsonHandler.json2LocalDTO(documento);

        assertEquals("Biblioteca", dto.getNome());
    }

    @Test
    public void lerLocalizacao() throws Exception {
        JSONObject documento = new JSONObject("{\"fields\":{"
                + "\"latitude\":{\"doubleValue\":41.178},"
                + "\"longitude\":{\"doubleValue\":-8.607}}}");

        LocalizacaoDTO dto = JsonHandler.json2LocalizacaoDTO(documento);

        assertEquals(41.178, dto.getLatitude(), 0.0001);
        assertEquals(-8.607, dto.getLongitude(), 0.0001);
    }

    @Test
    public void lerSessaoDoLoginEDaRenovacao() throws Exception {
        // O login e a renovação do token dão os mesmos dados com nomes diferentes.
        SessaoDTO login = JsonHandler.json2SessaoDTO("{"
                + "\"localId\":\"uid123\",\"idToken\":\"id1\","
                + "\"refreshToken\":\"ref1\",\"expiresIn\":\"3600\"}");
        SessaoDTO renovacao = JsonHandler.json2SessaoDTO("{"
                + "\"user_id\":\"uid123\",\"id_token\":\"id2\","
                + "\"refresh_token\":\"ref2\",\"expires_in\":\"3600\"}");

        assertEquals("uid123", login.getUid());
        assertEquals("id1", login.getIdToken());
        assertEquals("ref1", login.getRefreshToken());
        assertEquals(3600, login.getExpiraEmSegundos());

        assertEquals("uid123", renovacao.getUid());
        assertEquals("id2", renovacao.getIdToken());
        assertEquals("ref2", renovacao.getRefreshToken());
        assertEquals(3600, renovacao.getExpiraEmSegundos());
    }

    @Test
    public void lerContagemDaBiblioteca() throws Exception {
        String corpo = "[{\"result\":{\"aggregateFields\":{\"total\":{\"integerValue\":\"3\"}}}}]";

        assertEquals(3, JsonHandler.json2Contagem(corpo));
    }

    @Test
    public void lerMensagemDeErroDoFirebase() {
        String corpo = "{\"error\":{\"code\":400,\"message\":\"INVALID_LOGIN_CREDENTIALS\"}}";

        assertEquals("INVALID_LOGIN_CREDENTIALS", JsonHandler.json2MensagemErro(corpo));
        assertNull(JsonHandler.json2MensagemErro("isto não é JSON"));
    }

    // ------------------------------- DTO -> JSON -------------------------------

    @Test
    public void escreverLogin() throws Exception {
        JSONObject json = JsonHandler.loginDTO2Json(new LoginDTO("1250717@isep.ipp.pt", "segredo"));

        assertEquals("1250717@isep.ipp.pt", json.getString("email"));
        assertEquals("segredo", json.getString("password"));
        assertEquals(true, json.getBoolean("returnSecureToken"));
    }

    @Test
    public void escreverAmigo() throws Exception {
        AmigoDTO dto = new AmigoDTO();
        dto.setUid("uid123");
        dto.setNome("Marco");
        dto.setNumero("1251212");

        JSONObject json = JsonHandler.amigoDTO2Json(dto, "projects/p/databases/(default)/documents/alunos/eu/amigos/uid123");

        assertEquals("projects/p/databases/(default)/documents/alunos/eu/amigos/uid123", json.getString("name"));
        JSONObject campos = json.getJSONObject("fields");
        assertEquals("Marco", campos.getJSONObject("nome").getString("stringValue"));
        assertEquals("1251212", campos.getJSONObject("numero").getString("stringValue"));
    }

    @Test
    public void escreverFotoSoComOCampoDaFoto() throws Exception {
        JSONObject json = JsonHandler.fotoDTO2Json(new AlunoDTO("uid123", null, null, null, "QUJD"));

        JSONObject campos = json.getJSONObject("fields");
        assertEquals("QUJD", campos.getJSONObject("fotoBase64").getString("stringValue"));
        assertEquals(1, campos.length());   // só a foto: o nome, o número e a turma não vão
    }

    @Test
    public void escreverPosicao() throws Exception {
        JSONObject json = JsonHandler.localizacaoDTO2Json(new LocalizacaoDTO(41.178, -8.607));

        JSONObject campos = json.getJSONObject("fields");
        assertEquals(41.178, campos.getJSONObject("latitude").getDouble("doubleValue"), 0.0001);
        assertEquals(-8.607, campos.getJSONObject("longitude").getDouble("doubleValue"), 0.0001);
    }

    @Test
    public void escreverPresenca() throws Exception {
        JSONObject json = JsonHandler.presencaDTO2Json(new PresencaDTO("2026-10-30T10:00:00Z"));

        assertEquals("2026-10-30T10:00:00Z",
                json.getJSONObject("fields").getJSONObject("atualizadoEm").getString("timestampValue"));
    }

    // Documento de um local com a latitude no formato pedido.
    private JSONObject documentoLocal(String latitude) throws Exception {
        return new JSONObject("{"
                + "\"name\":\"projects/p/databases/(default)/documents/locais/B201\","
                + "\"fields\":{"
                + "\"nome\":{\"stringValue\":\"Sala B201\"},"
                + "\"latitude\":" + latitude + ","
                + "\"longitude\":{\"doubleValue\":-8.607}}}");
    }
}
