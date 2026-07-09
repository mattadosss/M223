# Spick M223 Block 5-8

## Block 5: Authentifizierung und Autorisierung

**Begriffe**

```text
Authentisierung  = Benutzer beweist seine Identität, z.B. Passwort eingeben.
Authentifizierung = System prüft diesen Beweis, z.B. Passwort korrekt?
Autorisierung    = System prüft Berechtigung, z.B. darf User Projekt löschen?
```

**HTTP ist stateless**

Der Server erkennt mehrere Requests vom gleichen Client nicht automatisch wieder. Deshalb braucht es z.B. Session-Cookies oder Tokens. Bei APIs wird oft OAuth/JWT verwendet.

**OAuth/JWT Ablauf**

```text
1. Benutzer will Webapp/API benutzen.
2. Benutzer meldet sich beim Identity Server an.
3. Identity Server prüft Login.
4. Identity Server erstellt signiertes JWT mit Claims.
5. Client sendet JWT bei jedem API-Request im Header mit:
   Authorization: Bearer <token>
6. API prüft Signatur mit Public Key.
7. API liest Claims/Rollen und entscheidet, ob Zugriff erlaubt ist.
```

**JWT Sicherheit**

```text
Private Key: signiert Token, darf nie geteilt werden.
Public Key: prüft Signatur, darf der API bekannt sein.
JWT ist meistens signiert, aber nicht verschlüsselt.
Inhalt kann oft gelesen werden.
Nie Passwörter oder sensible Daten ins Token schreiben.
Sinnvolle Claims: userId, username/upn, Rollen, Ablaufzeit.
```

**Access Token vs. Refresh Token**

```text
Access Token:
  kurzer gültig, wird bei API-Requests verwendet.

Refresh Token:
  länger gültig, holt neue Access Tokens beim Identity Server.
  Dort kann erneut geprüft werden, ob der User noch Rechte hat.
```

**Quarkus JWT Konfiguration**

```properties
mp.jwt.verify.publickey.location=publicKey.pem
mp.jwt.verify.issuer=https://example.com/issuer
mp.jwt.token.header=Authorization
smallrye.jwt.sign.key.location=privateKey.pem
quarkus.smallrye-jwt.enabled=true
```

**Token erzeugen**

```java
@Path("/auth")
public class AuthService {
    @POST
    @Path("/token")
    @Produces(MediaType.TEXT_PLAIN)
    public Response generateToken() {
        String token = Jwt.upn("jdoe@quarkus.io")
                .issuer("https://example.com/issuer")
                .groups(Set.of("User", "Admin"))
                .sign();

        return Response.ok(token).build();
    }
}
```

**Geschützten Endpunkt schreiben**

```java
@Path("/secure")
public class SecureResource {
    @GET
    @RolesAllowed("Admin")
    public String onlyAdmin() {
        return "Nur Admins sehen das";
    }
}
```

**Login-Response Beispiel**

```java
public record LoginRequest(String username, String password) {}
public record LoginResponse(String message, String token, User user) {}
```

```java
@POST
@Path("/login")
public Response login(LoginRequest request) {
    LoginResponse response = service.login(request);
    return Response.ok(response).build();
}
```

## Block 6: Testing

**Ziel von Testing**

Software-Testing prüft, ob Software den Anforderungen entspricht. Es findet Fehler früh, erhöht Wartbarkeit und verhindert Regressionen bei späteren Änderungen.

**Testarten**

```text
Unit-Test:
  Testet eine kleine Einheit isoliert.
  Schnell, viele Tests.
  Beispiel: Service-Methode direkt testen.

Integrationstest:
  Testet Zusammenspiel mehrerer Komponenten.
  Beispiel: REST Resource + Service + JSON.

Systemtest:
  Testet komplette Anwendung unter realistischen Bedingungen.

End-to-End-Test:
  Simuliert einen echten Benutzerworkflow.
  Beispiel: Projekt erstellen -> Task erstellen -> Kommentar lesen.

Akzeptanztest:
  Prüft, ob Anforderungen aus Benutzersicht erfüllt sind.
```

**Testpyramide**

```text
Viele Unit-Tests       = schnell, billig, kleine Logik
Einige Integrationstests = Zusammenspiel prüfen
Wenige E2E-Tests       = langsam, aber realistisch
```

**Code Coverage**

Code Coverage zeigt, wie viel Code von Tests ausgeführt wird. Hohe Coverage ist gut, garantiert aber keine Fehlerfreiheit. Qualität der Testfälle ist wichtiger als eine Prozentzahl.

**TDD**

