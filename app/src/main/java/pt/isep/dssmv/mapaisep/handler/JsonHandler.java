package pt.isep.dssmv.mapaisep.handler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pt.isep.dssmv.mapaisep.dto.AlunoDTO;
import pt.isep.dssmv.mapaisep.dto.AmigoDTO;
import pt.isep.dssmv.mapaisep.dto.AulaDTO;
import pt.isep.dssmv.mapaisep.dto.LocalDTO;
import pt.isep.dssmv.mapaisep.dto.LocalizacaoDTO;
import pt.isep.dssmv.mapaisep.dto.LoginDTO;
import pt.isep.dssmv.mapaisep.dto.PresencaDTO;
import pt.isep.dssmv.mapaisep.dto.SessaoDTO;

// Tudo o que lê ou escreve JSON de dados fica aqui, nos dois sentidos:
//   JSON -> DTO  (json2...): quando a resposta do Firebase chega;
//   DTO  -> JSON (...2Json): quando a app envia dados ao Firebase.
// Só esta classe conhece o formato do Firestore ("fields", "stringValue", "doubleValue"...).
// Os services não escrevem JSON de dados à mão; os mappers não sabem o que é JSON.
// É o papel do JsonHandler do exemplo dos professores (SchoolDiskClient).
// As pesquisas ao Firestore (runQuery) não estão aqui: não são dados, dizem ao Firebase
// o que procurar, por isso ficam nos services.
public final class JsonHandler {

    private JsonHandler() {
    }

    // ---------------------------------------------------------------------------------
    // JSON -> DTO
    // ---------------------------------------------------------------------------------

    // Documento de alunos/{uid}. O uid vem do caminho do documento (.../alunos/{uid}).
    // Os campos que não existirem ficam null (por exemplo, a foto).
    public static AlunoDTO json2AlunoDTO(JSONObject documento) throws JSONException {
        JSONObject campos = documento.getJSONObject("fields");
        return new AlunoDTO(
                idDoDocumento(documento),
                lerTextoOpcional(campos, "nome"),
                lerTextoOpcional(campos, "numero"),
                lerTextoOpcional(campos, "turmaId"),
                lerTextoOpcional(campos, "fotoBase64")
        );
    }

    // Documento de alunos/{uid do aluno}/amigos/{uid do amigo}.
    public static AmigoDTO json2AmigoDTO(JSONObject documento) throws JSONException {
        JSONObject campos = documento.getJSONObject("fields");

        AmigoDTO dto = new AmigoDTO();
        dto.setUid(idDoDocumento(documento));
        dto.setNome(campos.getJSONObject("nome").getString("stringValue"));
        dto.setNumero(campos.getJSONObject("numero").getString("stringValue"));
        return dto;
    }

    // Documento da coleção aulas. As horas ficam em texto ("09:00"), como na API.
    public static AulaDTO json2AulaDTO(JSONObject documento) throws JSONException {
        JSONObject campos = documento.getJSONObject("fields");

        AulaDTO dto = new AulaDTO();
        dto.setNome(campos.getJSONObject("nome").getString("stringValue"));
        dto.setDiaSemana(Integer.parseInt(campos.getJSONObject("diaSemana").getString("integerValue")));
        dto.setHoraInicio(campos.getJSONObject("horaInicio").getString("stringValue"));
        dto.setHoraFim(campos.getJSONObject("horaFim").getString("stringValue"));
        dto.setSalaId(campos.getJSONObject("salaId").getString("stringValue"));
        dto.setTurmaId(campos.getJSONObject("turmaId").getString("stringValue"));
        return dto;
    }

    // Documento da coleção locais. O id é o nome do documento (por exemplo "B201").
    // Se faltar o nome, usa o id, para a sala não desaparecer do mapa.
    public static LocalDTO json2LocalDTO(JSONObject documento) throws JSONException {
        JSONObject campos = documento.getJSONObject("fields");
        String id = idDoDocumento(documento);

        String nome = lerTextoOpcional(campos, "nome");
        if (nome == null || nome.trim().isEmpty()) {
            nome = id;
        }

        return new LocalDTO(
                id,
                nome,
                lerNumero(campos.getJSONObject("latitude")),
                lerNumero(campos.getJSONObject("longitude"))
        );
    }

    // Documento de localizacoes/{uid}.
    public static LocalizacaoDTO json2LocalizacaoDTO(JSONObject documento) throws JSONException {
        JSONObject campos = documento.getJSONObject("fields");
        return new LocalizacaoDTO(
                lerNumero(campos.getJSONObject("latitude")),
                lerNumero(campos.getJSONObject("longitude"))
        );
    }

