package com.brianstempin.vindiniumclient.bot.simple;

import com.brianstempin.vindiniumclient.bot.BotMove;
import com.brianstempin.vindiniumclient.bot.BotUtils;
import com.brianstempin.vindiniumclient.bot.advanced.Vertex;
import com.brianstempin.vindiniumclient.dto.GameState;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by bstempi on 9/22/14.
 */
public class MurderBot implements SimpleBot {

	private static Logger logger;
	static Vertex me;

	public MurderBot() {
		logger = Logger.getLogger("murderbot");
	}

	/**
	 * Does Dijkstra and returns the a 1d structure that can be treated as 2d
	 *
	 * @param board
	 * @param hero
	 * @return
	 */
	private static List<Vertex> doDijkstra(List<Vertex> vertexes) {

		// Zok, we have a graph constructed. Traverse.
		PriorityQueue<Vertex> vertexQueue = new PriorityQueue<Vertex>();
		vertexQueue.add(me);
		me.setMinDistance(0);

		while (!vertexQueue.isEmpty()) {
			Vertex v = vertexQueue.poll();
			double distance = v.getMinDistance() + 1;

			for (Vertex neighbor : v.getAdjacencies()) {
				if (distance < neighbor.getMinDistance()) {
					neighbor.setMinDistance(distance);
					neighbor.setPrevious(v);
					vertexQueue.remove(neighbor);
					vertexQueue.add(neighbor);
				}
			}
		}

		return vertexes;
	}

	public static List<Vertex> doAStar(List<Vertex> vertexes) {
		Vertex nearestVertex = null;
		List<Vertex> openList = new LinkedList<Vertex>();
		List<Vertex> closedList = new LinkedList<Vertex>();
		openList.add(me);
		Vertex current = null;

		current = lowestFInOpen(openList);
		closedList.add(current);
		openList.add(current);
		List<Vertex> adjvertex = current.getAdjacencies();

		for (int i = 0; i < adjvertex.size(); i++) {
			Vertex currentAdj = adjvertex.get(i);
			if (!openList.contains(currentAdj)) {
				currentAdj.setPrevious(current);
				currentAdj.setH(me);
				currentAdj.setG(current);
				openList.add(currentAdj);
			} else {
				if (currentAdj.getG() > currentAdj.calculategCosts(current)) {
					currentAdj.setPrevious(current);
					currentAdj.setG(current);
				}
			}
		}

		while (!openList.isEmpty()) {
			current = lowestFInOpen(openList);

			closedList.add(current);
			openList.remove(current);
			if ((current.getPosition().getX() == me.getPosition().getX())
					&& (current.getPosition().getY() == me.getPosition().getY())) {
				return vertexes;
			}
			adjvertex = current.getAdjacencies();

			for (int i = 0; i < adjvertex.size(); i++) {
				Vertex currentAdj = adjvertex.get(i);
				if (!openList.contains(currentAdj)) {
					currentAdj.setPrevious(current);
					currentAdj.setH(me);
					currentAdj.setG(current);
					openList.add(currentAdj);
				} else {
					if (currentAdj.getG() > currentAdj.calculategCosts(current)) {
						currentAdj.setPrevious(current);
						currentAdj.setG(current);
					}
				}
			}

		}

		return vertexes;
	}

	private static Vertex lowestFInOpen(List<Vertex> openList) {
		// TODO currently, this is done by going through the whole openList!
		Vertex cheapest = openList.get(0);
		for (int i = 0; i < openList.size(); i++) {
			if (openList.get(i).getF() < cheapest.getF()) {
				cheapest = openList.get(i);
			}
		}
		return cheapest;
	}

	private static List<Vertex> calcPath(Vertex start, Vertex goal) {
		LinkedList<Vertex> path = new LinkedList<Vertex>();
		Vertex curr = goal;
		boolean done = false;
		while (!done) {
			path.addFirst(curr);
			curr = (Vertex) curr.getPrevious();
			if (curr.equals(start)) {
				done = true;
			}
		}
		return path;
	}

