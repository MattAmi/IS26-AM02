

## 1. Setup Iniziale (Solo la prima volta)

Per scaricare questa base pulita:

1. Aprite IntelliJ IDEA.

2. Cliccate su **File -> New -> Project from Version Control...** (o "Get from VCS" nella schermata di avvio).

3. Incollate l'URL di questo repository GitHub.

4. Scegliete una cartella sul vostro PC e cliccate **Clone**.

5. *Se vi chiede di fidarvi del progetto, cliccate "Trust Project".*



---



## 2. Il Workflow Quotidiano (Il Loop d'Oro)

Ogni volta che aprite IntelliJ per lavorare, fate **ESATTAMENTE** questi passaggi, in quest'ordine:



### STEP 1: Aggiornare SEMPRE prima di scrivere ⬇️

Prima di toccare la tastiera, dovete scaricare il lavoro fatto dagli altri.

* In alto a destra in IntelliJ c'è una **freccia blu verso il basso** (Update Project). Cliccatela. 

* *Scorciatoia: `Ctrl + T` (Windows) o `Cmd + T` (Mac).*



### STEP 2: Scrivere il codice 

* Aprite i file "guscio" che vi servono e scrivete la vostra logica al posto dei `// TODO`.

* **ATTENZIONE:** Non spostate i file in altre cartelle e non rinominateli. Se serve ristrutturare, ne parliamo prima tutti insieme per evitare conflitti mortali.



### STEP 3: Il Commit (Salvare in locale ad ogni feature completata)

1. Aprite il tab **Commit** (di solito è una barra verticale a sinistra, oppure premete `Alt + 0` / `Cmd + 0`).

2. Vedrete l'elenco dei file che avete modificato (in blu).

3. Spuntate le caselle dei file che volete includere.

4. Scrivete un **Messaggio di Commit** che abbia senso 

5. Cliccate sul bottone blu **Commit** in basso.



### STEP 4: Il Push (Mandare su GitHub) 

Il Commit salva i dati sul vostro PC. Per mandarli su GitHub per gli altri:

* In alto a destra, cliccate la **freccia verde verso l'alto** (Push).

* Controllate che sia tutto ok e cliccate **Push**.

* *Oppure, nello Step 3, potete cliccare la freccina di fianco al tasto Commit e scegliere direttamente "Commit and Push".*



---



## 3. Emergenze e Soluzioni rapide



### A. "Ho fatto un casino e il mio codice non va più" (Rollback)

Se state lavorando su un file, si rompe tutto e volete tornare a come era prima dell'ultimo commit:

* Tasto destro sul file rotto -> **Git -> Rollback...**

* Tutte le vostre modifiche non committate vengono cancellate e il file torna sano.



### B. "IntelliJ non trova le dipendenze o i file rossi" (Maven Sync)

* Aprite il tab "Maven" a destra.

* Cliccate sull'icona delle due frecce circolari (**Reload All Maven Projects**). Aspettate che finisca di caricare in basso a destra.



### C. "MERGE CONFLICT!" 

Succede quando 2 persone hanno modificato la *stessa identica riga di codice* nello stesso momento. 
Quando provano a fare Pull o Push, IntelliJ blocca e apre una finestra chiamata **Conflicts**.

1. Cliccate su **Merge...**

2. Si apriranno 3 colonne: a sinistra il vostro codice, a destra quello del vostro bro, in mezzo il risultato finale.

3. Usate le **"X"** o le **">>"** (frecce) colorate ai lati per decidere quale pezzo di codice tenere o scartare nella colonna centrale.

4. Quando avete sistemato tutto, cliccate **Apply**. IntelliJ farà il merge da solo!
