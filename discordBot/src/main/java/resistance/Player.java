package resistance;

import org.javacord.api.entity.user.User;

public class Player {
	
	private User user;
	private Game.Rol rol;
	
	public Player(User user) {
		//private 
		this.user = user;
	}
	
	public User getUser() {
		return user;
	}
	public Game.Rol getRol(){
		return rol;
		
	}
	
	protected void setRol(Game.Rol rol) {
		this.rol = rol;
	}
	

}