	private static List<Vertex> Dijikstra_getPath(Vertex target) {
		List<Vertex> path = new LinkedList<Vertex>();

		path.add(target);
		Vertex next = target;
		while (next.getPrevious().getMinDistance() != 0) {
			path.add(next.getPrevious());
			next = next.getPrevious();
		}

		Collections.reverse(path);
		return path;
	}

	private static List<Vertex> Astar_getPath(Vertex target) {
		List<Vertex> path = new LinkedList<Vertex>();
		// logger.info("Path from " + me.getPosition().getX() + "," +
		// me.getPosition().getY() + " to "
		// + target.getPosition().getY() + "," + target.getPosition().getY() + "
		// is ");
		path.add(target);
		Vertex next = target;
		while (next.getPrevious().getH() != 0) {
			path.add(next.getPrevious());
			next = next.getPrevious();
		}

		Collections.reverse(path);

		// for (Vertex v : path)
		// logger.info(v.getPosition().getX() + "," + v.getPosition().getY());
		return path;
	}

	private boolean runAwayMode = false;
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public BotMove move(GameState gameState) {
		logger.info("Starting move");

		// Initialize the map
		List<Vertex> vertexes = initialising(gameState.getGame().getBoard(), gameState.getHero());

		// Dijkstra to find the distance from the hero to all the other nodes
		vertexes = doDijkstra(vertexes);

		// logger.info("ID = " + gameState.getGame().getId());
		// logger.info("size = " + gameState.getGame().getBoard().getSize());
		// logger.info("\n" + gameState.getGame().getBoard().getTiles());

		Vertex closestPub = null;
		Vertex closestPlayer = null;
		Vertex closestMine = null;

		// get the closest pub,mine and player using the minDistance from
		// dijkstra
		for (Vertex v : vertexes) {
			if (v.getTileType().startsWith("@") && v.getMinDistance() != 0
					&& v.getMinDistance() != Double.POSITIVE_INFINITY
					&& (closestPlayer == null || closestPlayer.getMinDistance() > v.getMinDistance()))
				closestPlayer = v;
			else if (v.getTileType().equals("[]") && v.getMinDistance() != Double.POSITIVE_INFINITY
					&& (closestPub == null || closestPub.getMinDistance() > v.getMinDistance()))
				closestPub = v;
			else if (v.getTileType().startsWith("$") && v.getMinDistance() != 0
					&& v.getMinDistance() != Double.POSITIVE_INFINITY
					&& (closestMine == null || closestMine.getMinDistance() > v.getMinDistance()))
				closestMine = v;
		}

		logger.info("closest pub is :" + closestPub.getPosition().getX() + "," + closestPub.getPosition().getY());
		logger.info(
				"closestPlayer is :" + closestPlayer.getPosition().getX() + "," + closestPlayer.getPosition().getY());
		logger.info("closestMine is :" + closestMine.getPosition().getX() + "," + closestMine.getPosition().getY());

		// Astar is done from source to the source
		vertexes = doAStar(vertexes);

		// Step one: Do I need HP?
		if (gameState.getHero().getGold() >= 2 && gameState.getHero().getLife() <= 30) {
			logger.info(gameState.getHero().getGold() + " >=2" + gameState.getHero().getLife() + " <= 30");
			runAwayMode = true;

			// get path using the Astar
			Vertex move = Astar_getPath(closestPub).get(0);

			// avoid if there is any enemy near by
			while (!avoid(move)) {

				// if there is an enemy nearby look for the next closestpub
				closestPub = nextclosestPub(vertexes, closestPub);
				move = Astar_getPath(closestPub).get(0);
			}
			logger.info("Getting beer");
			return BotUtils.directionTowards(gameState.getHero().getPos(), move.getPosition());
		}

		if (runAwayMode == true && gameState.getHero().getGold() >= 2 && gameState.getHero().getLife() <= 80) {
			logger.info(gameState.getHero().getGold() + " >=2" + gameState.getHero().getLife() + " <= 80");
			Vertex move = Astar_getPath(closestPub).get(0);
			while (!avoid(move)) {
				closestPub = nextclosestPub(vertexes, closestPub);
				move = Astar_getPath(closestPub).get(0);
			}
			logger.info("Getting beer");
			return BotUtils.directionTowards(gameState.getHero().getPos(), move.getPosition());
		} else {
			runAwayMode = false;
		}

		// Look for mine
		Vertex move = Astar_getPath(closestMine).get(0);
		if (avoid(move))
			return BotUtils.directionTowards(gameState.getHero().getPos(), move.getPosition());

		// Step two: Shank someone.
		move = null;
		move = Astar_getPath(closestPlayer).get(0);
		if (avoid(move)) {
			logger.info(" From " + me.getPosition().getX() + "," + me.getPosition().getY() + "Going after player - "
					+ closestPlayer.getPosition().getX() + "," + closestPlayer.getPosition().getY());
			return BotUtils.directionTowards(gameState.getHero().getPos(), move.getPosition());
		}

		return BotUtils.directionTowards(gameState.getHero().getPos(), gameState.getHero().getPos());

	}
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static List<Vertex> getPathAstar(Vertex target) {
		List<Vertex> path = new LinkedList<Vertex>();

		path.add(target);
		Vertex next = target;
		while (next.getPrevious().getPosition().getX() == me.getPosition().getX()
				&& next.getPrevious().getPosition().getY() == me.getPosition().getY()) {
			path.add(next.getPrevious());
			next = next.getPrevious();
		}

		Collections.reverse(path);
		return path;
	}

