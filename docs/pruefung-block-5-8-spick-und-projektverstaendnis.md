# M223 Pruefung Block 5-8: Spick + Projektverstaendnis

## 1. Projekt in einem Satz

Meine App ist eine kleine Lern-/Projektplattform: Benutzer koennen Projekte sehen/anlegen, Mitglieder hinzufuegen, Aufgaben verwalten und Kommentare zu Aufgaben schreiben. Das Frontend ruft REST-Endpunkte auf, die Resource-Klasse nimmt HTTP-Requests entgegen, der Service verarbeitet die Businesslogik, und Model/DTO-Klassen beschreiben die Daten.

## 2. Architektur: Was ist wofuer da?

```text
Browser / Frontend
  app.js
  - zeigt Projekte, Tasks, Kommentare
  - sendet fetch()-Requests an REST API
        |
        v
REST Resource / Controller
  LearningPlatformResource
  - definiert URLs, HTTP-Methoden, Statuscodes
  - liest PathParam, QueryParam und JSON-Body
  - ruft den Service auf
        |
        v
Service / Businesslogik
  LearningPlatformService
  - validiert Eingaben
  - entscheidet, was fachlich passieren soll
  - erstellt, aktualisiert, loescht und filtert Daten
  - wirft BadRequestException / NotFoundException
        |
        v
Model / Daten
  User, Project, Task, Comment
  - fachliche Objekte der App

DTOs
  UserRequest, ProjectRequest, TaskRequest, ...
  - JSON-Form der API-Requests/Responses
  - verhindern, dass das API direkt an interne Datenstrukturen gekoppelt ist
```

Wichtig fuer die Pruefung: In einer typischen Enterprise-App wuerde der Service ueber ORM/JPA mit der Datenbank arbeiten. In meinem aktuellen Code speichert `LearningPlatformService` die Daten aber in `Map<Long, ...>` im Speicher. Die SQL-Migrationen und Flyway-Tests zeigen das Datenbankkonzept, sind aber nicht direkt mit dem Service verdrahtet.

## 3. MVC / Schichten in meinem Projekt

```text
View:
  index.html, styles.css, app.js

Controller / Resource:
  LearningPlatformResource.java
  AuthService.java

Model:
  User.java, Project.java, Task.java, Comment.java

Service:
  LearningPlatformService.java
```

Sinn der Aufteilung: Jede Klasse hat eine klare Verantwortung. Das Frontend muss keine Businesslogik kennen, die Resource muss keine Datenregeln selber implementieren, und der Service ist testbar, weil die Logik an einem Ort liegt.

## 4. REST-Endpunkte in meinem Projekt

| Zweck | HTTP | Pfad | Methode im Resource |
|---|---:|---|---|
| Benutzer registrieren | POST | `/register` | `register()` |
| Login | POST | `/login` | `login()` |
| Benutzer lesen | GET | `/users/{id}` | `getUser()` |
| Projekte eines Users | GET | `/projects?userId=1` | `getProjects()` |
| Projekt erstellen | POST | `/projects` | `createProject()` |
| Projekt lesen | GET | `/projects/{id}` | `getProject()` |
| Projekt aendern | PUT | `/projects/{id}` | `updateProject()` |
| Projekt loeschen | DELETE | `/projects/{id}` | `deleteProject()` |
| Mitglied hinzufuegen | POST | `/projects/{id}/members` | `addProjectMember()` |
| Tasks eines Projekts | GET | `/projects/{id}/tasks` | `getProjectTasks()` |
| Task erstellen | POST | `/tasks` | `createTask()` |
| Task aendern | PUT | `/tasks/{id}` | `updateTask()` |
| Task loeschen | DELETE | `/tasks/{id}` | `deleteTask()` |
| Kommentare lesen | GET | `/tasks/{id}/comments` | `getTaskComments()` |
| Kommentar erstellen | POST | `/tasks/{id}/comments` | `createComment()` |

CRUD-Mapping:

```text
Create = POST
Read   = GET
Update = PUT/PATCH
Delete = DELETE
```

## 5. Beispiel-Ablauf: Task erstellen

