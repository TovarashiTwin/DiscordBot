package discordBot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import resistance.Game;
import resistance.Player;
import resistance.Vote;

public class Main {

	
	
	// this are the states where the game needs to wait for the players

	// TODO determinar prefijo (prefix)
	public static final String JOIN = "+join";
	public static final String HELP = "+help";
	public static final String STARTGAME = "+start";
	public static final String PROPOSETEAM = "+team";
	public static final String INFO = "+info";//que hace esto xd
	public static final String TEST = "+test";
	public static final String PREPAREGAME = "+prepare";
	public static final String END = "+end";
	private static final String THUMBSUP =  "👍";//preferiria que estuviese con \ u peeero
	private static final String THUMBSDOWN =  "👎";

	private Game game = null;
	private ServerTextChannel channel;// TODO solo escucharr los mensajes de este canal para la mayoria de los comandos
	private List<Vote> votos;
	private List<Message> deleteableMessages;
	DiscordApi api;

	public Main() {
		// TODO comprobaciones del estado del juego
		// TODO solo soporta un juego a la vez
		// TODO borrar mensajes
		// TODO mejorar robustez
	}

	public static void main(String[] args) {
		new Main().run(args[0]);

	}

	private void run(String token) {
		api = new DiscordApiBuilder().setToken(token).login().join();
		

		api.addMessageCreateListener(event -> {
			Message message = event.getMessage();
			if(!message.getAuthor().isBotUser())
			switch (message.getContent().split(" ")[0].toLowerCase()) {
			case (PREPAREGAME):
				prepareGame(event);
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
			}
		});
		System.out.println("Logeado!");
		
		
	}

	private void end(MessageCreateEvent event) {
		System.out.println("Comando: " + END + " invocado por: " + event.getMessageAuthor().getDisplayName());
		if (channel != null)
			channel.asServerChannel().ifPresent(channel -> channel.delete());
		game = null;
		// TODO borrar todo (complicado con los listeners)(hay un metodo en message de
		// remove listeners :)
	}

	private void proposeTeam(MessageCreateEvent event) {
		System.out.println("Comando: " + PROPOSETEAM + " invocado por: " + event.getMessageAuthor().getDisplayName());
		
		event.getMessageAuthor().asUser().ifPresent(user ->{
			if(user.getIdAsString().equals(game.getLeader().getUser().getIdAsString())) {
				List<User> users = event.getMessage().getMentionedUsers();
				boolean validUsers = true;
				for (User theUser : users) {
					Player player = null;
					if ((player = game.checkPlayer(theUser)) != null)
						game.addMissionParticipant(player);
					else
						validUsers = false;
				}
				int aux = game.getNumPlayersForMission();
				if (game.getMissionParticipants().size() != aux || !validUsers) {
					channel.sendMessage("El tamaño del equipo debe de ser: " + game.getNumPlayersForMission()
							+ " y todos los users deben ser jugadores (" + JOIN +")");
					game.clearMissionParticipants();// hay que researlo
				} else {// EQUIPO PUEDE SER VOTADO
					game.setState(Game.GameState.WAITVOTES);
					channel.sendMessage("Reacciona a este mensaje con '" + THUMBSUP + "' para aceptar la votación\n" + "o con '"
							+ THUMBSDOWN + "' para rechazarla").thenAcceptAsync(message -> {
								message.addReaction(THUMBSUP);
								message.addReaction(THUMBSDOWN);
								message.addReactionAddListener(emojiEvent -> {
									if (!emojiEvent.getUser().isBot()) {
										emojiEvent.getEmoji().asUnicodeEmoji().ifPresent(emoji -> {
											if (emoji.equals(THUMBSUP))
												registerVote(game.checkPlayer(emojiEvent.getUser()), true);
											else if (emoji.equals(THUMBSDOWN))
												registerVote(game.checkPlayer(emojiEvent.getUser()), false);
											message.removeReactionByEmoji(emojiEvent.getUser(), emoji);
										});
									}
								});
							});

				}				
			}
			else
				channel.sendMessage("Solo puede proponer equipo el lider");
							
		});	

	}

