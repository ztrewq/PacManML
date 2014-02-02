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
	
	public static float[] _getFeatures(Game game, int nodeIndex, MOVE move) {
		return extendFeatures(_getFeatures(game, nodeIndex, move));
	}
	
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
		
		float[] features = new float[11];
		features[0] = getSavePathLength(game, nodeIndex, move, 100);
		features[1] = getMinimumDistance(game, nodeIndex, move, game.getActivePillsIndices());
		features[2] = getMinimumDistance(game, nodeIndex, move, game.getActivePowerPillsIndices());
		features[3] = getMinimumDistance(game, nodeIndex, move, toPrimitiveArray(normalGhosts));
		features[4] = getMinimumDistance(game, nodeIndex, move, toPrimitiveArray(edibleGhosts));
		features[5] = completable(game, nodeIndex, move);
		features[6] = getRemainingEdibleTime(game);
		features[7] = getPillsInDirection(game, nodeIndex, move);
		features[8] = getJunctionDistance(game, nodeIndex, move);
		features[9] = getNumberOfSavePaths(game, nodeIndex, move, 60);
		features[10] = getMinimumDistance(game, game.getNeighbour(nodeIndex, move), move.opposite(), toPrimitiveArray(normalGhosts));
		
		return features;
	}
	
	public static float[] extendFeatures(float[] features) {
		float[] extendedFeatures = new float[19];
		for (int i = 0; i < features.length; i++) {
			extendedFeatures[i] = features[i];
		}
		
		extendedFeatures[11] = (1 - features[4]) * features[6]; // edible ghosts
		extendedFeatures[12] = Math.min(0, features[0] - features[8]); // safe path length after junction
		extendedFeatures[13] = Math.min(0, features[3] - features[8]); // danger value
		extendedFeatures[14] = Math.min(0, features[3] - features[2]); // distance difference ghost powerPill
		extendedFeatures[15] = Math.min(0, features[0] - features[1]);
		extendedFeatures[16] = Math.min(0, features[0] - features[2]);
		extendedFeatures[17] = Math.min(0, features[0] - features[3]);
		extendedFeatures[18] = Math.min(0, features[0] - features[4]);
		
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
	
	private static float getNumberOfSavePaths(Game game, int nodeIndex, MOVE initialMove, int depthLimit) {
		// for each living ghost get the path to next junction
		EnumMap<GHOST, int[]> ghostJunctionPaths = getGhostJunctionPaths(game);

		// initialize BFS
		LinkedList<BFSNode> frontier = new LinkedList<BFSNode>();
		frontier.add(new BFSNode(game.getNeighbour(nodeIndex, initialMove), nodeIndex, 1));
		int paths = 0;

		// BFS
		while (!frontier.isEmpty()) {
			BFSNode node = frontier.pop();
			
			// continue if node is not safely reachable
			if (reachableByGhost(game, node.nodeIndex, node.depth + EAT_DISTANCE, ghostJunctionPaths))
				continue;
			
			// reached depth limit
			if (node.depth == depthLimit) {
				paths++;
				continue;
			}

			// expand frontier with neighbor nodes
			for (MOVE move : game.getPossibleMoves(node.nodeIndex)) {
				int neighborNode = game.getNeighbour(node.nodeIndex, move);
				if (neighborNode != node.preNodeIndex)
					frontier.add(new BFSNode(neighborNode, node.nodeIndex, node.depth + 1));
			}
		}
		
		return (float) paths / 15;
	}
	
	/**
	 * get the distance to the next junction
	 */
	private static float getJunctionDistance(Game game, int nodeIndex, MOVE initialMove) {
		if (game.getNeighbour(nodeIndex, initialMove) == -1)
			throw new IllegalArgumentException("invalid move given");
		
		int[] pathToFirstJunction = getJunctionPath(game, game.getNeighbour(nodeIndex, initialMove), initialMove);
		return (float) pathToFirstJunction.length / MAX_DISTANCE;
	}
	
	/**
	 * get the number of pills in the given direction until a junction is reached
	 */
	private static float getPillsInDirection(Game game, int nodeIndex, MOVE initialMove) {
		int[] pathToJunction = getJunctionPath(game, nodeIndex, initialMove);
		int pills = 0;
		for (int node : pathToJunction) {
			int pillIndex = game.getPillIndex(node);
			int powerPillIndex = game.getPowerPillIndex(node);
			if (pillIndex != -1 && game.isPillStillAvailable(pillIndex) || powerPillIndex != -1 && game.isPowerPillStillAvailable(powerPillIndex))
				pills++;
		}
		
		return (float) pills / (game.getNumberOfPills() + game.getNumberOfPowerPills());
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
		EnumMap<GHOST, int[]> ghostJunctionPaths = getGhostJunctionPaths(game);

		// initialize BFS
		LinkedList<BFSNode> frontier = new LinkedList<BFSNode>();
		frontier.add(new BFSNode(game.getNeighbour(nodeIndex, initialMove), nodeIndex, 1));
		int maxDepth = 0;

		// BFS
		while (!frontier.isEmpty()) {
			BFSNode node = frontier.pop();
			
			// continue if node is not safely reachable
			if (reachableByGhost(game, node.nodeIndex, node.depth + EAT_DISTANCE, ghostJunctionPaths))
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
	
	private static float completable(Game game, int nodeIndex, MOVE initialMove) {
		EnumMap<GHOST, int[]> ghostJunctionPaths = getGhostJunctionPaths(game);
		int remainingPills = game.getNumberOfActivePills() + game.getNumberOfActivePowerPills();
		int safelyEdiblePills = 0;
		int[] pathToJunction = getJunctionPath(game, nodeIndex, initialMove);
		for (int i = 0; i < pathToJunction.length; i++) {
			if (!reachableByGhost(game, pathToJunction[i], i, ghostJunctionPaths)) {
				int pillIndex = game.getPillIndex(pathToJunction[i]);
				int powerPillIndex = game.getPowerPillIndex(pathToJunction[i]);
				if (pillIndex != -1 && game.isPillStillAvailable(pillIndex) || powerPillIndex != -1 && game.isPowerPillStillAvailable(powerPillIndex)) {
					safelyEdiblePills++;
				}
			}
			else {
				break;
			}
		}
		
		return safelyEdiblePills == remainingPills ? 1 : 0;
	}

	/**
	 * get the path to the next junction starting in nodeIndex taking the initial Move
	 */
	private static int[] getJunctionPath(Game game, int nodeIndex, MOVE initialMove) {
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
	 * get the path to the next junction for each living ghost
	 */
	private static EnumMap<GHOST, int[]> getGhostJunctionPaths(Game game) {
		EnumMap<GHOST, int[]> ghostJunctionPaths = new EnumMap<GHOST, int[]>(GHOST.class);
		for (GHOST ghost : GHOST.values()) {
			// ghost is alive
			if (game.getGhostLairTime(ghost) == 0) {
				// ghost did just left the lair
				if (game.getGhostLastMoveMade(ghost) == MOVE.NEUTRAL && game.getGhostCurrentNodeIndex(ghost) == game.getGhostInitialNodeIndex())
					ghostJunctionPaths.put(ghost, new int[] { game.getGhostCurrentNodeIndex(ghost) });
				// ghost did not just left the lair
				else
					ghostJunctionPaths.put(ghost, getJunctionPath(game, game.getGhostCurrentNodeIndex(ghost), game.getGhostLastMoveMade(ghost)));
			}
		}
		
		return ghostJunctionPaths;
	}
	
	/**
	 * test if nodeIndex can be reached by any ghost within the time limit
	 */
	private static boolean reachableByGhost(Game game, int nodeIndex, int timeLimit, EnumMap<GHOST, int[]> ghostJunctionPaths) {
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
				int[] junctionPath = ghostJunctionPaths.get(ghost);
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