```text
User fuellt Formular im Browser aus
  -> app.js liest FormData
  -> fetch("/tasks", { method: "POST", body: JSON.stringify(...) })
  -> LearningPlatformResource.createTask(TaskRequest request)
  -> LearningPlatformService.createTask(request)
       - prueft: Request vorhanden?
       - prueft: title nicht leer?
       - prueft: projectId vorhanden?
       - prueft: Projekt existiert?
       - erstellt Task mit neuer ID
       - speichert Task in Map
  -> Resource gibt HTTP 201 Created + JSON Task zurueck
  -> app.js rendert Task im Board
```

Merksatz: Die Resource uebersetzt HTTP in Java-Aufrufe. Der Service entscheidet fachlich, ob und wie etwas gespeichert wird.

## 6. Block 5: Authentifizierung und Autorisierung

Begriffe:

```text
Authentisierung  = Ich beweise, wer ich bin. Beispiel: Passwort eingeben.
Authentifizierung = Das System prueft diesen Beweis. Beispiel: Passwort stimmt?
Autorisierung    = Das System entscheidet, was ich darf. Beispiel: Darf ich Projekt X sehen?
```

HTTP ist stateless: Der Server merkt sich zwischen zwei Requests nicht automatisch, wer der Client ist. Deshalb braucht man z.B. Token oder Cookies.

JWT/OAuth-Idee:

```text
1. User loggt sich beim Identity Server ein.
2. Identity Server prueft Login.
3. Identity Server erstellt signiertes JWT mit Claims.
4. Client sendet JWT bei API-Requests im Authorization-Header mit.
5. API prueft Signatur mit Public Key.
6. API liest Claims/Rollen und entscheidet Zugriff.
```

JWT-Sicherheit:

```text
Private Key: signiert Token, darf nie geteilt werden.
Public Key: prueft Signatur, darf die API kennen.
Token ist meistens lesbar, nur signiert, nicht verschluesselt.
Keine Passwoerter oder sensiblen Daten ins Token schreiben.
```

In meinem Projekt:

```text
AuthService.generateToken()
  - erstellt ein JWT mit upn, issuer und groups
  - signiert es mit privateKey.pem

application.properties
  - mp.jwt.verify.publickey.location=publicKey.pem
  - mp.jwt.verify.issuer=https://example.com/issuer
  - smallrye.jwt.sign.key.location=privateKey.pem
```

Wichtig: Der `/login`-Endpunkt im `LearningPlatformService` gibt aktuell nur ein Demo-Token wie `demo-token-maria` zurueck. Der echte JWT-Demo-Endpunkt ist `/auth/token`.

## 7. Block 6: Testing

Warum Tests?

```text
Tests pruefen, ob Software Anforderungen erfuellt.
Sie finden Fehler frueh.
Sie schuetzen vor Regressionen bei spaeteren Aenderungen.
```

Testarten:

```text
Unit-Test:
  Testet eine kleine Einheit isoliert.
  Beispiel: LearningPlatformServiceTest testet Service-Methoden direkt.

Integrationstest:
  Testet Zusammenspiel mehrerer Komponenten.
  Beispiel: API-Request registriert User, danach GET /users/{id}.

End-to-End-Test:
  Testet einen ganzen Benutzer-Workflow.
  Beispiel: Projekt erstellen -> Task erstellen -> Kommentar erstellen -> wieder lesen.

Migrationstest:
  Testet, ob Flyway-Migrationen laufen und Daten/Spalten vorhanden sind.
```

Testpyramide:

```text
Viele Unit-Tests       = schnell, klein, billig
Einige Integrationstests = Zusammenspiel pruefen
Wenige E2E-Tests       = langsam, aber realistisch
```

AAA-Schema:

```text
Arrange = Testdaten vorbereiten
Act     = Methode/Request ausfuehren
Assert  = Ergebnis pruefen
```

Given-When-Then:

```text
Given = Ausgangslage
When  = Aktion
Then  = erwartetes Resultat
```

Beispiel aus meinem Projekt:

```java
// Given
LearningPlatformService service = new LearningPlatformService();

// When
User created = service.register(new UserRequest("lea", "lea@example.com"));

// Then
assertEquals(3L, created.id());
assertEquals("lea", created.username());
```

## 8. Block 7: Datenhaltung, Transaktionen, ACID, Migrationen

Transaktion:

