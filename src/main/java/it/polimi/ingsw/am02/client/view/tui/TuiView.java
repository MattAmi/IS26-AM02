package it.polimi.ingsw.am02.client.view.tui;

import it.polimi.ingsw.am02.client.model.ClientModel;
import it.polimi.ingsw.am02.client.model.ClientModelObserver;
import it.polimi.ingsw.am02.client.view.View;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;

import java.util.List;

public class TuiView implements View, ClientModelObserver {
    private final ClientModel model;

    public TuiView(ClientModel model) {
        this.model = model;
        this.model.addObserver(this);
    }

    @Override
    public void update() {
        render();
    }

    public void render() {
        clearScreen();
        System.out.println("========================================");
        System.out.println("          MESOS - BOARD GAME            ");
        System.out.println("========================================");

        if (model.getMyNickname() == null) {
            renderLogin();
        } else if (model.getCurrentLobby() == null) {
            renderLobbyList();
        } else {
            renderLobby();
        }

        System.out.print("\n> ");
    }

    private void renderLogin() {
        System.out.println("\nWelcome! Please log in to start.");
        System.out.println("Usage: login <your_nickname>");
    }

    private void renderLobbyList() {
        System.out.println("Logged in as: " + model.getMyNickname());
        System.out.println("\n--- AVAILABLE LOBBIES ---");

        List<LobbyInfo> lobbies = model.getAvailableLobbies();
        if (lobbies.isEmpty()) {
            System.out.println("No active lobbies. Create one!");
        } else {
            for (int i = 0; i < lobbies.size(); i++) {
                LobbyInfo info = lobbies.get(i);
                System.out.println("[" + i + "]" +
                        " | Players: " + info.currentPlayers() + "/" + info.expectedPlayers());
            }
        }
        System.out.println("\nCommands: create <size> | join <index> | quit");
    }

    private void renderLobby() {
        LobbyInfo lobby = model.getCurrentLobby();
        System.out.println("Logged in as: " + model.getMyNickname());
        System.out.println("\n--- LOBBY DETAILS ---");
        System.out.println("Lobby ID: " + lobby.lobbyId());
        System.out.println("Status: Waiting for players (" + lobby.currentPlayers() + "/" + lobby.expectedPlayers() + ")");

        System.out.println("\nPlayers inside:");
        lobby.currentPlayers().forEach(name -> System.out.println(" - " + name));

        System.out.println("\nCommands: totem <color> | leave | start | quit");
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }


    public void displayError(String message) {
        System.err.println("\n[ERROR] " + message);
    }
}