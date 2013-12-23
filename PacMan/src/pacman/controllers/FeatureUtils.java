package pacman.controllers;

import static pacman.game.Constants.EAT_DISTANCE;
import java.util.EnumMap;
import java.util.LinkedList;
import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class FeatureUtils {

	private static final int MAX_DISTANCE = 221;
	private static final int DEPTH_LIMIT = MAX_DISTANCE;
	

	/**
	 * get the features vector relative to nodeIndex and move
	 */
	public static float[] getFeatures(Game game, int nodeIndex, MOVE move) {
		LinkedList<Integer> normalGhosts = new LinkedList<Integer>();
		LinkedList<Integer> edibleGhosts = new LinkedList<Integer>();
		
		for (GHOST ghost : GHOST.values()) {
			if (game.getGhostLairTime(ghost) == 0) {
				int ghostNodeIndex = game.getGhostCurrentNodeIndex(ghost);
				if (game.getGhostEdibleTime(ghost) == 0)
					normalGhosts.add(ghostNodeIndex);
				else
					edibleGhosts.add(ghostNodeIndex);
			}
		}
		
		float[] features = new float[8];
		features[0] = getMinimumDistance(game, nodeIndex, move, game.getActivePillsIndices());
		features[1] = getMinimumDistance(game, nodeIndex, move, game.getActivePowerPillsIndices());
		features[2] = getMinimumDistance(game, nodeIndex, move, toPrimitiveArray(normalGhosts));
		features[3] = getMinimumDistance(game, nodeIndex, move, toPrimitiveArray(edibleGhosts));
		features[4] = getRemainingEdibleTime(game);
		features[5] = getSavePathLength(game, nodeIndex, move);
		features[6] = getRemainingNumberOfPills(game);
		features[7] = getRemainingGameTime(game);
		
		return features;
	}
	
	/**
	 * convert a list of Integers into a primitive array
	 */
	private static int[] toPrimitiveArray(LinkedList<Integer> integerList) {
	    int[] integers = new int[integerList.size()];
	    int i = 0;
	    for (int x : integerList)
	        integers[i++] = x;

	    return integers;
	}
	
	/**
	 * get the minimum path distance needed to reach any node in goalNodeIndices starting in startNodeIndex and taking the initialMove
	 */
	private static float getMinimumDistance(Game game, int startNodeIndex, MOVE initialMove, int[] goalNodeIndices) {
		if (game.getNeighbour(startNodeIndex, initialMove) == -1)
			throw new IllegalArgumentException("invalid move given");
		
		// skip if there are no goal nodes
		if (goalNodeIndices.length != 0) {
			// test startNodeIndex
			for (int goalNodeIndex : goalNodeIndices) {
				if (startNodeIndex == goalNodeIndex)
					return 0;
			}
			
			// BFS
			LinkedList<BFSNode> frontier = new LinkedList<BFSNode>();
			frontier.add(new BFSNode(game.getNeighbour(startNodeIndex, initialMove), startNodeIndex, 1));
			
			while (!frontier.isEmpty()) {
				BFSNode node = frontier.pop();
				
				// test if any goal is reached
				for (int goalNodeIndex : goalNodeIndices) {
					if (node.nodeIndex == goalNodeIndex)
						return (float) node.depth / MAX_DISTANCE;
				}
				
				// don't expand further if depth limit is reached
				if (node.depth == DEPTH_LIMIT)
					continue;
	
				// expand frontier with neighbor nodes
				for (MOVE move : game.getPossibleMoves(node.nodeIndex)) {
					int neighborNode = game.getNeighbour(node.nodeIndex, move);
					if (neighborNode != node.preNodeIndex)
						frontier.add(new BFSNode(neighborNode, node.nodeIndex, node.depth + 1));
				}
			}
		}
		
		return 1;
	}
	
	/**
	 * get the remaining edible time for all edible ghosts
	 */
	private static float getRemainingEdibleTime(Game game) {
		int time = 0;
		for (GHOST ghost : GHOST.values()) {
			if (game.getGhostEdibleTime(ghost) > 0) {
				time = game.getGhostEdibleTime(ghost);
				break;
			}
		}
		
		return (float) time / Constants.EDIBLE_TIME;
	}

	/**
	 * get the length of the longest save path
	 */
	private static float getSavePathLength(Game game, int nodeIndex, MOVE initialMove) {
		// for each living ghost get the path to next junction
		EnumMap<GHOST, int[]> firstJunctionPaths = new EnumMap<GHOST, int[]>(GHOST.class);
		for (GHOST ghost : GHOST.values()) {
			// ghost is alive
			if (game.getGhostLairTime(ghost) == 0) {
				// ghost did just left the lair
				if (game.getGhostLastMoveMade(ghost) == MOVE.NEUTRAL && game.getGhostCurrentNodeIndex(ghost) == game.getGhostInitialNodeIndex())
					firstJunctionPaths.put(ghost, new int[] { game.getGhostCurrentNodeIndex(ghost) });
				// ghost did not just left the lair
				else
					firstJunctionPaths.put(ghost, getPathToNextJunction(game, game.getGhostCurrentNodeIndex(ghost), game.getGhostLastMoveMade(ghost)));
			}
		}

		// initialize BFS
		LinkedList<BFSNode> frontier = new LinkedList<BFSNode>();
		frontier.add(new BFSNode(game.getNeighbour(nodeIndex, initialMove), nodeIndex, 0));
		int maxDepth = 0;
		int depthLimit = 160;

		// BFS
		while (!frontier.isEmpty()) {
			BFSNode node = frontier.pop();
			
			if (reachableByGhost(game, node.nodeIndex, node.depth + EAT_DISTANCE, firstJunctionPaths))
				continue;
			
			if (node.depth == depthLimit)
				return (float) node.depth / MAX_DISTANCE;

			maxDepth = Math.max(maxDepth, node.depth);
			
			// expand frontier with neighbor nodes
			for (MOVE move : game.getPossibleMoves(node.nodeIndex)) {
				int neighborNode = game.getNeighbour(node.nodeIndex, move);
				if (neighborNode != node.preNodeIndex)
					frontier.add(new BFSNode(neighborNode, node.nodeIndex, node.depth + 1));
			}
		}
		
		return (float) maxDepth / MAX_DISTANCE;
	}
	
	/**
	 * get the scaled (0 to 1) number of remaining pills.
	 * returns 1 if no pill has been collected so far.
	 */
	private static float getRemainingNumberOfPills(Game game) {
		return (float)game.getNumberOfActivePills() / game.getNumberOfPills();
	}
	
	/**
	 * get the scaled (0 to 1) remaining game time
	 */
	private static float getRemainingGameTime(Game game) {
		return (float) (Constants.LEVEL_LIMIT - game.getCurrentLevelTime()) / Constants.LEVEL_LIMIT;
	}

	/**
	 * get the path to the next junction starting in nodeIndex taking the initial Move
	 */
	private static int[] getPathToNextJunction(Game game, int nodeIndex, MOVE initialMove) {
		// no move
		if (initialMove == MOVE.NEUTRAL)
			throw new IllegalArgumentException("move must not be NEUTRAL");
		
		// nodeIndex is a junction
		if (game.isJunction(nodeIndex))
			return new int[] {nodeIndex};
					
		// impossible move was given
		if (game.getNeighbour(nodeIndex, initialMove) == -1) {
			MOVE[] moves = game.getPossibleMoves(nodeIndex);
			// no turn is possible following the given move
			if (moves[0] != initialMove.opposite() && moves[1] != initialMove.opposite())
				throw new IllegalArgumentException("invalid move given");
			// take turn
			else
				initialMove = game.getPossibleMoves(nodeIndex, initialMove)[0];
		}
		
		// initialize path traversal
		LinkedList<Integer> pathNodeList = new LinkedList<Integer>();
		pathNodeList.add(nodeIndex);
		
		MOVE moveDirection = initialMove;
		int currNodeIndex = nodeIndex;
		
		// follow path until junction is reached
		while (!game.isJunction(currNodeIndex)) {
			// reached a turn
			if (game.getNeighbour(currNodeIndex, moveDirection) == -1)
				moveDirection = game.getPossibleMoves(currNodeIndex, moveDirection)[0];
			
			currNodeIndex = game.getNeighbour(currNodeIndex, moveDirection);
			pathNodeList.add(currNodeIndex);
		}
		
		int[] path = new int[pathNodeList.size()];
		for (int i = 0; i < path.length; i++)
			path[i] = pathNodeList.pop();
		
		return path;
	}
	
	/**
	 * test if nodeIndex can be reached by any ghost within the time limit
	 */
	private static boolean reachableByGhost(Game game, int nodeIndex, int timeLimit, EnumMap<GHOST, int[]> firstJunctionPaths) {
		for (GHOST ghost : GHOST.values()) {
			// ghost is inside the lair
			if (game.getGhostLairTime(ghost) > 0) {
				int releaseTimeGhost = game.getGhostLairTime(ghost);
				int distanceGhostToNode = game.getShortestPathDistance(game.getGhostInitialNodeIndex(), nodeIndex);
				if (releaseTimeGhost + distanceGhostToNode <= timeLimit)
					return true;
			}
			
			// ghost is alive
			else if (game.getGhostLairTime(ghost) == 0) {
				int[] junctionPath = firstJunctionPaths.get(ghost);
				int distanceToJunction = junctionPath.length - 1;
				int distanceJunctionToNode = game.getShortestPathDistance(junctionPath[junctionPath.length - 1], nodeIndex);
				int edibleTime = game.getGhostEdibleTime(ghost);
				
				// ghost is not edible
				if (edibleTime == 0) {					
					// test path to first junction
					for (int i = 0; i < junctionPath.length; i++) {
						if (junctionPath[i] == nodeIndex) {
							if (i <= timeLimit)
								return true;
						}
					}
					
					// test total needed time
					int totalTime = distanceToJunction + distanceJunctionToNode; 
					if (totalTime <= timeLimit)
						return true;
				}
			}
		}
		
		return false;
	}

	
	/**
	 * a node used for breadth first search
	 */
	private static class BFSNode {
		private final int nodeIndex;
		private final int preNodeIndex;
		private final int depth;
		
		private BFSNode(int nodeIndex, int preNodeIndex, int depth) {
			this.nodeIndex = nodeIndex;
			this.preNodeIndex = preNodeIndex;
			this.depth = depth;
		}
	}
}