    // Resposta do login ou da renovação do token.
    // As duas respostas trazem os mesmos dados com nomes diferentes:
    //   login:     localId, idToken,  refreshToken,  expiresIn
    //   renovação: user_id, id_token, refresh_token, expires_in
    public static SessaoDTO json2SessaoDTO(String corpo) throws JSONException {
        JSONObject json = new JSONObject(corpo);

        if (json.has("id_token")) {
            return new SessaoDTO(
                    json.getString("user_id"),
                    json.getString("id_token"),
                    json.getString("refresh_token"),
                    Long.parseLong(json.getString("expires_in"))
            );
        }

        return new SessaoDTO(
                json.getString("localId"),
                json.getString("idToken"),
                json.getString("refreshToken"),
                Long.parseLong(json.getString("expiresIn"))
        );
    }

    // Resposta do runAggregationQuery da biblioteca:
    // [ { "result": { "aggregateFields": { "total": { "integerValue": "3" } } } } ]
    // Devolve só o número: não precisa de DTO.
    public static int json2Contagem(String corpo) throws JSONException {
        String valor = new JSONArray(corpo).getJSONObject(0)
                .getJSONObject("result")
                .getJSONObject("aggregateFields")
                .getJSONObject("total")
                .getString("integerValue");
        return Integer.parseInt(valor);
    }

    // Mensagem de erro do Firebase, por exemplo "INVALID_LOGIN_CREDENTIALS".
    // Devolve null se a resposta não tiver esse formato.
    public static String json2MensagemErro(String corpo) {
        try {
            return new JSONObject(corpo).getJSONObject("error").getString("message");
        } catch (JSONException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------------------------
    // DTO -> JSON
    // ---------------------------------------------------------------------------------

    // Pedido de login: { "email": ..., "password": ..., "returnSecureToken": true }
    public static JSONObject loginDTO2Json(LoginDTO dto) throws JSONException {
        return new JSONObject()
                .put("email", dto.getEmail())
                .put("password", dto.getPassword())
                .put("returnSecureToken", true);
    }

    // Documento do amigo guardado na lista do dono.
    // O caminho (alunos/{dono}/amigos/{amigo}) vai à parte: não é um dado do amigo,
    // é o sítio onde o Firestore o guarda.
    public static JSONObject amigoDTO2Json(AmigoDTO dto, String caminho) throws JSONException {
        JSONObject campos = new JSONObject()
                .put("nome", texto(dto.getNome()))
                .put("numero", texto(dto.getNumero()));

        return new JSONObject()
                .put("name", caminho)
                .put("fields", campos);
    }

    // Foto nova do aluno: escreve só o campo fotoBase64 (o pedido só altera a foto).
    public static JSONObject fotoDTO2Json(AlunoDTO dto) throws JSONException {
        JSONObject campos = new JSONObject().put("fotoBase64", texto(dto.getFotoBase64()));
        return new JSONObject().put("fields", campos);
    }

    // Posição do aluno: { "fields": { "latitude": {...}, "longitude": {...} } }
    public static JSONObject localizacaoDTO2Json(LocalizacaoDTO dto) throws JSONException {
        JSONObject campos = new JSONObject()
                .put("latitude", new JSONObject().put("doubleValue", dto.getLatitude()))
                .put("longitude", new JSONObject().put("doubleValue", dto.getLongitude()));
        return new JSONObject().put("fields", campos);
    }

    // Presença na biblioteca: { "fields": { "atualizadoEm": { "timestampValue": ... } } }
    public static JSONObject presencaDTO2Json(PresencaDTO dto) throws JSONException {
        JSONObject campos = new JSONObject()
                .put("atualizadoEm", new JSONObject().put("timestampValue", dto.getAtualizadoEm()));
        return new JSONObject().put("fields", campos);
    }

    // ---------------------------------------------------------------------------------
    // Ajudas internas
    // ---------------------------------------------------------------------------------

    // O id de um documento é a última parte do caminho: .../alunos/{uid} -> uid.
    private static String idDoDocumento(JSONObject documento) throws JSONException {
        String caminho = documento.getString("name");
        return caminho.substring(caminho.lastIndexOf('/') + 1);
    }

    // Campo de texto no formato do Firestore: { "stringValue": "..." }.
    private static JSONObject texto(String valor) throws JSONException {
        return new JSONObject().put("stringValue", valor);
    }

    // Texto de um campo; aceita também números (integerValue). null se não existir.
    private static String lerTextoOpcional(JSONObject campos, String nome) {
        JSONObject campo = campos.optJSONObject(nome);
        if (campo == null) {
            return null;
        }
        if (campo.has("stringValue")) {
            return campo.optString("stringValue");
        }
        if (campo.has("integerValue")) {
            return campo.optString("integerValue");
        }
        return null;
    }

    // Número de um campo. O Firebase Console permite guardar coordenadas como número
    // decimal, inteiro ou até texto: aceita os três, para nenhum local válido ser ignorado.
    private static double lerNumero(JSONObject campo) throws JSONException {
        if (campo.has("doubleValue")) {
            return campo.getDouble("doubleValue");
        }
        if (campo.has("integerValue")) {
            return Double.parseDouble(campo.getString("integerValue"));
        }
        if (campo.has("stringValue")) {
            return Double.parseDouble(campo.getString("stringValue"));
        }
        throw new JSONException("Coordenada sem valor numérico");
    }
}
