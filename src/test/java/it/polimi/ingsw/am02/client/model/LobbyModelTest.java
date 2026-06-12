package it.polimi.ingsw.am02.client.model;

import it.polimi.ingsw.am02.client.view.ClientView;
import it.polimi.ingsw.am02.common.dto.LobbyInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the client-side {@link LobbyModel}, verifying observer registration and
 * the state updates (nickname, available lobbies, current lobby, errors) it
 * propagates to the registered {@link ClientView}.
 */
class LobbyModelTest {

    private LobbyModel lobbyModel;
    private ClientView mockView;

    @BeforeEach
    void setUp() {
        lobbyModel = new LobbyModel();
        mockView = Mockito.mock(ClientView.class);
    }

    /**
     * Verifies that observers are registered only once and stop receiving
     * updates after being removed.
     */
    @Test
    void testAddRemoveObserver() {
        lobbyModel.addObserver(mockView);
        // We can verify it's added by triggering an update
        lobbyModel.updateError("test error");
        verify(mockView, times(1)).onError("test error");

        // Add again - should not be added twice
        lobbyModel.addObserver(mockView);
        lobbyModel.updateError("test error 2");
        verify(mockView, times(1)).onError("test error 2");

        lobbyModel.removeObserver(mockView);
        lobbyModel.updateError("test error 3");
        verify(mockView, never()).onError("test error 3");
    }

    /**
     * Verifies that the nickname is stored only on a successful username result
     * and left unchanged on failure.
     */
    @Test
    void testUpdateUsernameResult() {
        lobbyModel.addObserver(mockView);
        
        // Valid username
        lobbyModel.updateUsernameResult("user1", true, null);
        assertEquals("user1", lobbyModel.getMyNickname());
        verify(mockView).onUsernameResult("user1", true, null);

        // Invalid username
        lobbyModel.updateUsernameResult("user2", false, "reason");
        assertEquals("user1", lobbyModel.getMyNickname()); // Nickname should not change
        verify(mockView).onUsernameResult("user2", false, "reason");
    }

    /**
     * Verifies that refreshing the available lobbies clears the cached nickname
     * and current lobby and notifies the view.
     */
    @Test
    void testUpdateAvailableLobbies() {
        lobbyModel.addObserver(mockView);
        List<LobbyInfo> lobbies = new ArrayList<>();
        lobbies.add(new LobbyInfo("l1", 4, new ArrayList<>(), new java.util.HashMap<>()));
        
        // Setting nickname and current lobby to check if they are cleared
        lobbyModel.updateUsernameResult("user1", true, null);
        lobbyModel.updateCurrentLobby(new LobbyInfo("l2", 4, new ArrayList<>(), new java.util.HashMap<>()));
        
        lobbyModel.updateAvailableLobbies(lobbies);
        
        assertNull(lobbyModel.getMyNickname());
        assertNull(lobbyModel.getCurrentLobby());
        assertEquals(lobbies, lobbyModel.getAvailableLobbies());
        verify(mockView).onAvailableLobbiesUpdated(lobbies);
    }

    /**
     * Verifies that the current lobby is stored and the view is notified.
     */
    @Test
    void testUpdateCurrentLobby() {
        lobbyModel.addObserver(mockView);
        LobbyInfo lobby = new LobbyInfo("l1", 4, new ArrayList<>(), new java.util.HashMap<>());
        
        lobby.currentPlayers().add("user1");
        
        lobbyModel.updateCurrentLobby(lobby);
        
        assertEquals(lobby, lobbyModel.getCurrentLobby());
        verify(mockView).onCurrentLobbyUpdated(lobby);
    }

    /**
     * Verifies that dissolving a lobby clears the current lobby and notifies the
     * view.
     */
    @Test
    void testUpdateLobbyDissolved() {
        lobbyModel.addObserver(mockView);
        LobbyInfo lobby = new LobbyInfo("l1", 4, new ArrayList<>(), new java.util.HashMap<>());
        lobbyModel.updateCurrentLobby(lobby);
        
        lobbyModel.updateLobbyDissolved();
        
        assertNull(lobbyModel.getCurrentLobby());
        verify(mockView).onLobbyDissolved();
    }

    /**
     * Verifies that error messages are forwarded to the view.
     */
    @Test
    void testUpdateError() {
        lobbyModel.addObserver(mockView);
        lobbyModel.updateError("error message");
        verify(mockView).onError("error message");
    }

    /**
     * Verifies the default state of the model before any update is applied.
     */
    @Test
    void testGetters() {
        assertNull(lobbyModel.getMyNickname());
        assertNotNull(lobbyModel.getAvailableLobbies());
        assertTrue(lobbyModel.getAvailableLobbies().isEmpty());
        assertNull(lobbyModel.getCurrentLobby());
    }
}
