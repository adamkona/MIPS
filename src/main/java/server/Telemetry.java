package server;

import javafx.animation.AnimationTimer;
import objects.Entity;
import utils.Input;
import utils.Map;
import utils.ResourceLoader;
import utils.enums.Direction;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Telemetry {
    
    private static final int AGENT_COUNT = 5;
  private BlockingQueue<Input> inputs;
  private Entity[] agents;
  private ResourceLoader resourceLoader;

  public Telemetry() {
    resourceLoader = new ResourceLoader("src/main/resources");
    inputs = new LinkedBlockingQueue<>();
    int aiCount = AGENT_COUNT - makeConnections();
    if (aiCount > 0) {
      // Generate the AI to control each entity needed
    }
    agents = new Entity[AGENT_COUNT];

    agents[0] = new Entity(true, 0, resourceLoader);
    agents[1] = new Entity(false, 1, resourceLoader);
    agents[2] = new Entity(false, 2, resourceLoader);
    agents[3] = new Entity(false, 3, resourceLoader);
    agents[4] = new Entity(false, 4, resourceLoader);
    //startGame();
  }

  /**
   * Static method for updating game state increments positions if valid, increments points, and
   * detects and treats entity collisions TODO: 30/1/19 increment points functionality
   *
   * @param agents array of entities in current state
   * @return array of entities in new state
   * @author Alex Banks
   * @see this#entityCollision(Entity, Entity, Double)
   */
  private static Entity[] processPhysics(Entity[] agents) {
    Map m = (new ResourceLoader(System.getProperty("user.dir"))).getMap();

    for (int i = 0; i < AGENT_COUNT; i++) {
      Point2D.Double tempLocation = agents[i].getLocation();
      double offset = agents[i].getVelocity();

      switch (agents[i].getDirection()) {
        case UP:
          tempLocation.setLocation(tempLocation.getX() + offset, tempLocation.getY());
          break;
        case DOWN:
          tempLocation.setLocation(tempLocation.getX() - offset, tempLocation.getY());
          break;
        case RIGHT:
          tempLocation.setLocation(tempLocation.getX(), tempLocation.getY() + offset);
          break;
        case LEFT:
          tempLocation.setLocation(tempLocation.getX(), tempLocation.getY() - offset);
          break;
      }

      if (m.isWall(tempLocation)) {
        agents[i].setVelocity(0);
      } else {
        agents[i].setLocation(tempLocation);
      }
    
        // TODO add points for pellet collision
    }
    
      // separate loop for checking collision after iteration
    for (int i = 0; i < AGENT_COUNT; i++) {
      for (int j = i + 1; j < AGENT_COUNT; j++) {
          if ((int) agents[i].getLocation().getX() == (int) agents[j].getLocation().getX()
                  && (int) agents[i].getLocation().getY() == (int) agents[j].getLocation().getY()) {
          entityCollision(agents[i], agents[j], m.getSpawnPoint());
        }
      }
    }

    return agents;
  }

  /**
   * Static method for 'swapping' entities if they occupy the same square. Does nothing if both
   * entities are ghouls
   *
   * @param x Entity one
   * @param y Entity two
   * @param respawnPoint Point to relocate new ghoul too
   */
  private static void entityCollision(Entity x, Entity y, Double respawnPoint) {
      if (x.isPacman() && !y.isPacman()) {
      x.setPacMan(false);
      y.setPacMan(true);
      x.setLocation(respawnPoint);
      x.setDirection(Direction.UP);
      } else if (y.isPacman() && !y.isPacman()) {
      y.setPacMan(false);
      x.setPacMan(true);
      y.setLocation(respawnPoint);
      y.setDirection(Direction.UP);
    }
  }
    
    /**
     * Makes the connections to the other clients
     *
     * @return the number of human players in the game
     */
    private int makeConnections() {
        int count = 1;
        // TODO implement
        return count;
    }
    
    public Entity getEntity(int id) {
        return agents[id];
    }
    
    public void addInput(Input in) {
        inputs.add(in);
    }
    
    public void startGame() {
        // TODO implement
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                processInputs();
                informClients();
                agents = processPhysics(agents);
                updateClients();
            }
        }.start();
    }
    
    private void processInputs() {
        while (!inputs.isEmpty()) {
            Input input = inputs.poll();
            // Validate the input
            agents[input.getClientID()].setDirection(input.getMove());
        }
    }
    
    private void informClients() {
        // TODO implement
    }
    
    private void updateClients() {
        // TODO implement
    }
}