```text
Eine Transaktion fasst mehrere Datenoperationen zu einer logischen Einheit zusammen.
Beispiel: Projekt loeschen + alle Tasks loeschen + alle Kommentare loeschen.
```

ACID:

```text
Atomicity:
  Alles oder nichts. Wenn ein Teil fehlschlaegt, wird alles zurueckgerollt.

Consistency:
  Nach der Transaktion ist die Datenbank in einem gueltigen Zustand.
  Beispiel: Kein Task darf auf ein nicht existierendes Projekt zeigen.

Isolation:
  Gleichzeitige Transaktionen stoeren sich nicht gegenseitig.
  Beispiel: Zwei User bearbeiten Daten, ohne halbfertige Zwischenstaende zu sehen.

Durability:
  Nach Commit bleiben Daten dauerhaft gespeichert, auch nach Absturz.
```

Migration:

```text
Problem:
  In Produktion darf man die DB nicht einfach loeschen und neu erstellen.

Loesung:
  Schema-Aenderungen werden versioniert migriert.

In meinem Projekt:
  V1__create_projectarbeit_schema.sql
    - erstellt Tabellen app_user, project, project_member, task, comment
    - fuegt Testdaten ein

  V2__add_task_description.sql
    - fuegt task.description hinzu
    - befuellt bestehende Tasks mit Beschreibung

  quarkus.flyway.migrate-at-start=true
    - Flyway fuehrt Migrationen beim Start aus
```

Beziehungen im Datenmodell:

```text
app_user 1 --- n project       ueber owner_id
app_user n --- m project       ueber project_member
project  1 --- n task          ueber task.project_id
task     1 --- n comment       ueber comment.task_id
app_user 1 --- n comment       ueber comment.user_id
app_user 1 --- n task optional ueber task.assigned_user_id
```

## 9. Block 8: Frontend

Frontend-Aufgabe:

```text
index.html  = Struktur der Seite
styles.css  = Aussehen
app.js      = Verhalten, Zustand, API-Calls, Rendering
```

Wichtige Frontend-Idee:

```text
state speichert den aktuellen UI-Zustand:
  userId
  projects
  selectedProject
  tasks
  selectedTask
  comments

render...()-Funktionen bauen die sichtbare Seite aus state neu auf.
api() kapselt fetch(), JSON und Fehlerbehandlung.
```

Beispiel:

```text
loadProjects()
  -> GET /projects?userId=...
  -> speichert Antwort in state.projects
  -> waehlt erstes Projekt aus
  -> renderProjects()
  -> loadTasks()
```

## 10. Typische Pruefungsfragen + kurze Antworten

Was ist der Unterschied zwischen DTO und Model?

```text
Model = fachliches Objekt der App, z.B. Task.
DTO = Datenform fuer API-Eingabe/Ausgabe, z.B. TaskRequest.
DTOs schuetzen die interne Struktur und erlauben andere JSON-Formen als das Datenmodell.
```

Warum Dependency Injection?

```text
Objekte werden nicht selber mit new erstellt, sondern vom Framework bereitgestellt.
Vorteile: weniger Kopplung, bessere Testbarkeit, zentrale Verwaltung der Lebensdauer.
In meinem Projekt injiziert LearningPlatformResource den LearningPlatformService mit @Inject.
```

Was macht `@Path`?

```text
@Path definiert, unter welcher URL eine Resource oder Methode erreichbar ist.
Beispiel: @Path("/projects/{id}") + @GET ergibt GET /projects/1.
```

Was macht `@ApplicationScoped`?

```text
Die Klasse wird vom DI-Container verwaltet und lebt als eine gemeinsame Instanz fuer die App.
In meinem Projekt ist LearningPlatformService @ApplicationScoped.
```

Was ist der Unterschied zwischen `@PathParam` und `@QueryParam`?

```text
@PathParam liest Werte aus dem Pfad: /projects/5 -> id=5
@QueryParam liest Werte aus der Query: /projects?userId=1 -> userId=1
```

Was ist der Unterschied zwischen 200 und 201?

```text
200 OK = Request erfolgreich.
201 Created = neue Ressource wurde erstellt.
```

Warum wirft der Service Exceptions?

