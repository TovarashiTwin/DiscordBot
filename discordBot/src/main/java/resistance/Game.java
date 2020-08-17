package resistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.javacord.api.entity.user.User;

public class Game {

	public enum Rol {
		RESISTANCE, SPY
	}
	public enum GameState{	 WAITPROPOSETEAM, WAITVOTES,WAITMISSION,PREPARINGGAME}

	// FILA NUM PLAYERS -> COLUMNA NUM MISION
	private static final int[][] PLAYERSFORMISSION = { { 2, 3, 2, 3, 3 }, { 2, 3, 4, 3, 4 }, { 2, 3, 3, 4, 4 },
			{ 3, 4, 4, 5, 5 }, { 3, 4, 4, 5, 5 }, { 3, 4, 4, 5, 5 } };

	private List<Player> jugadores;
	private int round;
	private int leader;//es un entero para rotarlo de forma más fácil
	private GameState state;
	private int numVictoriasResistencia = 0;
	private int numVictoriasSpys = 0;
	
	


	public Game() {
		jugadores = new ArrayList<Player>();
		round = 0;//TODO actualmente no tiene sentido

	}
	

	public GameState getState() {
		return state;
	}


	public void setState(GameState state) {
		this.state = state;
	}


	public void addPlayer(User user) {
		// TODO no jugadores repetidos (dejarlo para debug??)
		jugadores.add(new Player(user));
		
	}

	public boolean start() {
		if (round == 0) {
			switch (jugadores.size()) {
			case 5:
				givePlayersRoles(3, 2);
				break;
			case 6:
				givePlayersRoles(4, 2);
				break;
			case 7:
				givePlayersRoles(4, 3);
				break;
			case 8:
				givePlayersRoles(5, 3);
				break;
			case 9:
				givePlayersRoles(6, 3);
				break;
			case 10:
				givePlayersRoles(6, 4);
				break;
			default:// TODO saltar error, por ahora tirar pa lante
				givePlayersRoles(jugadores.size(), jugadores.size());
			}
			round = 1;
			giverRandomPlayerLeaderRole();
			return true;
		} else
			return false;
	}

	
	
	private void giverRandomPlayerLeaderRole() {
		Random rn = new Random();
		leader = rn.nextInt(jugadores.size());
		
	}
	private void giveNextPlayerLeader() {
		leader++;
		if(leader >= jugadores.size())
			leader = 0;		
	}

	/**
	 * Le da a los jugadores el rol que les corresponde
	 * 
	 * @param numResistance
	 * @param numSpies
	 */
	private void givePlayersRoles(int numResistance, int numSpies) {
		List<Rol> roles = new ArrayList<Rol>();
		for (int i = 0; i < numResistance; i++)
			roles.add(Rol.RESISTANCE);
		for (int i = 0; i < numSpies; i++)
			roles.add(Rol.SPY);
		Collections.shuffle(roles);
		for (int i = 0; i < jugadores.size(); i++)
			jugadores.get(i).setRol(roles.get(i));
	}

	public int getNumPlayersForMission() {
		return PLAYERSFORMISSION[jugadores.size()-1][round-1];
	}

	public List<Player> getPlayers() {
		return jugadores;
	}
	public int getNumVictoriasResistencia() {
		return numVictoriasResistencia;
	}


	public int getNumVictoriasSpys() {
		return numVictoriasSpys;
	}
	public Player getLeader() {
		return jugadores.get(leader);
	}
	public int getRound() {
		return round;
	}

}
