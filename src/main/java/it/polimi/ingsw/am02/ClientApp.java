package it.polimi.ingsw.am02;

import it.polimi.ingsw.am02.client.network.rmi.RmiServerProxy;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import it.polimi.ingsw.am02.common.messages.events.Event;
import it.polimi.ingsw.am02.common.messages.events.lobby.UpdatedLobbiesEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.UpdatedLobbyEvent;
import it.polimi.ingsw.am02.common.messages.events.lobby.UsernameResultEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * ClientApp normalizzata per Stress Test RMI.
 */
public class ClientApp {
    private static List<LobbyInfo> availableLobbies = new ArrayList<>();
    private static String currentLobbyId = null;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== 🧪 STRESS TEST CLIENT - AM02 ===");

        try {
            // Istanziamo il Proxy con l'override del notifyEvent per gestire i log di test
            RmiServerProxy proxy = new RmiServerProxy("127.0.0.1", 1099) {
                @Override
                public void notifyEvent(Event event) {
                    handleTestEvent(event);
                }
            };

            proxy.connect();

            System.out.print("👤 Inserisci Nickname: ");
            String nickname = scanner.nextLine();
            proxy.requestSetUsername(nickname);

            while (true) {
                // Delay per permettere agli eventi asincroni di stampare prima del menu
                Thread.sleep(300);

                System.out.println("\n----------------------------------");
                System.out.println("MENU: c (Crea Lobby) | j (Entra in Lobby) | q (Esci)");
                System.out.print("> ");

                String cmd = scanner.nextLine().toLowerCase().trim();

                switch (cmd) {
                    case "c" -> {
                        System.out.println("⏳ Creazione lobby in corso...");
                        proxy.requestCreateLobby(3);
                    }
                    case "j" -> {
                        if (availableLobbies.isEmpty()) {
                            System.out.println("❌ Nessuna lobby disponibile nella lista locale.");
                        } else {
                            System.out.println("--- ELENCO LOBBY ---");
                            for (int i = 0; i < availableLobbies.size(); i++) {
                                LobbyInfo info = availableLobbies.get(i);
                                // Concatenazione semplice per evitare errori di formattazione printf
                                System.out.println("[" + i + "]" +
                                        " (" + info.currentPlayers() + "/" + info.expectedPlayers() +
                                        ") ID: " + info.lobbyId());
                            }
                            System.out.print("Scegli il numero della lobby: ");
                            try {
                                int index = Integer.parseInt(scanner.nextLine());
                                String id = availableLobbies.get(index).lobbyId();
                                System.out.println("⏳ Invio richiesta di partecipazione per: " + id);
                                proxy.requestJoinLobby(id);
                            } catch (Exception e) {
                                System.out.println("❌ Selezione non valida.");
                            }
                        }
                    }
                    case "q" -> {
                        System.out.println("Uscita in corso...");
                        proxy.disconnect();
                        System.exit(0);
                    }
                    default -> System.out.println("⚠️ Comando non riconosciuto.");
                }
            }
        } catch (Exception e) {
            System.err.println("💥 Errore fatale: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleTestEvent(Event event) {
        // Pulizia dello stream di output per evitare sovrapposizioni visive
        System.out.flush();

        if (event instanceof UsernameResultEvent e) {
            System.out.println("\n[AUTH] " + (e.isValid() ? "✅ Login effettuato: " + e.username() : "❌ Rifiutato: "));
        }
        else if (event instanceof UpdatedLobbiesEvent e) {
            availableLobbies = e.lobbies();
            System.out.println("\n[LIST] 📢 Ricevuto elenco aggiornato (" + availableLobbies.size() + " lobby attive)");
        }
        else if (event instanceof UpdatedLobbyEvent e) {
            LobbyInfo info = e.lobby();
            currentLobbyId = info.lobbyId();
            System.out.println("\n[LOBBY] 🏠 AGGIORNAMENTO: Sei nella lobby: " + info.lobbyId());
            System.out.println("        Giocatori: " + info.currentPlayers() + "/" + info.expectedPlayers());
            System.out.println("        ID: " + currentLobbyId);
        }
        else {
            System.out.println("\n[EVENT] Ricevuto messaggio: " + event.getClass().getSimpleName());
        }
        // Ristampa il prompt per mantenere l'interfaccia pulita
        System.out.print("\n> ");
    }
}