package resistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.javacord.api.entity.user.User;

public class Game {

	public enum Rol {
		RESISTANCE, SPY
	}
	public enum GameState{ WAITPROPOSETEAM, WAITVOTES, WAITMISSION, PREPARINGGAME }

	// FILA NUM PLAYERS -> COLUMNA NUM MISION
	private static final int[][] PLAYERSFORMISSION = { {1,2,1,2,1},{1,2,1,2,1},{1,2,3,2,1},{1,2,3,2,1},{ 2, 3, 2, 3, 3 }, { 2, 3, 4, 3, 4 }, { 2, 3, 3, 4, 4 },
			{ 3, 4, 4, 5, 5 }, { 3, 4, 4, 5, 5 }, { 3, 4, 4, 5, 5 } };

	private List<Player> jugadores;
	private int round;
	private int leader;//es un entero para rotarlo de forma más fácil
	private GameState state;
	private int numVictoriasResistencia = 0;
	private int numVictoriasSpys = 0;
	private List<Player> missionParticipants;

	public Game() {
		jugadores = new ArrayList<Player>();
		missionParticipants = new ArrayList<Player>();
		round = 0;//TODO actualmente no tiene sentido

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
	
	/**
	 * 
	 * @return true spy win
	 */
	public boolean giveSpyAWin() {
		round ++;
		numVictoriasSpys++;
		if(numVictoriasSpys == 3)
			return true;
		return false;
	}
	/**
	 * @return true resistance win
	 */
	public boolean giveResistanceAWin() {
		round++;
		numVictoriasResistencia++;
		if(numVictoriasResistencia == 3)
			return true;
		return false;
	}
	
	private void giverRandomPlayerLeaderRole() {
		Random rn = new Random();
		leader = rn.nextInt(jugadores.size());
		
	}
	public Player giveNextPlayerLeader() {
		leader++;
		if(leader >= jugadores.size())
			leader = 0;
		return getLeader();
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
		//TODO tiene que empezar desde 5 realmente!!!!!!
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

	public Player checkPlayer(User theUser) {
		//esto es super poco eficiente pero nunca habrá más de 10 jugadores so :)
		for(Player thePlayer:jugadores)
			if(thePlayer.getUser().getIdAsString().equals(theUser.getIdAsString()))
				return thePlayer;
		return null;
		
	}

	public void addMissionParticipant(Player theUser) {
		missionParticipants.add(theUser);//TODO users repetidos? de momento se puede		
	}
	public void clearMissionParticipants() {
		missionParticipants = new ArrayList<Player>();
	}
	
	public List<Player> getMissionParticipants() {
		return missionParticipants;
	}
	public GameState getState() {
		return state;
	}


	public void setState(GameState state) {
		this.state = state;
	}


	public boolean addPlayer(User user) {
		if(!user.isBot()) {//Los bots no se pueden unir
			if(checkPlayer(user) != null) {//Ese jugador ya está en la partida, no se puede unir
				return false;
			}			
			jugadores.add(new Player(user));					
			return true;
		}
		return false;		
		
	}

	public List<String> getSpys() {
		List<String> toReturn = new ArrayList<String>();		
		for(Player thePlayer:jugadores)
			if(thePlayer.getRol() == Rol.SPY)
				toReturn.add(thePlayer.getUser().getDiscriminatedName());		
		return toReturn;
	}

}
