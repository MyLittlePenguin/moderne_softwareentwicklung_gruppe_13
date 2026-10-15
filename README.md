# moderne_softwareentwicklung_gruppe_13

## Was ist Git und warum sollte es verwendet werden?

Git speichert jeden Stand eines Projekts als *Commit*: eine vollständige Momentaufnahme, die über einen Hash ihres Inhalts eindeutig identifiziert wird und so eine unveränderliche Historie bildet. Textdateien vergleicht Git zeilenweise. Änderungen erscheinen als *Hunks*, also Blöcke geänderter Zeilen, die man mit `git add -p` einzeln in Commits übernehmen kann und die Git beim Mergen automatisch zusammenführt, solange sie nicht dieselben Zeilen betreffen.

Jeder Klon enthält das komplette Repository, gearbeitet wird lokal und mit `git push` und `git pull` synchronisiert, meist über SSH mit einem hinterlegten Schlüssel. Jeder Commit trägt die E-Mail-Adresse seines Autors (`git config user.email`). Ursprünglich wurden Änderungen sogar per E-Mail als Patches ausgetauscht (`git format-patch`, `git send-email`, `git am`), was beim Linux-Kernel bis heute üblich ist.

Durch die unveränderliche Historie kann man sich bei der Verwendung von git darauf verlassen, dass jede Änderung auch wieder Rückgängig gemacht werden kann. `git log` z.B. zeigt die Historie des aktuellen Verzeichnisses an. Wenn also eine Änderung einen Fehler mitbringt, kann man mit dem Befehl `git revert HEAD` die letzte Änderung rückgängig machen. Für das Verteilte Arbeiten bringt es den Vorteil, dass git ein Hash-Graph ist und jeder Commit einen Vorgänger-Commit hat (ähnlich einer Blockchain). Wenn die Änderungen eines Kollegen oder einer Kollegin in den eigenen übertragen werden sollen und die Änderungen sich nicht gegenseitig betreffen, der Hash-Graph mit einem `git rebase` zusammengeführt werden.

## Grundlegende Git-Befehle

### `git init`

Mit diesem Befehl wird ein neues Git-Repository initialisiert. Dieses kann dann
entweder mit einem bereits bestehenden Remote-Repository verbunden oder von
anderen geklont werden.

```bash
git init
```

### `git add`

Mit diesem Befehl können Änderungen an bestehenden Dateien oder auch neue
Dateien bzw. ganze Verzeichnisse zur Versionsverfolgung durch das Git-Repository
hinzugefügt werden. Damit werden sie allerdings noch nicht als Commit in einen
Branch geschrieben, sondern erst einmal nur für den nächsten Commit vorgemerkt
(Staging Area).

```bash
git add <path1> [path2 [...]]

# Beispiel zum Hinzufügen aller Änderungen und neuer Dateien im aktuellen Verzeichnis
git add .
```

### `git commit`

Mit einem Commit werden die für den aktuellen Stand vorgemerkten Änderungen
tatsächlich als ein neuer Zustand (ein Snapshot) in den aktuellen Branch
geschrieben. Dies findet erst einmal nur im lokalen Repository statt.

```bash
# Beispiel zum Schreiben aller vorgemerkten Änderungen in einen Branch mit Nachricht
git commit -m "Commit Message"

# Beispiel zum direkten Vormerken und Schreiben aller *bereits bekannten* Änderungen in einem Schritt.
# Hinweis: Neue, noch nie hinzugefügte Dateien werden hierbei ignoriert!
git commit -am "Commit Message"
```

### `git push`

Mit diesem Befehl werden die lokalen Commits an ein Remote-Repository (z. B. auf
GitHub) übertragen, damit andere Teamkolleg:innen darauf zugreifen können.

```bash
# Beispiel zum Hochladen des aktuellen Branches zum Standard-Remote (origin)
git push origin <branch-name>
```

## Branches und ihre Nutzung, Umgang mit Merge-Konflikten

### Was sind Branches?

Branches ermöglichen es, parallel am Code zu arbeiten, ohne den stabilen Code im
Hauptbranch (meist `master` oder `main`) zu gefährden.

### Wichtige Befehle für Branches

- **Branches anzeigen:**
  Listet alle lokalen Branches auf. Der aktuell aktive Branch ist mit einem Sternchen (`*`) markiert.
  ```bash
  git branch
  ```
- **Neuen Branch erstellen:**
  ```bash
  git branch <branch-name>
  ```
- **Zu einem anderen Branch wechseln:**
  *(Empfohlen: Die moderne Syntax mit `git switch`, alternativ geht auch `git checkout`)*
  ```bash
  git switch <branch-name>
  ```