```text
Der Service erkennt fachliche Fehler.
BadRequestException = Eingabe ist ungueltig.
NotFoundException = gesuchte Ressource existiert nicht.
Quarkus uebersetzt diese Exceptions in passende HTTP-Fehlerantworten.
```

Warum ist mein aktueller In-Memory-Service nicht produktionsreif?

```text
Daten gehen beim Neustart verloren.
Mehrere Serverinstanzen haetten unterschiedliche Daten.
Maps ersetzen keine echte Datenbanktransaktion.
Fuer Produktion waere JPA/ORM + Datenbank + @Transactional sinnvoll.
```

## 11. Mini-Spick fuer eine A4-Seite

```text
M223 Block 5-8

Architektur:
Frontend -> Resource/Controller -> Service -> Model/DB
Resource: HTTP, URLs, Statuscodes, JSON
Service: Businesslogik, Validierung, Fehler
Model: fachliche Datenobjekte
DTO: API-Datenobjekte fuer Requests/Responses

REST:
GET lesen, POST erstellen, PUT aktualisieren, DELETE loeschen
@Path URL, @GET/@POST Methode, @PathParam Pfadwert, @QueryParam ?wert
200 OK, 201 Created, 400 Bad Request, 404 Not Found

DI:
@ApplicationScoped macht Service zum Bean
@Inject gibt Bean in Resource
Vorteil: weniger Kopplung, testbarer

Auth:
Authentisierung = Identitaet beweisen
Authentifizierung = Beweis pruefen
Autorisierung = Rechte pruefen
HTTP stateless -> Token/Cookie noetig
JWT: signiertes Token mit Claims, im Authorization: Bearer ...
Private Key signiert, Public Key validiert
Keine Passwoerter ins JWT

Testing:
Unit = kleine Einheit isoliert
Integration = Komponenten zusammen
E2E = kompletter Workflow
Testpyramide: viele Unit, weniger Integration, wenige E2E
AAA: Arrange, Act, Assert
GWT: Given, When, Then
Coverage wichtig, aber Testqualitaet wichtiger

Daten/ACID:
Transaktion = mehrere DB-Operationen als Einheit
A Atomicity: alles oder nichts
C Consistency: gueltiger Zustand
I Isolation: parallele TX getrennt
D Durability: nach Commit dauerhaft
Migration: DB-Schema versioniert aendern, nicht produktive DB loeschen
Flyway: V1__, V2__, quarkus.flyway.migrate-at-start=true

Frontend:
state haelt UI-Zustand
fetch ruft API
render-Funktionen bauen HTML aus state
escapeHtml gegen HTML-Injection
```

## 12. Wie ich mein Projekt muendlich erklaeren wuerde

Meine App ist in Schichten aufgebaut. Das Frontend zeigt eine Projekt- und Aufgabenuebersicht. Wenn der Benutzer etwas macht, zum Beispiel eine Aufgabe erstellt, sendet `app.js` einen JSON-Request an die REST-API. Die Klasse `LearningPlatformResource` definiert die Endpunkte und nimmt den Request entgegen. Sie verarbeitet aber nicht selber die Businesslogik, sondern ruft `LearningPlatformService` auf. Der Service validiert die Eingaben, erstellt oder aendert die fachlichen Objekte und gibt das Resultat zurueck. Die Resource baut daraus eine HTTP-Antwort, zum Beispiel `201 Created`.

Die Models `User`, `Project`, `Task` und `Comment` beschreiben die Hauptdaten. Die DTOs wie `TaskRequest` beschreiben, welche Daten das Frontend beim Erstellen oder Aendern sendet. Fuer Authentifizierung gibt es eine JWT-Konfiguration und einen Demo-Endpunkt `/auth/token`, der ein signiertes Token erzeugt. Fuer Datenbankmigrationen gibt es Flyway-Skripte, die Tabellen erstellen und spaeter die Spalte `description` zu `task` hinzufuegen. Die Tests pruefen verschiedene Ebenen: Service-Logik direkt, API-Endpunkte mit RestAssured, einen ganzen Workflow und die Flyway-Migration.

Der wichtigste Punkt: Die Struktur trennt Anzeige, HTTP-Schnittstelle, Businesslogik und Daten. Dadurch wird die App verstaendlicher, testbarer und einfacher erweiterbar.
