package pacman.controllers;

import pacman.game.Game;
import pacman.game.Constants.MOVE;
import pacman.utils.Vector;

public abstract class AController extends Controller<MOVE>{

	public abstract MOVE getMove(Game game, long timeDue);
	
	public abstract Vector getPolicyParameters();
	
	public abstract void setPolicyParameters(Vector parameters);
	
	public abstract AController copy();
	
}
