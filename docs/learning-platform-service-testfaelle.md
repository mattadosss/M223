# Testfaelle fuer LearningPlatformService

| Name des Tests | Alle Parameter | Erwartetes Resultat |
| --- | --- | --- |
| `registerCreatesUserWithNextId` | `UserRequest(username="lea", email="lea@example.com")` | Ein neuer Benutzer mit `id=3`, `username="lea"` und `email="lea@example.com"` wird zurueckgegeben und ist ueber `getUser(3)` abrufbar. |
| `registerRejectsBlankUsername` | `UserRequest(username=" ", email="leer@example.com")` | Es wird eine `BadRequestException` mit der Meldung `username darf nicht leer sein` geworfen. |
| `loginReturnsTokenAndExistingUser` | `LoginRequest(username="maria", password="secret")` | Die Antwort enthaelt `message="Login erfolgreich"`, `token="demo-token-maria"` und den bestehenden Benutzer `maria` mit `id=1`. |
| `getProjectsUsesDefaultUserWhenNoUserIdIsProvided` | `userId=null` | Es werden nur Projekte zurueckgegeben, in denen Benutzer `1` Owner oder Mitglied ist; das Projekt `Teamarbeit in Java` ist enthalten. |
| `createProjectMergesOwnerIntoMembers` | `ProjectRequest(name="Sprint Planung", description="Backlog ordnen", ownerId=2, memberIds=[1, 2, 1])` | Das Projekt wird mit `id=3`, `ownerId=2` und eindeutigen `memberIds=[2, 1]` angelegt. |
| `createTaskRejectsMissingProjectId` | `TaskRequest(projectId=null, title="Ohne Projekt", description=null, status=null, dueDate=null, assignedUserId=null)` | Es wird eine `BadRequestException` mit der Meldung `projectId darf nicht leer sein` geworfen. |
| `updateTaskKeepsExistingValuesWhenFieldsAreEmpty` | `id=1`, `TaskRequest(projectId=null, title=" ", description=null, status="DONE", dueDate=null, assignedUserId=null)` | Aufgabe `1` behaelt Projekt, Titel, Beschreibung, Faelligkeitsdatum und Zuweisung; nur der Status wird zu `DONE`. |
| `deleteTaskRemovesTaskAndItsComments` | `id=1` | Die Antwort enthaelt `message="Aufgabe wurde geloescht"`; ein anschliessender Abruf der Kommentare fuer Aufgabe `1` wirft `NotFoundException`. |
