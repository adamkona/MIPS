package server;

import ai.AILoopControl;
import java.util.HashMap;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import objects.Entity;
import objects.Pellet;
import utils.GameLoop;
import utils.Input;
import utils.Map;
import utils.Methods;
import utils.Point;
import utils.ResourceLoader;
import utils.enums.Direction;

public class Telemetry implements Telemeters {

  private static final int AGENT_COUNT = 2;
  private final int playerCount;
  private BlockingQueue<Input> inputs;
  private BlockingQueue<String> outputs;
  private Entity[] agents;
  private boolean singlePlayer;
  private Map map;
  private HashMap<String, Pellet> pellets;
  private AILoopControl ai;
  private boolean aiRunning;
  private ResourceLoader resourceLoader;

  public Telemetry(
      Map map,
      int playerCount,
      Queue<Input> inputQueue,
      Queue<String> outputQueue,
      ResourceLoader resourceLoader) {
    this.map = map;
    inputs = (BlockingQueue<Input>) inputQueue;
    outputs = (BlockingQueue<String>) outputQueue;
    this.playerCount = playerCount;
    this.resourceLoader = resourceLoader;
    this.singlePlayer = false;
    initialise();
    startGame();
  }

  public Telemetry(Map map, Queue<Input> clientQueue, ResourceLoader resourceLoader) {
    this.map = map;
    inputs = (BlockingQueue<Input>) clientQueue;
    outputs = new LinkedBlockingQueue<>();
    this.playerCount = 1;
    singlePlayer = true;
    this.resourceLoader = resourceLoader;
    initialise();
    startGame();
  }

  /**
   * Static method for updating game state increments positions if valid, increments points, and
   * detects and treats entity collisions
   *
   * <p>TODO: increment points functionality
   *
   * @param agents array of entities in current state
   * @author Alex Banks, Matthew Jones
   * @see this#detectEntityCollision(Entity, Entity, ResourceLoader)
   */
  private static void processPhysics(
      Entity[] agents, Map m, ResourceLoader resourceLoader, HashMap<String, Pellet> pellets) {

    for (int i = 0; i < AGENT_COUNT; i++) {
      if (agents[i].getDirection() != null) {
        Point prevLocation = agents[i].getLocation();
        agents[i].move();
        Point faceLocation = agents[i].getFaceLocation();

        if (m.isWall(faceLocation)) {
          System.out.println("~Player" + i + " drove into a wall");
          agents[i].setLocation(prevLocation.centralise());
          agents[i].setDirection(null);
        }
      }
    }

    // separate loop for checking collision after iteration

    for (int i = 0; i < AGENT_COUNT; i++) {
      for (int j = (i + 1); j < AGENT_COUNT; j++) {

        if (agents[i].isPacman() && !agents[j].isPacman()) {
          detectEntityCollision(agents[i], agents[j], resourceLoader);
        }

        if (agents[j].isPacman() && !agents[i].isPacman()) {
          detectEntityCollision(agents[j], agents[i], resourceLoader);
        }
      }
    }
    pelletCollision(agents, pellets);
    for (Pellet p : pellets.values()) {
      p.incrementRespawn();
    }
  }

  /**
   * Static method for 'swapping' a pacman and ghoul if they occupy the same area.
   *
   * @param pacman Entity currently acting as pacman
   * @param ghoul Entity currently running as ghoul
   * @author Alex Banks, Matthew Jones
   */
  private static void detectEntityCollision(
      Entity pacman, Entity ghoul, ResourceLoader resourceLoader) {
    Point pacmanCenter = pacman.getLocation();
    Point ghoulFace = ghoul.getFaceLocation();

    if (pacmanCenter.inRange(ghoulFace)) {

      pacman.setPacMan(false);
      ghoul.setPacMan(true);
      pacman.setLocation(resourceLoader.getMap().getRandomSpawnPoint());
      pacman.setDirection(Direction.UP);
      pacman.updateImages(resourceLoader);
      ghoul.updateImages(resourceLoader);

      // System.out.println("~Ghoul" + ghoul.getClientId() + " captured Mipsman" +
      // pacman.getClientId());
    }
  }