	private void startGame(MessageCreateEvent event) {
		System.out.println("Comando: " + STARTGAME + " invocado por: " + event.getMessageAuthor().getDisplayName());
		if (game != null && game.start()) {// TODO actualmente solo se puede tener un game
			//channel.sendMessage("Juego empezado!");
			game.start();
			for (Player thePlayer : game.getPlayers()) {
				thePlayer.getUser().sendMessage("Tu rol es " + thePlayer.getRol());
				Game.Rol rol = thePlayer.getRol();
				String message = "";
				if(rol == Game.Rol.SPY) {
					message += "Los espias son: ";
					for(String espia:game.getSpys())
						message += espia;
					thePlayer.getUser().sendMessage(message);
				}
					
			}
			// TODO usar el display name
			event.getMessage().delete();
			for(Message mensaje:deleteableMessages)
				mensaje.delete();
			game.setState(Game.GameState.WAITPROPOSETEAM);
			preProposeTeam();
		} else {
			channel.sendMessage("No se ha podido empezar el juego");
		}		
	}

	private void join(MessageCreateEvent event) {
		System.out.println("Comando: " + JOIN + " invocado por: " + event.getMessageAuthor().getDisplayName());
		if (game != null) {
			deleteableMessages.add(event.getMessage());
			List<User> users = event.getMessage().getMentionedUsers();
			if (users.size() == 0) {
				Optional<User> optionalUser = event.getMessageAuthor().asUser();
				optionalUser.ifPresent(user -> {					
					if(game.addPlayer(user))
						channel.sendMessage("Jugador añadido: " + event.getMessageAuthor().getDisplayName()).thenAcceptAsync(message -> deleteableMessages.add(message));//parece que si que se puede hacer
					else
						channel.sendMessage("El jugador no se pudo unir, un usuario no puede unirse más de una vez").thenAcceptAsync(message -> deleteableMessages.add(message));
				});
				
			} else {
				boolean jugadorRechazado = false;
				for(User theUser:users)
					if(!game.addPlayer(theUser))
						jugadorRechazado = true;
				channel.sendMessage("Jugadores añadidos").thenAcceptAsync(message -> deleteableMessages.add(message));
				if(jugadorRechazado)
					channel.sendMessage("Uno o más jugadores no pudieron unirse (No se puede unir más de una vez, ni unir un bot").thenAcceptAsync(message -> deleteableMessages.add(message));
			}
		} else {
			channel.sendMessage("No se ha podido añadir el jugador");
		}
	}

	private void prepareGame(MessageCreateEvent event) {
		System.out.println("Comando: " + PREPAREGAME + " invocado por: " + event.getMessageAuthor().getDisplayName());
		Server server;
		if (event.getServer().isPresent() && game == null) {//No permite crear dos juegos seguidos
			server = event.getServer().get();
			game = new Game();
			new ServerTextChannelBuilder(server).setName("Partida-Resistencia").create().thenAcceptAsync(channel -> {
				this.channel = channel;				
				channel.addMessageCreateListener(channelEvent -> {					
					Message message = channelEvent.getMessage();//TODO uso getMessageauthor y getMessage.getAuthor
					if(!message.getAuthor().isBotUser())
					switch (message.getContent().split(" ")[0].toLowerCase()) {
					case (JOIN):
						if(game.getState() == Game.GameState.PREPARINGGAME)
							join(channelEvent);
						else
							this.channel.sendMessage("No puedes unirte a la partida ahora");
						break;
					case (STARTGAME):
						if(game.getState() == Game.GameState.PREPARINGGAME) {
							if(!event.getMessageAuthor().equals(channelEvent.getMessage().getAuthor()))
								startGame(channelEvent);
						}
						else
							this.channel.sendMessage("No puedes unirte a la partida ahora");						
						break;
						
					case (PROPOSETEAM):
						if(game.getState() == Game.GameState.WAITPROPOSETEAM) 
							proposeTeam(channelEvent);
						else
							this.channel.sendMessage("No puedes proponer un equipo ahora");
						break;
						
					case (END):
						if(!event.getMessageAuthor().equals(channelEvent.getMessage().getAuthor()))
						end(channelEvent);
						break;
						
					default:
						if(!channelEvent.getMessageAuthor().isBotUser())
							channelEvent.getMessage().delete();
						break;
					}
				});
				
				channel.sendMessage("Unete a la partida con "+ JOIN).thenAcceptAsync(message -> deleteableMessages.add(message));
				event.getChannel().sendMessage("Canal creado para jugar! " + channel.getMentionTag());
				
			});
			
			deleteableMessages = Collections.synchronizedList(new ArrayList<Message>());
			votos = Collections.synchronizedList(new ArrayList<Vote>());
			
		} else {
			System.err.println("Server no presente, comando invocado por privado? o partida ya empezada");
		}

	}

