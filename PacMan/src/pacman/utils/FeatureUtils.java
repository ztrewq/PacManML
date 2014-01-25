package pacman.utils;

import static pacman.game.Constants.EAT_DISTANCE;

import java.util.EnumMap;
import java.util.LinkedList;

import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class FeatureUtils {

	private static final int MAX_DISTANCE = 221;
	
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
		
		float[] features = new float[10];
		features[0] = getSavePathLength(game, nodeIndex, move, 125);
		features[1] = getMinimumDistance(game, nodeIndex, move, game.getActivePillsIndices());
		features[2] = getMinimumDistance(game, nodeIndex, move, game.getActivePowerPillsIndices());
		features[3] = getMinimumDistance(game, nodeIndex, move, toPrimitiveArray(normalGhosts));
		features[4] = getMinimumDistance(game, nodeIndex, move, toPrimitiveArray(edibleGhosts));
		features[5] = getRemainingNumberOfPills(game);
		features[6] = move == game.getPacmanLastMoveMade().opposite() ? 1 : 0; // 1 if reversed
		features[7] = getRemainingEdibleTime(game);
		features[8] = getPillsInDirection(game, nodeIndex, move, 25);
		features[9] = getJunctionDistance(game, nodeIndex, move);
		
		return features;
	}
	
	public static float[] getExtendedFeatures(float[] features) {
		float[] extendedFeatures = new float[19];
		for (int i = 0; i < features.length; i++) {
			extendedFeatures[i] = features[i];
		}
		
		extendedFeatures[10] = (1 - features[4]) * features[7]; // ghost eating progress
		extendedFeatures[11] = features[0] - features[9]; // safe path length after junction #
		extendedFeatures[12] = features[3] - features[9]; // danger value #
		extendedFeatures[13] = features[5] - features[8]; // possible eat progress
		extendedFeatures[14] = features[3] - features[2]; // distance difference ghost powerPill
		extendedFeatures[15] = features[0] - features[1];
		extendedFeatures[16] = features[0] - features[2];
		extendedFeatures[17] = features[0] - features[3];
		extendedFeatures[18] = features[0] - features[4];
		
		return extendedFeatures;
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
	
	private static float getJunctionDistance(Game game, int nodeIndex, MOVE initialMove) {
		if (game.getNeighbour(nodeIndex, initialMove) == -1)
			throw new IllegalArgumentException("invalid move given");
		
		int[] pathToFirstJunction = getPathToJunction(game, game.getNeighbour(nodeIndex, initialMove), initialMove);
		return (float) pathToFirstJunction.length / MAX_DISTANCE;
	}
	
	/**
	 * get the length of the longest save path
	 */
	private static float getPillsInDirection(Game game, int nodeIndex, MOVE initialMove, int depthLimit) {
		// initialize BFS
		LinkedList<BFSNode> frontier = new LinkedList<BFSNode>();
		frontier.add(new BFSNode(game.getNeighbour(nodeIndex, initialMove), nodeIndex, 1));
		
		int pillsAndPowerPills = 0;
		
		// BFS
		while (!frontier.isEmpty()) {
			BFSNode node = frontier.pop();
			
			if (game.isPillStillAvailable(node.nodeIndex) || game.isPowerPillStillAvailable(node.nodeIndex))
				pillsAndPowerPills++;
			
			if (node.depth == depthLimit)
				continue;

			// expand frontier with neighbor nodes
			for (MOVE move : game.getPossibleMoves(node.nodeIndex)) {
				int neighborNode = game.getNeighbour(node.nodeIndex, move);
				if (neighborNode != node.preNodeIndex) {
					frontier.add(new BFSNode(neighborNode, node.nodeIndex, node.depth + 1));
				}
			}
		}
		
		return (float) pillsAndPowerPills / (game.getNumberOfPills() + game.getNumberOfPowerPills());
	}
	
	/**
	 * get the minimum path distance needed to reach any node in goalNodeIndices starting in startNodeIndex and taking the initialMove.
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
			
			// breadth first search
			LinkedList<BFSNode> frontier = new LinkedList<BFSNode>();
			frontier.add(new BFSNode(game.getNeighbour(startNodeIndex, initialMove), startNodeIndex, 1));
			
			while (!frontier.isEmpty()) {
				BFSNode node = frontier.pop();
				
				// reached startNode again
				if (node.nodeIndex == startNodeIndex) {
					// push back current node
					frontier.push(node);
					
					// iterate over frontier
					int minDist = MAX_DISTANCE;
					while (!frontier.isEmpty()) {
						node = frontier.pop();
						for (int goalNodeIndex : goalNodeIndices) {
							int distNodeToGoal = game.getShortestPathDistance(node.nodeIndex, goalNodeIndex);
							if (distNodeToGoal != -1) {
								minDist = Math.min(minDist, node.depth + distNodeToGoal);
							}
						}
					}
					
					return (float) minDist / MAX_DISTANCE;
				}
				
				// test if any goal is reached
				for (int goalNodeIndex : goalNodeIndices) {
					if (node.nodeIndex == goalNodeIndex)
						return (float) node.depth / MAX_DISTANCE;
				}
				
				// don't expand further if depth limit is reached
				if (node.depth == MAX_DISTANCE)
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
	private static float getSavePathLength(Game game, int nodeIndex, MOVE initialMove, int depthLimit) {
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
					firstJunctionPaths.put(ghost, getPathToJunction(game, game.getGhostCurrentNodeIndex(ghost), game.getGhostLastMoveMade(ghost)));
			}
		}

		// initialize BFS
		LinkedList<BFSNode> frontier = new LinkedList<BFSNode>();
		frontier.add(new BFSNode(game.getNeighbour(nodeIndex, initialMove), nodeIndex, 0));
		int maxDepth = 0;

		// BFS
		while (!frontier.isEmpty()) {
			BFSNode node = frontier.pop();

			// continue if node is not safely reachable
			if (reachableByGhost(game, node.nodeIndex, node.depth + EAT_DISTANCE, firstJunctionPaths))
				continue;
			
			// don't expand further if depth limit is reached
			if (node.depth == depthLimit)
				return (float) node.depth / MAX_DISTANCE;

			maxDepth = node.depth;
			
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
		return (float) (game.getNumberOfActivePills() + game.getNumberOfActivePowerPills()) / (game.getNumberOfPills() + game.getNumberOfPowerPills());
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
	private static int[] getPathToJunction(Game game, int nodeIndex, MOVE initialMove) {
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