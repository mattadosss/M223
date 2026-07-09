# Projektarbeit Migration und ACID-Beispiel

## ACID-Eigenschaft

Gewaehlte Eigenschaft: **Atomicity**.

Beispiel: Beim Erstellen einer Aufgabe wird zuerst der Datensatz in `task` gespeichert. Direkt danach soll ein Start-Kommentar angelegt werden. Faellt das System genau zwischen diesen Schritten aus, entsteht ohne Atomicity ein halber Zustand. Mit Atomicity wird die gesamte Transaktion zurueckgerollt.

Sequenzdiagramm: `docs/acid-atomicity-projectarbeit-sequence.puml`

## Migration

| Version | Zweck |
| --- | --- |
| `V1__create_projectarbeit_schema.sql` | Erstellt `app_user`, `project`, `project_member`, `task` und `comment` und fuegt Seed-Daten ein. |
| `V2__add_task_description.sql` | Fuegt `task.description` hinzu und befuellt bestehende Aufgaben mit Beschreibungen. |

Flyway wird ueber `quarkus.flyway.migrate-at-start=true` beim Start ausgefuehrt.

## Nachweis

Die Tests laufen gegen eine isolierte H2-Testdatenbank.

| Test | Nachweis |
| --- | --- |
| `ProjectarbeitMigrationTest.flywayAppliedProjectarbeitMigrations` | Prueft, dass beide Flyway-Migrationen erfolgreich angewendet wurden. |
| `ProjectarbeitMigrationTest.seededProjectsAreStillAvailableAfterMigration` | Prueft, dass die Seed-Projekte nach der Migration noch vorhanden sind. |
| `ProjectarbeitMigrationTest.migratedTasksHaveDescriptionColumnWithData` | Prueft, dass alle migrierten Aufgaben eine Beschreibung besitzen. |
