package resistance;

import org.javacord.api.entity.user.User;

public class Vote {
	private User user;
	private boolean voto;//TRUE -> aceptada/success FALSE -> rechazada/saboteada
	
	
	public Vote(User user, boolean voto) {
		super();
		this.user = user;
		this.voto = voto;
	}
	public User getUser() {
		return user;
	}
	public boolean isVoto() {
		return voto;
	}
	
	
	/**
	 * The users ids must be identical
	 */
	@Override
	public boolean equals(Object o) {
		if(o instanceof User) {
			if(((User)o).getId() == this.user.getId())
				return true;
		}
		return false;
	}
	@Override
	public String toString() {
		String toReturn =  user.getMentionTag();
		if(voto)
			return toReturn + " aceptó la propuesta";
		else
			return toReturn + " rechazó la propuesta";
	}
	
	
}