	private static boolean avoid(Vertex target) {
		List<Vertex> adj = target.getAdjacencies();

		for (Vertex adjacent : adj) {
			if (adjacent.getTileType().startsWith("@"))
				return false;
		}

		return true;
	}

	@Override
	public void setup() {
		// No-op
	}

	@Override
	public void shutdown() {
		// No-op
	}

	public Vertex nextclosestPlayer(List<Vertex> vertexes, Vertex closestPlayer) {
		Vertex nextclosestPlayer = null;
		for (Vertex v : vertexes) {
			if (v.getTileType().startsWith("@") && v.getMinDistance() != 0
					&& v.getMinDistance() != Double.POSITIVE_INFINITY
					&& (nextclosestPlayer == null || (nextclosestPlayer.getMinDistance() > v.getMinDistance()
							&& closestPlayer.getMinDistance() < v.getMinDistance())))
				nextclosestPlayer = v;
		}

		return nextclosestPlayer;
	}

	public Vertex nextclosestMine(List<Vertex> vertexes, Vertex closestMine) {
		Vertex nextclosestMine = null;
		for (Vertex v : vertexes) {
			if (v.getTileType().startsWith("@") && v.getMinDistance() != 0
					&& v.getMinDistance() != Double.POSITIVE_INFINITY
					&& (nextclosestMine == null || (nextclosestMine.getMinDistance() > v.getMinDistance()
							&& closestMine.getMinDistance() < v.getMinDistance())))
				nextclosestMine = v;
		}

		return nextclosestMine;
	}

	public Vertex nextclosestPub(List<Vertex> vertexes, Vertex closestPub) {
		Vertex nextclosestPub = null;
		logger.info("Enter nextclosestpub");
		for (Vertex v : vertexes) {
			logger.info(nextclosestPub.getMinDistance() + " > " + v.getMinDistance());
			if (v.getTileType().equals("[]") && v.getMinDistance() != Double.POSITIVE_INFINITY
					&& (closestPub == null || (nextclosestPub.getMinDistance() > v.getMinDistance()
							&& closestPub.getMinDistance() < v.getMinDistance())))
				nextclosestPub = v;
		}

		return nextclosestPub;
	}

