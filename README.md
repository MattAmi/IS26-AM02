# Guida al Workflow con Git e IntelliJ

## 1. Setup Iniziale (Solo la prima volta)

Per scaricare questa base pulita:
1. Aprite IntelliJ IDEA.
2. Cliccate su **File -> New -> Project from Version Control...** (o "Get from VCS" nella schermata di avvio).
3. Incollate l'URL di questo repository GitHub.
4. Scegliete una cartella sul vostro PC e cliccate **Clone**.
5. *Se vi chiede di fidarvi del progetto, cliccate "Trust Project".*

---

## 2. Il Workflow Quotidiano (Il Loop d'Oro con i Branch)

Ogni volta che iniziate a lavorare a una nuova feature o a un bug (speriamo mai), seguite **ESATTAMENTE** questi passaggi, in quest'ordine. 

### STEP 1: Tornare alla base e aggiornare
Prima di fare qualsiasi cosa, assicuratevi di partire dall'ultima versione pulita del progetto.
* In basso a destra su IntelliJ, cliccate sul nome del branch attuale (se avete appena aperto il progetto sarà `main` o `master`).
* Cliccate su **main** nell'elenco -> **Checkout**.
* Ora in alto a destra cliccate la **freccia blu verso il basso** (Update Project) per scaricare tutto il lavoro confermato dagli altri.

### STEP 2: Creare il VOSTRO Branch (Fondamentale!)
Ora che siete aggiornati, createvi il vostro spazio sicuro.
* Cliccate di nuovo in basso a destra su `main`.
* Scegliete **New Branch**.
* Dategli un nome sensato, tutto minuscolo e con i trattini al posto degli spazi (es. `aggiunta-login`, `fix-bottone-rosso`). Assicuratevi che la spunta "Checkout branch" sia attiva e premete **Create**.
* *Boom. Ora siete nel vostro branch. Potete spaccare tutto qui dentro e il `main` non ne risentirà.*

### STEP 3: Scrivere il codice
* scrivete la vostra logica e fate le modifiche che vi servono (non devono esse troppe in 1 volta non sarebbe best practice).
* **ATTENZIONE:** Non spostate i file in altre cartelle e non rinominateli. Se serve ristrutturare, ne parliamo prima tutti insieme.

### STEP 4: Il Commit (Salvare in locale)
1. Aprite il tab **Commit** (barra verticale a sinistra o `Alt + 0` / `Cmd + 0`).
2. Vedrete l'elenco dei file modificati (in blu/verde).
3. Spuntate le caselle dei file da includere.
4. Scrivete un **Messaggio di Commit** che abbia senso per far capire agli altri cosa avete fatto.
5. Cliccate sul bottone blu **Commit**. *(Fatelo spesso, ogni volta che completate un pezzettino logico funzionante!)*

### STEP 5: Il Push (Mandare il branch su GitHub)
Il Commit salva i dati sul vostro PC. Per mandarli su GitHub:
* In alto a destra, cliccate la **freccia verde verso l'alto** (Push).
* IntelliJ vi farà vedere che state mandando online il *vostro* branch (es. `origin/aggiunta-login`). Cliccate **Push**.

### STEP 6: Unire il lavoro al Main (La Pull Request / Merge)
Avete finito la vostra feature e volete metterla nel progetto ufficiale?
* Andate sulla pagina del repository su GitHub dal browser.
* Vedrete un avviso con scritto **"Compare & pull request"** vicino al vostro branch appena pushato. Cliccatelo.
* Scrivete due righe su cosa fa la vostra feature e aprite la Pull Request. 
* Un bro controllerà il codice e, se è tutto ok, cliccherà **Merge pull request**. Lavoro unito con successo!

---

## 3. Emergenze e Soluzioni rapide

### A. Ho scritto codice sul branch sbagliato! (Stash)
Vi accorgete di aver lavorato sul `main` invece che sul vostro branch, ma **non** avete ancora fatto commit:
1. Tasto destro sulla cartella principale del progetto a sinistra -> **Git -> Stash Changes...** (Questo "nasconde" le modifiche in una scatola).
2. Seguite lo STEP 2 per creare il vostro branch corretto.
3. Tasto destro sul progetto -> **Git -> Unstash Changes...** -> Selezionate l'ultima voce in lista e cliccate **Apply Stash**. Il codice è tornato nel branch giusto!

### B. CAOS e il codice non va più (Rollback)
Se state lavorando su un file, si rompe tutto e volete tornare a come era prima dell'ultimo commit:
* Tasto destro sul file rotto -> **Git -> Rollback...**
* Tutte le vostre modifiche non committate vengono cancellate.

### C. "IntelliJ non trova le dipendenze o i file rossi" (Maven Sync)
* Aprite il tab "Maven" a destra.
* Cliccate sull'icona delle due frecce circolari (**Reload All Maven Projects**). Aspettate che finisca di caricare in basso a destra.

### D. "MERGE CONFLICT!"
Succede se in fase di unione due persone hanno modificato la *stessa identica riga*. IntelliJ aprirà una finestra **Conflicts**.
1. Cliccate su **Merge...**
2. Si apriranno 3 colonne: a sinistra il vostro codice, a destra quello del bro, in mezzo il risultato.
3. Usate le **"X"** o le **">>"** (frecce) colorate ai lati per decidere quale pezzo tenere al centro.
4. Cliccate **Apply**.
