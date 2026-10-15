# moderne_softwareentwicklung_gruppe_13

## Was ist Git

Git ist ein verteiltes Versionskontrollsystem initiiert von Linux Torvalds. Git speichert jeden Stand eines Projekts als *Commit*: eine vollständige Momentaufnahme, die über einen Hash ihres Inhalts eindeutig identifiziert wird und so eine nachvollziehbare Historie bildet. **Git ist nicht GitHub**, git ist das lokale Programm und GitHub oder auch GitLab sind die Anbieter von zentralen Repositories.

Ein repository ist das Arbeitsverzeichnis mit dem Index der im versteckten Verzeichnis `.git` liegt. Der Index ist ein gerichteter azyklischer Graph (*Directed Acyclic Graph*, DAG) in dem die Commits mit ihren Hashwerten festgehalten werden. Ein Commit ist ein Datensatz an Änderungen, welcher im `.git`-Verzeichnis abgelegt wird. Der Index hält eine Referenz auf jeden Commit und bildet den DAG. `Directed` heißt, dass jeder commit auf seine Eltern verweist, aber nichts von seinen Nachfolgern weiß. `Azyklisch` bedeutet, dass die commits nie einen Kreis bilden können und `Graph` bedeutet dass ein commit nur einen Vorgänger aber mehrere Nachfolger haben kann.

Die Stärke von git liegt in der Arbeit mit Textdateien. Diese vergleicht git zeilenweise, Änderungen erscheinen als `hunks`, also Blöcke geänderter Zeilen, welche man in commits übernehmen kann. Diese commits können in einem eigenen branch gesammelt werden und mit einem merge wieder in einen Ursprungsbranch eingefügt werden.

Ein repository kann lokal genutzt werden oder aber man benutzt das repository verteilt. Dabei kann ein zentrales repository auf einem Server wie `GitHub` genutzt werden. Ein repository kann dann von jedem geklont werden. Jeder Klon enthält das komplette Repository, gearbeitet wird im Klon lokal und die commits werden anschließend synchronisiert. Das geschieht heutzutage meist über SSH oder HTTPS.

## Warum sollte es verwendet werden

- **Nachvollziehbarkeit und Integrität:** Jede Änderung ist mit Autor, Zeitpunkt und Begründung festgehalten und durch die Hash-Verkettung gegen unbemerkte Manipulation geschützt. Commits und Tags lassen sich zusätzlich mit GPG oder SSH signieren.
- **Änderungen sind umkehrbar:** `git revert <commit>` erzeugt einen neuen Commit, der die Änderungen eines alten umkehrt, ohne die Historie umzuschreiben. Selbst Commits, auf die kein Branch mehr zeigt (z. B. nach einem `reset` oder `rebase`), bleiben über `git reflog` noch eine Zeit lang auffindbar, bevor die Garbage Collection sie entfernt. Nicht geschützt ist dagegen, was nie committet wurde: Änderungen im Arbeitsverzeichnis gehen z. B. durch `git reset --hard` endgültig verloren.
- **Verteiltes Arbeiten:** Jeder Klon enthält das vollständige Repository samt Historie. Man kann offline arbeiten, und jeder Klon ist ein Backup. Da Objekte über ihren Inhalt adressiert werden, haben sie in jedem Klon denselben Hash. Synchronisieren (`git fetch`, `git push`) bedeutet daher nur, die fehlenden Objekte zu übertragen und Refs zu aktualisieren. Anders als bei zentralen Systemen wie SVN braucht man dafür keinen Server als einzige Quelle der Wahrheit; Plattformen wie GitHub sind lediglich ein vereinbarter gemeinsamer Remote.
- **Paralleles Arbeiten:** Weil Branches nur Zeiger sind und Merges über die Merge-Base im DAG funktionieren, sind Feature-Branches, Code-Reviews per Pull Request und Workflows wie Trunk-based Development oder Git Flow einfach umzusetzen.

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
