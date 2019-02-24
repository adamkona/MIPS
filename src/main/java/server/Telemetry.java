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

public class Telemetry extends Telemeters {

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
    agents[2] = new Entity(false, 2, new Point(9.5, 15.5, map));
    agents[3] = new Entity(false, 3, new Point(11.5, 1.5, map));
    agents[4] = new Entity(false, 4, new Point(14.5, 11.5, map));
    Methods.updateImages(agents, resourceLoader);
    if (singlePlayer) {
      agents[(new Random()).nextInt(AGENT_COUNT)].setMipsman(true);
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

    pellets = initialisePellets(map, resourceLoader);
  }

  @Override
  public void setMipID(int ID) {
    this.agents[ID].setMipsman(true);
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
    }; // .start();
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
      if (Methods.validateDirection(d, agents[id].getLocation(), map)) {
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

  private int getMipID() {
    for (Entity e : agents) {
      if (e.isMipsman()) {
        return e.getClientId();
      }
    }
    return 0;
  }

  private void updateClients(Entity[] agents) {
    outputs.add(NetworkUtility.makeEntitiesPositionPacket(agents) + Integer.toString(getMipID()));
  }

  public Entity[] getAgents() {
    return agents;
  }
}