```text
Red: Test schreiben, der fehlschlägt.
Green: Nur so viel Code schreiben, dass Test besteht.
Refactor: Code verbessern, ohne Verhalten zu ändern.
```

**Mocks**

Mocks simulieren Abhängigkeiten. Damit testet man eine Komponente isoliert, ohne echte externe Systeme wie Datenbank, API oder Mailserver auszuführen.

**AAA Schema**

```text
Arrange = Testdaten und Umgebung vorbereiten
Act     = zu testende Methode ausführen
Assert  = Ergebnis prüfen
```

**Given-When-Then**

```text
Given = Ausgangslage
When  = Aktion
Then  = erwartetes Ergebnis
```

**Unit-Test Beispiel**

```java
class LearningPlatformServiceTest {
    private LearningPlatformService service;

    @BeforeEach
    void setUp() {
        service = new LearningPlatformService();
    }

    @Test
    void registerCreatesUserWithNextId() {
        // Arrange + Act
        User created = service.register(
                new UserRequest("lea", "lea@example.com"));

        // Assert
        assertEquals(3L, created.id());
        assertEquals("lea", created.username());
        assertEquals("lea@example.com", created.email());
    }
}
```

**Exception testen**

```java
@Test
void createTaskRejectsMissingProjectId() {
    BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> service.createTask(
                    new TaskRequest(null, "Titel", null, null, null, null)));

    assertEquals("projectId darf nicht leer sein", exception.getMessage());
}
```

**Quarkus REST-Test mit RestAssured**

```java
@QuarkusTest
class LearningPlatformResourceTest {
    @Test
    void createsProjectFromRequestBody() {
        given()
            .contentType("application/json")
            .body("""
                  {
                    "name": "Neue Lerngruppe",
                    "description": "Gemeinsam lernen",
                    "ownerId": 1,
                    "memberIds": [1, 2]
                  }
                  """)
        .when()
            .post("/projects")
        .then()
            .statusCode(201)
            .body("name", is("Neue Lerngruppe"))
            .body("ownerId", is(1));
    }
}
```

**E2E-Test Idee**

```java
@Test
void createProjectTaskAndCommentWorkflow() {
    Integer projectId = given().contentType("application/json")
            .body("{\"name\":\"E2E\",\"ownerId\":1,\"memberIds\":[1]}")
            .when().post("/projects")
            .then().statusCode(201)
            .extract().path("id");

    Integer taskId = given().contentType("application/json")
            .body("""
                  {
                    "projectId": %d,
                    "title": "Task",
                    "status": "OPEN"
                  }
                  """.formatted(projectId))
            .when().post("/tasks")
            .then().statusCode(201)
            .extract().path("id");

    given().when().get("/projects/{id}/tasks", projectId)
            .then().statusCode(200)
            .body("size()", greaterThanOrEqualTo(1));
}
```

## Block 7: Datenhaltung, Transaktionen, ACID, Migration

**Warum Datenbank statt Datei?**

Mehrbenutzersysteme haben gleichzeitige Zugriffe. Datenbanken bieten strukturierte Daten, Abfragen, Beziehungen, Transaktionen, Konsistenz und Schutz vor kaputten Zwischenzuständen.

**Transaktion**

Eine Transaktion fasst mehrere Lese-/Schreiboperationen zu einer logischen Einheit zusammen.

Beispiel:

```text
Projekt löschen:
1. Projekt löschen
2. alle Tasks des Projekts löschen
3. alle Kommentare dieser Tasks löschen

Wenn Schritt 2 fehlschlägt, darf Schritt 1 nicht dauerhaft gespeichert bleiben.
```

**ACID**

```text
Atomicity / Atomarität:
  Alles oder nichts. Entweder alle Operationen werden ausgeführt oder keine.

Consistency / Konsistenz:
  Datenbank ist nach der Transaktion wieder gültig.
  Beispiel: Kein Task zeigt auf ein nicht existierendes Projekt.

Isolation:
  Parallele Transaktionen sehen keine ungültigen Zwischenstände.
  Beispiel: User B sieht nicht den halb gelöschten Zustand von User A.

Durability / Dauerhaftigkeit:
  Nach Commit sind Daten dauerhaft gespeichert, auch bei Absturz.
```

**Datenbankmigration**

Während Entwicklung kann man DB oft löschen und neu erstellen. In Produktion geht das nicht, weil echte Daten erhalten bleiben müssen. Migrationen ändern das Schema versioniert.

**Flyway**

```text
V1__create_projectarbeit_schema.sql
V2__add_task_description.sql

V1, V2 = Reihenfolge/Version
Text nach __ = Beschreibung
Flyway merkt sich ausgeführte Migrationen in flyway_schema_history
```

