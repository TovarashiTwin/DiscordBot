package discordBot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import resistance.Game;
import resistance.Player;
import resistance.Vote;

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
	private static final String THUMBSUP =  "👍";//preferiria que estuviese con \ u peeero
	private static final String THUMBSDOWN =  "👎";

	private Game game = null;
	private TextChannel channel;// TODO solo escucharr los mensajes de este canal para la mayoria de los comandos
	private List<Vote> votos;
	private List<User> missionParticipants; //TODO esto para Game
	
	DiscordApi api;

	public Main() {
		// TODO comprobaciones del estado del juego
		// TODO solo soporta un juego a la vez
		// TODO crear canales y borrar mensajes etc
		// TODO mejorar robustez
	}

	public static void main(String[] args) {
		new Main().run(args[0]);

	}

	private void run(String token) {
		api = new DiscordApiBuilder().setToken(token).login().join();

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
			case (END):
				end(event);
				break;
			}
		});
	}

	private void end(MessageCreateEvent event) {
		System.out.println("Comando: " + END + " invocado por: " + event.getMessageAuthor().getDisplayName());
		if (channel != null)
			channel.asServerChannel().ifPresent(channel -> channel.delete());

	}

	private void proposeTeam(MessageCreateEvent event) {
		System.out.println("Comando: " + PROPOSETEAM + " invocado por: " + event.getMessageAuthor().getDisplayName());
		
		
		missionParticipants = event.getMessage().getMentionedUsers();
		
		
		int aux = game.getNumPlayersForMission();
		if (missionParticipants.size() != aux) {// TODO mirar que sea un jugador!
			channel.sendMessage("El tamaño del equipo debe de ser: " + game.getNumPlayersForMission());
		}else {// EQUIPO PUEDE SER VOTADO
				
			List<CompletableFuture<Message>> mensajes = new ArrayList<CompletableFuture<Message>>();
			for(Player thePlayer:game.getPlayers()) {
				CompletableFuture<Message> mensaje;
				(mensaje = thePlayer.getUser().sendMessage("Reacciona a este mensaje con '" +THUMBSUP + "' para aceptar la votación\n"
						+ "o con '"+THUMBSDOWN+"' para rechazarla")).thenAcceptAsync(
								message -> {
									message.addReaction(THUMBSUP);
									message.addReaction(THUMBSDOWN);									
									message.addReactionAddListener(emojiEvent -> {										
										emojiEvent.getEmoji().asUnicodeEmoji().ifPresent(emoji -> {
											if(emoji.equals(THUMBSUP) && !emojiEvent.getUser().isBot())
												registerVote(thePlayer, true);
											else if(emoji.equals(THUMBSDOWN) && !emojiEvent.getUser().isBot())
												registerVote(thePlayer, false);
											else if(!emojiEvent.getUser().isBot())
												thePlayer.getUser().sendMessage("No me intentes liar puto 😠");
											});
										});									
									});
			}
			//me espero a que se manden todos los mensajes
//			for(CompletableFuture<Message> future:mensajes)
//				future.join();
			
//			for (User theUser : participantes) {
//				theUser.sendMessage("Prueba prueba");// TODO
//			}
		}

	}

	private void startGame(MessageCreateEvent event) {
		System.out.println("Comando: " + STARTGAME + " invocado por: " + event.getMessageAuthor().getDisplayName());
		if (game != null && game.start()) {// TODO actualmente solo se puede tener un game
			channel.sendMessage("Juego empezado!");
			game.start();
			for (Player thePlayer : game.getPlayers()) {
				thePlayer.getUser().sendMessage("Tu rol es " + thePlayer.getRol());
			}
			// TODO usar el display name
			preProposeTeam();
		} else {
			channel.sendMessage("No se ha podido empezar el juego");
		}
	}

	private void join(MessageCreateEvent event) {
		System.out.println("Comando: " + JOIN + " invocado por: " + event.getMessageAuthor().getDisplayName());
		if (game != null) {
			List<User> users = event.getMessage().getMentionedUsers();
			if (users.size() == 0) {
				Optional<User> optionalUser = event.getMessageAuthor().asUser();
				optionalUser.ifPresent(user -> game.addPlayer(user));
				channel.sendMessage("Jugador añadido: " + event.getMessageAuthor().getDisplayName());
			} else {
				for(User theUser:users)
					game.addPlayer(theUser);
				channel.sendMessage("Jugadores añadidos");
			}
		} else {
			channel.sendMessage("No se ha podido añadir el jugador");
		}
	}

	private void prepareGame(MessageCreateEvent event) {
		System.out.println("Comando: " + PREPAREGAME + " invocado por: " + event.getMessageAuthor().getDisplayName());
		Server server;
		if (event.getServer().isPresent()) {
			server = event.getServer().get();
			game = new Game();
			channel = new ServerTextChannelBuilder(server).setName("Partida Resistencia").create().join();
			votos = Collections.synchronizedList(new ArrayList<Vote>());
			event.getChannel().sendMessage("Canal creado para jugar!");
		} else {
			System.err.println("Server no presente");
		}

	}

	private void test(MessageCreateEvent event) {
		System.out.println("Comando: " + TEST + " invocado por: " + event.getMessageAuthor().getDisplayName());
		prepareGame(event);
		join(event);
		startGame(event);
		proposeTeam(event);
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
		channel.sendMessage("Ronda: " + game.getRound());
		channel.sendMessage("Victorias Rebeldes: " + game.getNumVictoriasResistencia());
		channel.sendMessage("Victorias Espías: " + game.getNumVictoriasSpys());
		preProposeTeam();
	}

	/**
	 * Explica cuantos tienen que ir
	 */
	private void preProposeTeam() {
		String message = "";
		message += "El jugador: " + game.getLeader().getUser().getMentionTag() + " es el lider\n";
		int numJugadoresMision = game.getNumPlayersForMission();
		message += "Necesita escoger un total de: " + numJugadoresMision + " jugadores" + "\n";
		message += "Se escogen con: " + PROPOSETEAM + " <@user> <@user> ...";
		channel.sendMessage(message);

	}
	/**
	 * TODO actualmente no se puede cambiar un voto
	 * @param player
	 * @param vote
	 */
	private void registerVote(Player player, boolean vote) {
		// Codigo para no repetir votos
		boolean aux = false;
		for (Vote theVote : votos)// esto es poco optimo pero el juego es de maximo 10 jugadores so np
			if (theVote.getUser().equals(player.getUser()))
				;// comprobar si el equals de user funciona
		aux = true;
		if (aux)
			votos.add(new Vote(player.getUser(), vote));

		// Comprobar si todos los votos estan hechos ya

		if (game.getPlayers().size() == votos.size()) {
			
			int numRechazados = 0;
			MessageBuilder mb = new MessageBuilder().append("Los resultados de los votos son:\n");

			for (Vote theVote : votos) {
				if (!theVote.isVoto())
					numRechazados++;
				mb.append(theVote + "\n");
			}
			if(numRechazados >= game.getPlayers().size())
				mb.append("Propuesta rechazada",MessageDecoration.BOLD);
			else {
				mb.append("Propuesta aceptada",MessageDecoration.BOLD);
				mision();
			}
				
			mb.send(channel);
			
			//ya se puede limpiar la lista de votos
			votos.clear();			
		}
	}

	private void mision() {
		for(User theUser: missionParticipants) {
			
		}
			
		
	}

}