	private void test(MessageCreateEvent event) {
		System.out.println("Comando: " + TEST + " invocado por: " + event.getMessageAuthor().getDisplayName());
//		prepareGame(event);
//		join(event);
//		startGame(event);
//		proposeTeam(event);
		//new MessageBuilder().setEmbed(new EmbedBuilder().setTitle("Ronda 1").setDescription("Victoria Espías: 1\nVictorias Resistencia: 2").setColor(Color.BLUE)).send(event.getChannel());
		//new MessageBuilder().setEmbed(new EmbedBuilder()
		//		.setTitle("Ronda 1").setDescription("El jugador @Tovarashi es el lider \n Necesitá coger dos jugadores").setFooter("Se escogen con: !team <@user> <@user> ..").setColor(Color.ORANGE)).send(event.getChannel());
		//new MessageBuilder().setEmbed(new EmbedBuilder()
		//		.setTitle("Ronda 1").setDescription("El jugador @Tovarashi es el lider \n Necesitá coger dos jugadores \n ||Se escogen con: !team <@user> <@user> ..||").setColor(Color.ORANGE)).send(event.getChannel());
//		event.getMessage().addReaction(THUMBSUP);
//		event.getMessage().addReaction(THUMBSDOWN);
//		event.getMessage().addReactionAddListener(emojiEvent -> {
//			emojiEvent.getMessage().ifPresent(message -> {
//				if(!emojiEvent.getUser().isBot()) {
//				emojiEvent.getEmoji().asUnicodeEmoji().ifPresent(emoji -> {
//					if(emoji.equals(THUMBSUP))
//						registerVote(game.checkPlayer(emojiEvent.getUser()), true);
//					else if(emoji.equals(THUMBSUP))
//						registerVote(game.checkPlayer(emojiEvent.getUser()), false);					
//					message.removeReactionByEmoji(emojiEvent.getUser(), emoji);
//					});			
//				}
//				});
//			});
		channel.sendMessage("!test");
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
		//TODO un formato mejor :)
		channel.sendMessage("Ronda: " + game.getRound());
		channel.sendMessage("Victorias Rebeldes: " + game.getNumVictoriasResistencia());
		channel.sendMessage("Victorias Espías: " + game.getNumVictoriasSpys());
		preProposeTeam();
	}

	/**
	 * Explica cuantos tienen que ir
	 */
	private void preProposeTeam() {
		//TODO mejorar formato
		//TODO actualizar el mensaje con nuevo lider
		new MessageBuilder().setEmbed(new EmbedBuilder()
				.setTitle("Ronda: "+ game.getRound())
				.setDescription( "Resistencia: " + game.getNumVictoriasResistencia()
						+ "Espías: " + game.getNumVictoriasSpys() + "\n"
						+"El jugador "+ game.getLeader().getUser().getMentionTag() +" es el lider \n"
						+ "Se necesitan escoger un total de: " + game.getNumPlayersForMission() + " jugadores" + "\n")
				.setFooter("Se escogen con:" + PROPOSETEAM +"@user @user...")
				.setColor(Color.ORANGE))
				.send(channel);				
		
//		String message = "";
//		message += "El jugador: " + game.getLeader().getUser().getMentionTag() + " es el lider\n";
//		int numJugadoresMision = game.getNumPlayersForMission();
//		message += "Necesita escoger un total de: " + numJugadoresMision + " jugadores" + "\n";
//		message += "Se escogen con: " + PROPOSETEAM + " <@user> <@user> ...";
//		channel.sendMessage(message);

	}
	
