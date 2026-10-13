package pt.isep.dssmv.mapaisep.services;

import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.Calendar;
import java.util.List;

public class HorarioService {

    // An interface declares methods that other classes must implement.

    public interface HorarioCallback {
        // MenuPrincipal requests the lessons from HorarioService.
        // Since Back4app takes time to respond, the lessons cannot be returned immediately.
        // Instead, MenuPrincipal defines a callback function containing the instructions
        // to execute when the data arrives, and passes it to HorarioService.
        // HorarioService keeps a reference to this callback while waiting for the response.
        // When Back4app responds, HorarioService executes the callback,
        // passing the list of lessons or an error back to MenuPrincipal.
        void onResult(List<ParseObject> aulas, String erro);
    }

    public void obterAulasHoje(HorarioCallback callback) {
        //passamos a implementacao da funcao onResult e os seus valores
        //dai horarioCallback callback
        // Receives the callback object created in MenuPrincipal,
        // containing its implementation of onResult().

        // Get the user authenticated by the Parse SDK.
        ParseUser aluno = ParseUser.getCurrentUser();

        // Stop if no user is logged in.
        if (aluno == null) {
            callback.onResult(null, "Nenhum aluno autenticado.");
            return;
        }

        // Fetch the student's latest data from Back4app.
        //objeto aluno com os dados recebidos do servidor, se a consulta de aluno tiver sucesso
        aluno.fetchInBackground((alunoAtualizado, error) -> {

            if (error != null) {
                callback.onResult(null, error.getMessage());

                return;
            }

            // Get the Turma object referenced by the student's TurmaAluno field.
            ParseObject turma = alunoAtualizado.getParseObject("TurmaAluno");

            if (turma == null) {
                callback.onResult(null, "O aluno não tem turma associada.");
                return;
            }

            // Get the current day of the week.
            // Monday = 1, Tuesday = 2, ..., Sunday = 7.
            Calendar calendario = Calendar.getInstance();

            int dia = calendario.get(Calendar.DAY_OF_WEEK);
            int diaSemana;

            if (dia == Calendar.SUNDAY) {
                diaSemana = 7;
            } else {
                diaSemana = dia - 1;
            }

            // Create a query for the Aula class in Back4app.
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Aula");

            // Retrieve only lessons belonging to the student's class.
            query.whereEqualTo("turma", turma);

            // Retrieve only lessons scheduled for today.
            query.whereEqualTo("diaSemana", diaSemana);

            // Sort lessons by their starting time.
            query.orderByAscending("horaInicio");

            // Send the request and receive the lessons asynchronously.
            query.findInBackground((aulas, erroConsulta) -> {

                if (erroConsulta != null) {
                    callback.onResult(null, erroConsulta.getMessage());
                    return;
                }

                // Deliver the retrieved lessons to the caller.
                callback.onResult(aulas, null);
            });
        });
    }
}
