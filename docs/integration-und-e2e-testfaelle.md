# Integration- und End-to-End-Testfaelle

Hinweis: Die Anwendung verwendet aktuell keine externe Datenbank, sondern einen isolierten In-Memory-Datenbestand im `LearningPlatformService`. Die Tests laufen im Quarkus-Testprofil und erzeugen eigene Testdaten ueber die API.

## Integrationstests

| Name des Tests | Given | When | Then |
| --- | --- | --- | --- |
| `registerUserThroughApiAndReadItBackFromServiceState` | Ein neuer Benutzer mit `username="integration-user"` und `email="integration-user@example.com"` soll registriert werden. | `POST /register` wird aufgerufen. | Die API liefert `201`; der Benutzer kann danach ueber `GET /users/{id}` aus dem Service-Zustand gelesen werden. |
| `createProjectThroughApiAndReadItBackFromServiceState` | Ein neues Projekt mit Owner `1` und Mitgliedern `[1, 2]` soll erstellt werden. | `POST /projects` wird aufgerufen. | Die API liefert `201`; das Projekt kann danach ueber `GET /projects/{id}` mit Name, Owner und Mitgliedern gelesen werden. |

## End-to-End-Test

| Name des Tests | Given | When | Then |
| --- | --- | --- | --- |
| `createProjectTaskAndCommentWorkflowMakesTaskAndCommentAvailable` | Ein Projekt wird ueber `POST /projects` angelegt. | Zu diesem Projekt wird ueber `POST /tasks` eine Aufgabe angelegt und ueber `POST /tasks/{id}/comments` kommentiert. | Die Aufgabe erscheint in `GET /projects/{id}/tasks`; der Kommentar erscheint in `GET /tasks/{id}/comments`. |