**Flyway aktivieren**

```properties
quarkus.flyway.migrate-at-start=true
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/app
quarkus.datasource.username=app
quarkus.datasource.password=app
```

**Migration Beispiel: Tabelle erstellen**

```sql
CREATE TABLE project (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    owner_id BIGINT NOT NULL,
    CONSTRAINT fk_project_owner
        FOREIGN KEY (owner_id) REFERENCES app_user(id)
);
```

**Migration Beispiel: Spalte hinzufügen**

```sql
ALTER TABLE task ADD COLUMN description VARCHAR(500);

UPDATE task
SET description = 'Migrierte Aufgabe ohne ursprüngliche Beschreibung'
WHERE description IS NULL;
```

**Beziehungen**

```text
1:n = ein Datensatz hat viele andere
  project 1:n task

n:m = viele zu viele, braucht Zwischentabelle
  app_user n:m project über project_member

Foreign Key = Verweis auf Primärschlüssel anderer Tabelle
Primary Key = eindeutige ID einer Tabelle
```

**Migrationstest**

```java
@QuarkusTest
class ProjectarbeitMigrationTest {
    @Inject
    DataSource dataSource;

    @Test
    void flywayAppliedMigrations() throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM "flyway_schema_history"
            WHERE "success" = TRUE AND "version" IN ('1', '2')
            """;

        assertEquals(2, queryInt(sql));
    }
}
```

**Typischer produktiver Service mit Transaktion**

```java
@ApplicationScoped
public class ProjectService {
    @Inject
    EntityManager em;

    @Transactional
    public Project createProject(ProjectRequest request) {
        Project project = new Project();
        project.name = request.name();
        project.description = request.description();
        em.persist(project);
        return project;
    }
}
```

## Block 8: Frontend

**Frontend Zweck**

Das Frontend ist die Benutzeroberfläche. Es zeigt Daten an, nimmt Eingaben entgegen und ruft die Backend-API auf.

```text
index.html = Struktur
styles.css = Darstellung
app.js = Verhalten, API-Aufrufe, Rendering
```

**State im Frontend**

```js
const state = {
  userId: 1,
  projects: [],
  selectedProject: null,
  tasks: [],
  selectedTask: null,
  comments: []
};
```

Der State speichert den aktuellen Zustand der Oberfläche. Render-Funktionen bauen daraus HTML.

**API-Funktion mit fetch**

```js
async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }

  return response.json();
}
```

**GET Request**

```js
async function loadProjects() {
  state.projects = await api(`/projects?userId=${state.userId}`);
  renderProjects();
}
```

**POST Request**

```js
async function createTask(form) {
  const created = await api("/tasks", {
    method: "POST",
    body: JSON.stringify({
      projectId: state.selectedProject.id,
      title: form.get("title"),
      description: form.get("description"),
      status: "OPEN"
    })
  });

  state.selectedTask = created;
}
```

**Formular abfangen**

```js
el.taskForm.addEventListener("submit", async event => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  await createTask(form);
  await loadTasks();
});
```

**HTML sicher ausgeben**

```js
function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
```

## REST / Quarkus Kurzspick

```java
@Path("/projects")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProjectResource {
    @Inject
    ProjectService service;

    @GET
    public List<Project> getProjects(@QueryParam("userId") Long userId) {
        return service.getProjects(userId);
    }

    @POST
    public Response createProject(ProjectRequest request) {
        Project project = service.createProject(request);
        return Response.created(URI.create("/projects/" + project.id()))
                .entity(project)
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getProject(@PathParam("id") long id) {
        return Response.ok(service.getProject(id)).build();
    }
}
```

```text
@Path              URL/Pfad
@GET/@POST/...     HTTP-Methode
@Consumes          welches Format angenommen wird
@Produces          welches Format zurückgegeben wird
@Inject            Dependency Injection
@PathParam         Wert aus URL-Pfad
@QueryParam        Wert aus ?query
Response.ok()      HTTP 200
Response.created() HTTP 201
```

## Mini-Merksätze

```text
Resource = HTTP-Schicht, übersetzt Requests in Service-Aufrufe.
Service = Businesslogik, Validierung, Fehler, Transaktionen.
DTO = API-Datenform.
Model/Entity = fachliches Datenobjekt.
JWT = signiertes Token für stateless Auth.
Tests = Anforderungen automatisch prüfen.
Migration = Datenbankschema ändern, ohne Daten zu verlieren.
Frontend = ruft API auf und rendert State.
```
