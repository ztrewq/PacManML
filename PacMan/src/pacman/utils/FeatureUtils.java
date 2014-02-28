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
	public static Vector getFeatures(Game game, int nodeIndex, MOVE move) {
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

		double[] features = new double[12];
		features[0] = getSavePathLength(game, nodeIndex, move, 110);
		features[1] = getNumberOfSavePaths(game, nodeIndex, move, 50);
		features[2] = getJunctionDistance(game, nodeIndex, move);
		features[3] = getPillsInDirection(game, nodeIndex, move);
		features[4] = getMinimumDistance(game, nodeIndex, move, game.getActivePillsIndices());
		features[5] = getMinimumDistance(game, nodeIndex, move, game.getActivePowerPillsIndices());
		features[6] = completable(game, nodeIndex, move);
		features[7] = getMinimumDistance(game, nodeIndex, move, toArray(edibleGhosts));
		features[8] = getRemainingEdibleTime(game);
		features[9] = getMinimumDistance(game, nodeIndex, move, toArray(normalGhosts));
		features[10] = getMinimumDistance(game, game.getNeighbour(nodeIndex, move), move.opposite(), toArray(normalGhosts));
		features[11] = move == game.getPacmanLastMoveMade().opposite() ? 1 : 0;

		return new Vector(features);
	}

	public static Vector extendFeatures(Vector features) {
		double[] extendedFeatures = new double[29];
		int index = 0;

		// copy old features
		for (double feature : features.getValues()) {
			extendedFeatures[index++] = feature;
		}

		// add squared value of features 0-5 and 7-9
		for (int j = 0; j <= 5; j++)
			extendedFeatures[index++] = features.getAt(j) * features.getAt(j);
		for (int j = 7; j <= 9; j++)
			extendedFeatures[index++] = features.getAt(j) * features.getAt(j);

		// add some further featuers
		extendedFeatures[index++] = (1 - features.getAt(7)) * features.getAt(8); // edible ghosts
		extendedFeatures[index++] = features.getAt(0) - features.getAt(2); // safe path length after junction
		extendedFeatures[index++] = features.getAt(9) - features.getAt(2); // danger value
		extendedFeatures[index++] = features.getAt(9) - features.getAt(5); // distance difference ghost powerPill
		extendedFeatures[index++] = features.getAt(0) - features.getAt(4);
		extendedFeatures[index++] = features.getAt(0) - features.getAt(5);
		extendedFeatures[index++] = features.getAt(0) - features.getAt(7);
		extendedFeatures[index++] = features.getAt(0) - features.getAt(9);

		return new Vector(extendedFeatures);
	}

	/**
	 * convert a list of Integers into an array
	 */
	public static int[] toArray(LinkedList<Integer> integerList) {
	    int[] integers = new int[integerList.size()];
	    int i = 0;
	    for (int x : integerList)
	        integers[i++] = x;

	    return integers;
	}

	public static double getNumberOfSavePaths(Game game, int nodeIndex, MOVE initialMove, int depthLimit) {
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

		return (double) paths / 15;
	}

	/**
	 * get the distance to the next junction
	 */
	public static double getJunctionDistance(Game game, int nodeIndex, MOVE initialMove) {
		if (game.getNeighbour(nodeIndex, initialMove) == -1)
			throw new IllegalArgumentException("invalid move given");

		int[] pathToFirstJunction = getJunctionPath(game, game.getNeighbour(nodeIndex, initialMove), initialMove);
		return (double) pathToFirstJunction.length / MAX_DISTANCE;
	}

	/**
	 * get the number of pills in the given direction until a junction is reached
	 */
	public static double getPillsInDirection(Game game, int nodeIndex, MOVE initialMove) {
		int[] pathToJunction = getJunctionPath(game, nodeIndex, initialMove);
		int pills = 0;
		for (int node : pathToJunction) {
			int pillIndex = game.getPillIndex(node);
			int powerPillIndex = game.getPowerPillIndex(node);
			if (pillIndex != -1 && game.isPillStillAvailable(pillIndex) || powerPillIndex != -1 && game.isPowerPillStillAvailable(powerPillIndex))
				pills++;
		}

		return (double) pills / (game.getNumberOfPills() + game.getNumberOfPowerPills());
	}
	
	/**
	 * @return 	is pacman faster to the next junction compared to the ghosts
	 */
	public static boolean anyGhostFasterToJunction(Game game, int nodeIndex, MOVE move){
		double savePath = getSavePathLength(game, nodeIndex, move, 110);
		double junctionDist = getJunctionDistance(game, nodeIndex, move);
		if(savePath <= junctionDist){
			return true;
		}
		return false;
	}
	
	/**
	 * get the minimum path distance needed to reach any node in goalNodeIndices starting in startNodeIndex and taking the initialMove.
	 */
	public static double getMinimumDistance(Game game, int startNodeIndex, MOVE initialMove, int[] goalNodeIndices) {
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

					return (double) minDist / MAX_DISTANCE;
				}

				// test if any goal is reached
				for (int goalNodeIndex : goalNodeIndices) {
					if (node.nodeIndex == goalNodeIndex)
						return (double) node.depth / MAX_DISTANCE;
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
	public static double getRemainingEdibleTime(Game game) {
		int time = 0;
		for (GHOST ghost : GHOST.values()) {
			if (game.getGhostEdibleTime(ghost) > 0) {
				time = game.getGhostEdibleTime(ghost);
				break;
			}
		}

		return (double) time / Constants.EDIBLE_TIME;
	}

	/**
	 * get the length of the longest save path
	 */
	public static double getSavePathLength(Game game, int nodeIndex, MOVE initialMove, int depthLimit) {
		// assure a general depth limit
		depthLimit = Math.min(depthLimit, MAX_DISTANCE);

		// get junction paths for pacMan and ghosts
		EnumMap<GHOST, int[]> ghostJunctionPaths = getGhostJunctionPaths(game);
		int[] myJunctionPath = getJunctionPath(game, nodeIndex, initialMove);
		int k = Math.max(myJunctionPath.length, getMaxGhostJunctionPathLength(game, ghostJunctionPaths));

		// initialize BFS
		LinkedList<BFSNode> frontier = new LinkedList<BFSNode>();
		frontier.add(new BFSNode(game.getNeighbour(nodeIndex, initialMove), nodeIndex, 1));
		int maxDepth = 0;

		// node BFS up to depth of k + 1
		while (!frontier.isEmpty()) {
			if (frontier.getFirst().depth > k)
				break;

			// get current node
			BFSNode node = frontier.pop();

			// skip if node is not safely reachable
			if (reachableByGhost(game, node.nodeIndex, node.depth + EAT_DISTANCE, ghostJunctionPaths))
				continue;

			// don't expand further if depth limit is reached
			if (node.depth >= depthLimit)
				return (double) node.depth / MAX_DISTANCE;

			// update maxDepth
			maxDepth = node.depth;

			// expand frontier with neighbor nodes
			for (MOVE move : game.getPossibleMoves(node.nodeIndex)) {
				int neighborNode = game.getNeighbour(node.nodeIndex, move);
				if (neighborNode != node.preNodeIndex)
					frontier.add(new BFSNode(neighborNode, node.nodeIndex, node.depth + 1));
			}
		}

		// remove every not safely reachable node in order to assure
		// the invariant of the succeeding loop 
		{
			LinkedList<BFSNode> temp = new LinkedList<FeatureUtils.BFSNode>();
			while (!frontier.isEmpty()) {
				BFSNode node = frontier.pop();
				if (!reachableByGhost(game, node.nodeIndex, node.depth + EAT_DISTANCE, ghostJunctionPaths))
					temp.add(node);
			}
			frontier.addAll(temp);
		}

		// junction BFS
		while (!frontier.isEmpty()) {
			BFSNode node = frontier.pop();

			// don't expand further if depth limit is reached
			if (node.depth > depthLimit)
				return (double) depthLimit / MAX_DISTANCE;

			// expand frontier
			for (MOVE move : game.getPossibleMoves(node.nodeIndex)) {
				if (game.getNeighbour(node.nodeIndex, move) != node.preNodeIndex) {
					int[] junctionPath = getJunctionPath(game, game.getNeighbour(node.nodeIndex, move), move);
					int junctionNode = junctionPath[junctionPath.length - 1];
					int preJunctionNode = junctionPath.length > 1 ? junctionPath[junctionPath.length - 2] : node.nodeIndex;
					int distToJunctionNode = node.depth + junctionPath.length;

					// expand frontier with safely reachable neighbor junction
					if (!reachableByGhost(game, junctionNode, distToJunctionNode + EAT_DISTANCE, ghostJunctionPaths))
						frontier.add(new BFSNode(junctionNode, preJunctionNode, distToJunctionNode));

					// get depth of farthest reachable node
					else {
						int depth = 0;
						for (int i = 0; i < junctionPath.length; i++) {
							if (!reachableByGhost(game, junctionPath[i], node.depth + i + 1 + EAT_DISTANCE, ghostJunctionPaths))
								depth = i + 1;
							else
								break;
						}

						maxDepth = Math.max(maxDepth, node.depth + depth);
					}
				}
			}
		}

		maxDepth = Math.min(maxDepth, depthLimit);
		return (double) maxDepth / MAX_DISTANCE;
	}

	/**
	 * test if the current level is completable by following the path to the next junction
	 */
	public static double completable(Game game, int nodeIndex, MOVE initialMove) {
		EnumMap<GHOST, int[]> ghostJunctionPaths = getGhostJunctionPaths(game);
		int remainingPills = game.getNumberOfActivePills() + game.getNumberOfActivePowerPills();
		int safelyEdiblePills = 0;
		int[] pathToJunction = getJunctionPath(game, nodeIndex, initialMove);
		for (int i = 0; i < pathToJunction.length; i++) {
			if (!reachableByGhost(game, pathToJunction[i], i + EAT_DISTANCE, ghostJunctionPaths)) {
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
	public static int[] getJunctionPath(Game game, int nodeIndex, MOVE initialMove) {
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
	public static EnumMap<GHOST, int[]> getGhostJunctionPaths(Game game) {
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
	 * get length of the longest ghost junction path
	 */
	public static int getMaxGhostJunctionPathLength(Game game, EnumMap<GHOST, int[]> ghostJunctionPaths) {
		int max = 0;
		for (GHOST ghost : GHOST.values()) {
			if (game.getGhostLairTime(ghost) == 0) {
				int[] junctionPath = ghostJunctionPaths.get(ghost);
				max = Math.max(max, junctionPath.length);
			}
		}
		return max;
	}

	/**
	 * test if nodeIndex can be reached by any ghost within the time limit
	 */
	public static boolean reachableByGhost(Game game, int nodeIndex, int timeLimit, EnumMap<GHOST, int[]> ghostJunctionPaths) {
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
	public static class BFSNode {
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