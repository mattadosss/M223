ALTER TABLE task ADD COLUMN description VARCHAR(500);

UPDATE task
SET description = CASE id
    WHEN 1 THEN 'Routen und JSON-Strukturen abstimmen'
    WHEN 2 THEN 'Modelle fuer Projekt, Aufgabe und Kommentar ergaenzen'
    WHEN 3 THEN 'Kurzen Ablauf fuer die Vorstellung schreiben'
    ELSE 'Migrierte Aufgabe ohne urspruengliche Beschreibung'
END
WHERE description IS NULL;