- **Branch erstellen und *sofort* dorthin wechseln:**
  ```bash
  git switch -c <branch-name>
  ```

### Branches zusammenführen (`git merge`)

Wenn du deine Arbeit auf einem Feature-Branch abgeschlossen hast und sie in den
Hauptbranch integrieren möchtest, nutzt du `git merge`.

**Ablauf:**

1. Wechsle in den Zielbranch (meistens `main`):
```bash
git switch main
```
2. Hole dir eventuelle Updates vom Remote-Server:
```bash
git pull
```
3. Führe den Feature-Branch in den aktuellen Branch zusammen:
```bash
git merge <feature-branch-name>
```

### Umgang mit Merge-Konflikten

#### Was ist ein Merge-Konflikt?

Ein Konflikt entsteht, wenn Git nicht automatisch entscheiden kann, welche
Änderungen übernommen werden sollen, weil dieselbe Datei in
derselben Zeile im aktuellen Branch und im zusammenzuführenden Branch
unterschiedlich bearbeitet wurde.

#### Schritte zur Lösung eines Merge-Konflikts:

1. **Konflikt erkennen:**
Beim Versuch des Mergens bricht Git ab und markiert die betroffenen Dateien.
Wenn du `git status` eingibst, siehst du die Konfliktdateien.

2. **Datei öffnen und bereinigen:**
Öffne die betroffene Datei in deinem Editor (z. B. VS Code oder NVIM). Git fügt dort
Konfliktmarker ein, die wie folgt aussehen:

```text
<<<<<<< HEAD
Dein Code im aktuellen Branch
=======
Der Code aus dem Branch, den du mergen wolltest
>>>>>>> <feature-branch-name>
```

3. **Entscheidung treffen:** Lösche die Marker (`<<<<<<<`, `=======`, `>>>>>>>`)
und passe den Code so an, wie er am Ende aussehen soll (entweder Variante A,
Variante B oder eine Kombination aus beidem).

4. **Änderungen vormerken:**
Nachdem du den Konflikt bereinigt hast, fügst du die Datei wieder zum Staging hinzu:
```bash
git add <dateiname>
```

5. **Merge abschließen:**
Schließe den Vorgang mit einem Commit ab:
```bash
git commit -m "Merge Konflikt in <dateiname> behoben"
```

**Hinweis:** Hier wurde der manuelle Weg über die CLI und einen Text-Editor
beschrieben. Wenn man eine IDE wie IntelliJ benutzt, bietet diese eine grafisch
aufbereitete Bedienoberfläche, um die gleichen Schritte durchzuführen. Viele
empfinden diese Oberfläche als übersichtlicher und hilfreich.

### Paralleles Arbeiten mit Git Worktrees

#### Was ist ein Git Worktree?

Normalerweise hast du für ein Repository immer nur ein aktives
Arbeitsverzeichnis. Wenn du an einem anderen Branch arbeiten
willst, musst du entweder deine aktuellen Änderungen committen, per `git stash`
verstecken oder den Branch wechseln. Ein Worktree erlaubt es, mehrere
Branches gleichzeitig in verschiedenen Ordnern auf deiner Festplatte
auszuchecken.

#### Wichtige Worktree-Befehle:

- **Aktive Worktrees anzeigen:**
Listet alle verknüpften Arbeitsverzeichnisse und die dort ausgecheckten Branches auf.
```bash
git worktree list
```

- **Neuen Worktree erstellen:**
Erstellt einen neuen Ordner auf deiner Festplatte und checkt dort direkt einen bestehenden (oder mit `-b` einen neuen) Branch aus.
```bash
# Beispiel: Erstellt einen Ordner "../hotfix" und checkt dort den Branch "hotfix-branch" aus
git worktree add ../hotfix hotfix-branch

# Beispiel inklusive Erstellung eines neuen Branches
git worktree add -b neuer-branch ../neuer-ordner
```

- **Worktree entfernen:**
Wenn du mit der Arbeit im Worktree fertig bist, kannst du ihn wieder löschen (die Commits bleiben natürlich erhalten).
```bash
git worktree remove ../hotfix
```

## Git mit IntelliJ/PyCharm benutzen: Local Repository und Remote Repository

## Nützliche Git-Tools und Plattformen (z. B. GitHub)

## Arbeitsanteile

**Was ist Git und warum sollte es verwendet werden?:** Timo Kramer, ()\
**Grundlegende Git-Befehle:** Ronny Krone, Ostfalia (70462294)\
**Branches und ihre Nutzung, Umgang mit Merge-Konflikten:** Ronny Krone\
**Git mit IntelliJ/PyCharm benutzen:** Hassan Mahmoud, ()\
**Nützliche Git-Tools und Plattformen:** Hassan Mahmoud\