	public List<Vertex> initialising(GameState.Board board, GameState.Hero hero) {
		List<Vertex> vertexes = new LinkedList<Vertex>();
		// Build the graph sans edges
		me = new Vertex();
		for (int row = 0; row < board.getSize(); row++) {
			for (int col = 0; col < board.getSize(); col++) {
				Vertex v = new Vertex();
				GameState.Position pos = new GameState.Position(row, col);
				int tileStart = row * board.getSize() * 2 + (col * 2);
				v.setTileType(board.getTiles().substring(tileStart, tileStart + 1 + 1));
				v.setPosition(pos);
				vertexes.add(v);
			}
		}
		// logger.info("hero = " + hero.getPos().getX() + "," +
		// hero.getPos().getY());
		// Add in the edges
		for (int i = 0; i < board.getSize() * board.getSize(); i++) {
			int row = i % (board.getSize());
			int col = i / board.getSize();

			Vertex v = vertexes.get(i);

			// Check: Is this us?
			if (v.getPosition().getX() == hero.getPos().getX() && v.getPosition().getY() == hero.getPos().getY()) {
				me = v;
				// logger.info("v=" + v.getPosition().getX() + "," +
				// v.getPosition().getY());
				// logger.info("me=" + me.getPosition().getX() + "," +
				// me.getPosition().getY());
			}

			if (v.getTileType().equals("##") || v.getTileType().equals("[]") || v.getTileType().startsWith("$"))
				// Impassable tiles link to nowhere.
				continue;

			// Make sure not to link to impassable blocks that don't have a
			// function
			// Make sure you don't link impassable blocks to other blocks (you
			// can't go from the pub to somewhere else)
			for (int j = col - 1; j <= col + 1; j += 2) {
				if (j >= 0 && j < board.getSize()) {
					Vertex adjacentV = vertexes.get(j * board.getSize() + row);
					v.getAdjacencies().add(adjacentV);
				}
			}
			for (int j = row - 1; j <= row + 1; j += 2) {
				if (j >= 0 && j < board.getSize()) {
					Vertex adjacentV = vertexes.get(col * board.getSize() + j);
					v.getAdjacencies().add(adjacentV);
				}
			}
		}
		// logger.info(me.getPosition().getX() + "," + me.getPosition().getY());
		return vertexes;
	}

	private static class Vertex implements Comparable<Vertex> {
		private String tileType;
		private List<Vertex> adjacencies;
		private double minDistance;
		private Vertex previous;
		private GameState.Position position;
		private boolean walkable;
		private double h, f, g;

		private Vertex() {
			this.minDistance = Double.POSITIVE_INFINITY;

			// Safe default size...we want to avoid resizing
			this.adjacencies = new ArrayList<Vertex>(50 * 50);
			this.walkable = true;
			this.g = 0;
			this.f = 0;
			this.h = 0;
		}

		public GameState.Position getPosition() {
			return position;
		}

		public void setPosition(GameState.Position position) {
			this.position = position;
		}

		public String getTileType() {
			return tileType;
		}

		public void setTileType(String tileType) {
			this.tileType = tileType;
		}

		public List<Vertex> getAdjacencies() {
			return adjacencies;
		}

		public void setAdjacencies(List<Vertex> adjacencies) {
			this.adjacencies = adjacencies;
		}

		public double getMinDistance() {
			return minDistance;
		}

		public void setMinDistance(double minDistance) {
			this.minDistance = minDistance;
		}

		public Vertex getPrevious() {
			return previous;
		}

		public void setPrevious(Vertex previous) {
			this.previous = previous;
		}

		@Override
		public int compareTo(Vertex o) {
			return Double.compare(getMinDistance(), o.getMinDistance());
		}

		public double getG() {
			return g;
		}

		public void setG(Vertex current) {
			this.g = current.getG() + 1;
		}

		public double calculategCosts(Vertex previous) {

			return (previous.getG() + 1);

		}

		public double getH() {
			return h;
		}

		public void setH(Vertex endNode) {
			this.h = Math.sqrt((Math.pow(absolute(this.getPosition().getX() - endNode.getPosition().getY()), 2)
					+ Math.pow(absolute(this.getPosition().getX() - endNode.getPosition().getY()), 2)));
			// System.out.println(this.h);
		}

		public double getF() {
			return g + h;
		}

		public void setF(double f) {
			this.f = f;
		}

		private int absolute(int a) {
			return a > 0 ? a : -a;
		}

	}
}
