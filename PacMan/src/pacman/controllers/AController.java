package pacman.controllers;

import pacman.game.Game;
import pacman.game.Constants.MOVE;

public abstract class AController extends Controller<MOVE>{

	public abstract MOVE getMove(Game game, long timeDue);
	
	public abstract float[] getPolicyParameters();
	
	public abstract void setPolicyParameters(float[] parameters);
	
	public abstract AController copy();
	
}
