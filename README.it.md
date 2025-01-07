# Hermes
[![en](https://img.shields.io/badge/lang-en-red.svg)](https://github.com/GiannettaGerardo/Hermes/blob/main/README.md)

Hermes è un process/workflow engine concorrente, veloce e facile da usare. Non supporta alcuno standard specifico per la
creazione di processi ed esegue pochissimi controlli sul formato del grafo, rendendo Hermes molto veloce. La libreria instanzia 
il grafo in memoria e si aspetta che ci rimanga per tutta la durata del processo. Hermes può essere usato benissimo per 
processi ad instanziazione ripetuta frequentemente ma mostra il meglio di sé su processi a lunga durata in memoria.

Hermes è thread-safe e concorrente, non adotta mutex bloccanti in nessuna operazione di scrittura o lettura pur permettendo
la scrittura ad un solo thread alla volta.

## Nodi del grafo

Hermes prevede 4 tipologie di nodi per creare i suoi grafi:

- **TASK**: l'unica tipologia di nodi che può essere letta dal grafo e su cui il workflow si ferma in attesa di una 
operazione di completamento. Può essere completata inserendo delle variabili, la cui numerosità deve essere esplicitata;
- **FORWARD**: nodo su cui il workflow non si ferma mai ma passa sempre oltre, con tutti i vantaggi degli archi condizionali e fork;
- **JOIN**: nodo utilizzato appositamente per riunire archi multipli derivanti da fork. Richiede l'esplicito inserimento 
del numero *minimo* di archi di cui effettuare il join;
- **ENDING**: nodo utilizzato per stabilire la fine ufficiale del workflow. Dopo che il workflow è arrivato su di un nodo 
di ending, non saranno possibili ulteriori operazioni sul grafo e qualunque ramificazione in fork lasciata in sospeso non
non potrà più essere terminata.

## Archi del grafo

Hermes prevede 2 tipologie di archi:

- **Normale**: contiene l'ID dell'unico nodo di destinazione;
- **Condizionale**: contiene una condizione **IF** - **ELSE IF** - **ELSE** in forma di lista e utilizzando il formato
[**Json Logic**](https://jsonlogic.com/) per la valutazione. Ogni elemento della lista deve contenere l'ID di un nodo di destinazione.

***Work in progress...***