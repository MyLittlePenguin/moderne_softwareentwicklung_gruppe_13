# moderne_softwareentwicklung_gruppe_13

## Was ist Git und warum sollte es verwendet werden?

Git speichert jeden Stand eines Projekts als *Commit*: eine vollständige Momentaufnahme, die über einen Hash ihres Inhalts eindeutig identifiziert wird und so eine unveränderliche Historie bildet. Textdateien vergleicht Git zeilenweise. Änderungen erscheinen als *Hunks*, also Blöcke geänderter Zeilen, die man mit `git add -p` einzeln in Commits übernehmen kann und die Git beim Mergen automatisch zusammenführt, solange sie nicht dieselben Zeilen betreffen.

Jeder Klon enthält das komplette Repository, gearbeitet wird lokal und mit `git push` und `git pull` synchronisiert, meist über SSH mit einem hinterlegten Schlüssel. Jeder Commit trägt die E-Mail-Adresse seines Autors (`git config user.email`). Ursprünglich wurden Änderungen sogar per E-Mail als Patches ausgetauscht (`git format-patch`, `git send-email`, `git am`), was beim Linux-Kernel bis heute üblich ist.

Durch die unveränderliche Historie kann man sich bei der Verwendung von git darauf verlassen, dass jede Änderung auch wieder Rückgängig gemacht werden kann. `git log` z.B. zeigt die Historie des aktuellen Verzeichnisses an. Wenn also eine Änderung einen Fehler mitbringt, kann man mit dem Befehl `git revert HEAD` die letzte Änderung rückgängig machen. Für das Verteilte Arbeiten bringt es den Vorteil, dass git ein Hash-Graph ist und jeder Commit einen Vorgänger-Commit hat (ähnlich einer Blockchain). Wenn die Änderungen eines Kollegen oder einer Kollegin in den eigenen übertragen werden sollen und die Änderungen sich nicht gegenseitig betreffen, der Hash-Graph mit einem `git rebase` zusammengeführt werden.