  /**
   * Static method to detect if the pacMan entity will eat a pellet
   *
   * @param agents The entities
   * @param pellets The pellets
   * @author Matthew Jones
   */
  private static void pelletCollision(Entity[] agents, HashMap<String, Pellet> pellets) {
    for (Entity agent : agents) {
      if (!agent.isPacman()) {
        continue;
      }
      Point p = agent.getFaceLocation();
      int x = (int) p.getX();
      int y = (int) p.getY();
      Pellet pellet = pellets.get(Integer.toString(x) + y);
      if (pellet != null && pellet.isActive()) {
        pellet.setActive(false);
        agent.incrementScore();
      }
    }
  }

  public HashMap<String, Pellet> getPellets() {
    return pellets;
  }

  /**
   * Initialises the game agents/entities and AI to control them
   *
   * @author Matthew Jones
   */
  private void initialise() {
    agents = new Entity[AGENT_COUNT];
    agents[0] = new Entity(false, 0, new Point(1.5, 1.5, map));
    agents[1] = new Entity(false, 1, new Point(1.5, 18.5, map));
    //agents[2] = new Entity(false, 2, new Point(9.5, 15.5, map));
    //agents[3] = new Entity(false, 3, new Point(11.5, 1.5, map));
    //agents[4] = new Entity(false, 4, new Point(14.5, 11.5, map));
    Methods.updateImages(agents, resourceLoader);
    if (singlePlayer) {
      agents[(new Random()).nextInt(AGENT_COUNT)].setPacMan(true);
    }

    int aiCount = AGENT_COUNT - playerCount;
    if (aiCount > 0) {
      int[] aiControlled = new int[aiCount];
      int highestId = AGENT_COUNT - 1;
      for (int i = 0; i < aiCount; i++) {
        aiControlled[i] = highestId;
        highestId--;
      }
      aiRunning = false;
      ai = new AILoopControl(agents, aiControlled, map, inputs);
    }

    pellets = new HashMap<String, Pellet>();
    for (int i = 0; i < map.getMaxX(); i++) {
      for (int j = 0; j < map.getMaxY(); j++) {
        Point point = new Point(i + 0.5, j + 0.5);
        if (!map.isWall(point)) {
          Pellet pellet = new Pellet(point);
          pellet.updateImages(resourceLoader);
          pellets.put(Integer.toString(i) + j, pellet);
        }
      }
    }
  }

  public void setMipID(int ID) {
    this.agents[ID].setPacMan(true);
  }

  public Map getMap() {
    return map;
  }

  public Entity getEntity(int id) {
    return agents[id];
  }

  public void addInput(Input in) {
    inputs.add(in);
  }

  private void startGame() {
    startAI();

    final long DELAY = (long) Math.pow(10, 7);
    final long positionDELAY = (long) Math.pow(10, 8);

    new GameLoop(DELAY) {
      @Override
      public void handle() {
        processInputs();

        processPhysics(agents, map, resourceLoader, pellets);
      }
    }.start();

    new GameLoop(positionDELAY) {
      @Override
      public void handle() {
        updateClients(agents);
      }
    }; //.start();
  }

  public void startAI() {
    if (!aiRunning && ai != null) {
      ai.start();
    }
  }

  /**
   * Method to deal with the inputs provided in the inputs queue
   *
   * @author Matthew Jones
   */
  private void processInputs() {
    while (!inputs.isEmpty()) {
      Input input = inputs.poll();
      int id = input.getClientID();
      Direction d = input.getMove();
      if (Methods.validiateDirection(d, agents[id], map)) {
        agents[id].setDirection(d);
        if (!singlePlayer) {
          // this is currently what's set to update on other clients' systems. they'll get valid
          // inputs
          informClients(input, agents[id].getLocation()); // Inputs sent to the other clients
        }
      }
    }
  }

  private void informClients(Input input, Point location) {
    outputs.add(NetworkUtility.makeEntitiyMovementPacket(input, location));
  }

  private int getMipID(){
    for(Entity e:agents){
      if (e.isPacman()){
        return e.getClientId();
      }
    }
    return 0;
  }

  private void updateClients(Entity[] agents) {
    outputs.add(NetworkUtility.makeEntitiesPositionPacket(agents)+Integer.toString(getMipID()));
  }

  public Entity[] getAgents() {
    return agents;
  }
}
