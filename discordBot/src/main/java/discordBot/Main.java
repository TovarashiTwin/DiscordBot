package discordBot;

import java.util.List;
import java.util.Optional;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import resistance.Game;
import resistance.Player;

public class Main {

	// this are the states where the game needs to wait for the players

	// TODO determinar prefijo (prefix)
	public static final String JOIN = "!join";
	public static final String HELP = "!help";
	public static final String STARTGAME = "!start";
	public static final String PROPOSETEAM = "!team";
	public static final String INFO = "!info";
	public static final String TEST = "!test";
	public static final String PREPAREGAME = "!prepare";
	public static final String END = "!end";

	private Game game = null;
	private TextChannel channel;//TODO solo escucahr los mensajes de este canal para la mayoria de los comandos
	DiscordApi api;

	public Main() {
		// TODO solo soporta un juego a la vez
		// TODO crear canales y borrar mensajes etc
		// TODO mejorar robustez
	}

	public static void main(String[] args) {
		new Main().run(args[0]);

	}

	private void run(String token) {
		api = new DiscordApiBuilder().setToken(token)
				.login().join();

		System.out.println("Logeado!");

		api.addMessageCreateListener(event -> {
			Message message = event.getMessage();
			switch (message.getContent().split(" ")[0].toLowerCase()) {

			case (PREPAREGAME):
				prepareGame(event);
				break;
			case (JOIN):
				join(event);
				break;
			case (STARTGAME):
				startGame(event);
				break;

			case (PROPOSETEAM):
				proposeTeam(event);
				break;

			case (INFO):
				info(event);
				break;
			case (HELP):
				help(event);
				break;

			case (TEST):
				test(event);
				break;			
			
			case(END):
				end(event);
				break;
			}
		});
	}

	

	private void end(MessageCreateEvent event) {
		System.out.println("Comando: " + END + " invocado por: " + event.getMessageAuthor().getDisplayName());
		if(channel != null)
			channel.asServerChannel().ifPresent(channel -> channel.delete());
			
	}

	private void proposeTeam(MessageCreateEvent event) {
		System.out.println("Comando: " + PROPOSETEAM + " invocado por: " + event.getMessageAuthor().getDisplayName());		
		List<User> participantes = event.getMessage().getMentionedUsers();
		int aux = game.getNumPlayersForMission();
		if(participantes.size() != aux)
			channel.sendMessage("El tamaño del equipo debe de ser: "+game.getNumPlayersForMission());
		else {//EQUIPO ACEPTADO
			//TODO mirar que sea un jugador!
			for(User theUser:participantes) {
				theUser.sendMessage("Prueba prueba");//TODO
			}
		}
		
		
	}

	private void startGame(MessageCreateEvent event) {
		System.out.println("Comando: " + STARTGAME + " invocado por: " + event.getMessageAuthor().getDisplayName());
		if (game != null && game.start()) {// TODO actualmente solo se puede tener un game
			event.getChannel().sendMessage("Juego empezado!");
			game.start();
			for (Player thePlayer : game.getPlayers()) {
				thePlayer.getUser().sendMessage("Tu rol es " + thePlayer.getRol());
			}
			//TODO usar el display name			
			preProposeTeam();
		} else {
			channel.sendMessage("No se ha podido empezar el juego");
		}
	}

	

	private void join(MessageCreateEvent event) {
		System.out.println("Comando: " + JOIN + " invocado por: " + event.getMessageAuthor().getDisplayName());
		if (game != null) {
			Optional<User> optionalUser = event.getMessageAuthor().asUser();
			optionalUser.ifPresent(user -> game.addPlayer(user));
			event.getChannel().sendMessage("Jugador añadido: " + event.getMessageAuthor().getDisplayName());
		} else {
			channel.sendMessage("No se ha podido añadir el jugador");
			
		}
	}

	private void prepareGame(MessageCreateEvent event) {
		System.out.println("Comando: " + PREPAREGAME + " invocado por: " + event.getMessageAuthor().getDisplayName());
		Server server;
		if(event.getServer().isPresent()) {
			server = event.getServer().get();
			game = new Game();
			channel = new ServerTextChannelBuilder(server).setName("Partida Resistencia").create().join();
			event.getChannel().sendMessage("Canal creado para jugar!");
		}else {
			System.err.println("Server no presente");
		}
		
		
		
	}	
	
	
	private void test(MessageCreateEvent event) {
		System.out.println("Comando: " + TEST + " invocado por: " + event.getMessageAuthor().getDisplayName());
		event.getChannel().sendMessage("Test 1");
		event.getChannel().sendMessage("Test 2");
		event.getChannel().sendMessage("Test 3");
		event.getChannel().sendMessage("Test 4");
		event.getChannel().sendMessage("Test 5");
	}

	private void help(MessageCreateEvent event) {
		System.out.println("Comando: " + HELP + " invocado por: " + event.getMessageAuthor().getDisplayName());
		event.getChannel().sendMessage("Commando no implementado :)");
	}

	private void info(MessageCreateEvent event) {
		System.out.println("Comando: " + INFO + " invocado por: " + event.getMessageAuthor().getDisplayName());
		event.getChannel().sendMessage("Commando no implementado :)");
	}
	
	/**
	 * 
	 */
	private void inicioDeRonda() {
		channel.sendMessage("Ronda: "+game.getRound());
		channel.sendMessage("Victorias Rebeldes: "+game.getNumVictoriasResistencia());
		channel.sendMessage("Victorias Espías: "+game.getNumVictoriasSpys());
		preProposeTeam();
	}
	/**
	 * Explica cuantos tienen que ir
	 */
	private void preProposeTeam() {
		String message = "";
		message += "El jugador: " + game.getLeader().getUser().getMentionTag() + " es el lider\n";
		int numJugadoresMision = game.getNumPlayersForMission();
		message += "Necesita escoger un total de: "+numJugadoresMision +" jugadores"+ "\n";
		message += "Se escogen con: "+PROPOSETEAM + " <@user> <@user> ...";
		channel.sendMessage(message);
		
	}
	
	
	
	

}