	/**
	 * TODO actualmente no se puede cambiar un voto
	 * @param player
	 * @param vote
	 */
	private void registerVote(Player player, boolean vote) {
		
		if(player != null) {
		// Codigo para no repetir votos
		boolean aux = false;
		for (Vote theVote : votos)// esto es poco optimo pero el juego es de maximo 10 jugadores so np
			if (theVote.getUser().equals(player.getUser())) {
				aux = true;// comprobar si el equals de user funciona
				System.out.println("voto repetido" + player.getUser().getDiscriminatedName());
			}
		if (!aux)
			votos.add(new Vote(player.getUser(), vote));
			System.out.println("Voto de "+player.getUser().getDiscriminatedName());
		// Comprobar si todos los votos estan hechos ya

		if (game.getPlayers().size() == votos.size()) {
			
			int numRechazados = 0;
			MessageBuilder mb = new MessageBuilder().append("Los resultados de los votos son:\n");

			for (Vote theVote : votos) {
				if (!theVote.isVoto())
					numRechazados++;
				mb.append(theVote + "\n");
			}
			int players = game.getPlayers().size();
			if(numRechazados > ((players%2 == 0)?players/2-1 :players/2)) {
				mb.append("Propuesta rechazada",MessageDecoration.BOLD);
				game.giveNextPlayerLeader();
				game.clearMissionParticipants();
				mb.send(channel).join();
				game.setState(Game.GameState.WAITPROPOSETEAM);
				preProposeTeam();//TODO logica de muchos planes rechazados
			}		
			else {
				mb.append("Propuesta aceptada",MessageDecoration.BOLD);
				mb.send(channel).join();
				game.setState(Game.GameState.WAITMISSION);
				mision();
			}
				
			
			
			//ya se puede limpiar la lista de votos
			votos.clear();			
		}
		}
		else {
			System.err.println("Player null ¿no se registro? ¿bug?");
		}
		
	}
	
	//TODO estaria bien no repetir codigo :)
	public void registerMissionAction(Player player, boolean vote) {
		// Codigo para no repetir votos
				boolean aux = false;
				for (Vote theVote : votos)// esto es poco optimo pero el juego es de maximo 10 jugadores so np
					if (theVote.getUser().equals(player.getUser()))
						aux = true;// comprobar si el equals de user funciona
				
				if (!aux)
					votos.add(new Vote(player.getUser(), vote));

				// Comprobar si todos los votos estan hechos ya
				
				System.err.println("Voto de mision");
				if (game.getMissionParticipants().size() == votos.size()) {
					System.err.println("Todos los votos computados");
					int numRechazados = 0;
					MessageBuilder mb = new MessageBuilder().append("Los resultados de la mision son:\n");
					boolean nextRound = false;	
					
					for (Vote theVote : votos) {
						if (!theVote.isVoto())
							numRechazados++;
										
					}
					mb.append("Sabotajes: "+numRechazados);
					if(numRechazados >= 1) {//TODO mirar la ronda
						if(game.giveSpyAWin()) {
							mb = new MessageBuilder().append("¡Victoria de los espias!",MessageDecoration.BOLD);
							for(Player thePlayer:game.getPlayers())
								mb.append(thePlayer.getUser().getMentionTag() + " es " + thePlayer.getRol());
							//TODO end game							
						}
						else {
							mb.append("Misión fracasada",MessageDecoration.BOLD);
							nextRound = true;
						}
					}
					else {
						if(game.giveResistanceAWin()) {
							mb = new MessageBuilder().append("¡Victoria de la resistencia!\n",MessageDecoration.BOLD);
							for(Player thePlayer:game.getPlayers())
								mb.append(thePlayer.getUser().getMentionTag() + " es " + thePlayer.getRol()+"\n");
							//TODO end game
						}							
						else {			
							mb.append("Misión exitosa",MessageDecoration.BOLD);
							nextRound = true;
						}
					}
					mb.send(channel);
					
					if(nextRound) {
						game.clearMissionParticipants();
						votos.clear();
						game.giveNextPlayerLeader();
						//inicioDeRonda();
						game.setState(Game.GameState.WAITPROPOSETEAM);
						preProposeTeam();
					}
						
					
					//ya se puede limpiar la lista de votos
					votos.clear();		
				}
	}

	private void mision() {
		for(Player thePlayer:game.getMissionParticipants())//TODO logica de la resistencia
			thePlayer.getUser().sendMessage("Reacciona a este mensaje con " + THUMBSUP + 
					" para que la misión sea un exito, o con "+ THUMBSDOWN + "para que sabotearla").
					thenAcceptAsync(message -> {
						message.addReaction(THUMBSUP);
						message.addReaction(THUMBSDOWN);									
						message.addReactionAddListener(emojiEvent -> {										
							emojiEvent.getEmoji().asUnicodeEmoji().ifPresent(emoji -> {
								if(emoji.equals(THUMBSUP) && !emojiEvent.getUser().isBot())
									registerMissionAction(thePlayer, true);
								else if(emoji.equals(THUMBSDOWN) && !emojiEvent.getUser().isBot())
									registerMissionAction(thePlayer, false);
								else if(!emojiEvent.getUser().isBot())
									thePlayer.getUser().sendMessage("No me intentes liar puto 😠");
								});
							});	
						
					});
		
	}

}
