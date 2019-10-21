package agents;

import java.util.Random;
import java.io.*;
import java.util.*;

import loveletter.*;

/**
 * An interface for representing an agent in the game Love Letter All agent's
 * must have a 0 parameter constructor
 */
public class MCTSAgent implements Agent {

  private Random rand;
  private State current;
  private int myIndex;

  // 0 place default constructor
  public MCTSAgent() {
    rand = new Random();
  }

  /**
   * Reports the agents name
   */
  public String toString() {
    return "Monte Moit";
  }

  /**
   * Method called at the start of a round
   * 
   * @param start the starting state of the round
   **/
  public void newRound(State start) {
    current = start;
    myIndex = current.getPlayerIndex();
  }

  /**
   * Method called when any agent performs an action.
   * 
   * @param act     the action an agent performs
   * @param results the state of play the agent is able to observe.
   **/
  public void see(Action act, State results) {
    current = results;
  }

  /**
   * Perform an action after drawing a card from the deck
   * 
   * @param c the card drawn from the deck
   * @return the action the agent chooses to perform
   * @throws IllegalActionException when the Action produced is not legal.
   */
  public Action playCard(Card c) {
    Action act = null;
    Card play;

    testState(c);

    while (!current.legalAction(act, c)) {
      if (rand.nextDouble() < 0.5)
        play = c;
      else
        play = current.getCard(myIndex);
      int target = rand.nextInt(current.numPlayers());
      try {
        switch (play) {
        case GUARD:
          act = Action.playGuard(myIndex, target, Card.values()[rand.nextInt(7) + 1]);
          break;
        case PRIEST:
          act = Action.playPriest(myIndex, target);
          break;
        case BARON:
          act = Action.playBaron(myIndex, target);
          break;
        case HANDMAID:
          act = Action.playHandmaid(myIndex);
          break;
        case PRINCE:
          act = Action.playPrince(myIndex, target);
          break;
        case KING:
          act = Action.playKing(myIndex, target);
          break;
        case COUNTESS:
          act = Action.playCountess(myIndex);
          break;
        default:
          act = null;// never play princess
        }
      } catch (IllegalActionException e) {
        /* do nothing */}
    }
    return act;
  }

  public void testState(Card topCard) {

    MyRandomAgent[] agents = { new MyRandomAgent(), new MyRandomAgent(), new MyRandomAgent(), new MyRandomAgent() };
    Card[] remainingCards = current.unseenCards();
    Card inHand = current.getCard(myIndex);

    boolean[] eliminated = new boolean[current.numPlayers()];
    for (int i = 0; i < eliminated.length; i++) {
      if (current.eliminated(i)) {
        eliminated[i] = true;
      }
    }

    int topIndex = 16 - current.deckSize();

    Card[][] discards = new Card[agents.length][16];

    // Iterator to traverse the list
    int cntr = 0;
    for (int j = 0; j < discards.length; j++) {

      Iterator iterator = current.getDiscards(j);
      while (iterator.hasNext()) {
        discards[j][cntr++] = (Card) iterator.next();
      }

      cntr = 0; // reset
    }

    boolean[] handmaid = new boolean[agents.length];
    for (int i = 0; i < handmaid.length; i++) {
      handmaid[i] = current.handmaid(i);
    }

    MyState s = new MyState(rand, agents, remainingCards, topCard, inHand, myIndex, eliminated, topIndex, discards,
        handmaid);

  }

  /**
   * Apply the Monti Carlo Algorithm in order to make the best move in the current
   * position
   * 
   * @param c card picked up
   * @return card the agent should play given the position
   */
  public Card MontiCarlo(Card c) {

    // Need to set up the root node outside of the for loops

    Node rootNode = new Node();

    Node child1 = new Node();
    Node child2 = new Node();

    // Set up the child nodes for the root node
    // They are both currently lead nodes, don't need to store anything in them at
    // this stage
    // Need to make sure over all of the for loops, that number of visits and score
    // are maintained

    rootNode.setChild1(child1);
    rootNode.setChild2(child2);

    child1.setParent(rootNode);
    child2.setParent(rootNode);

    // GET INFORMATION FOR DETERMINISATION

    MyState[] playerStates = new MyState[4];

    MyRandomAgent[] agents = { new MyRandomAgent(), new MyRandomAgent(), new MyRandomAgent(), new MyRandomAgent() };
    Card[] remainingCards = current.unseenCards();
    Card inHand = current.getCard(myIndex);
    // eliminated info
    boolean[] eliminated = new boolean[current.numPlayers()];
    for (int i = 0; i < eliminated.length; i++) {
      if (current.eliminated(i)) {
        eliminated[i] = true;
      }
    }

    int topIndex = 16 - current.deckSize();
    // discards info
    Card[][] discards = new Card[agents.length][16];

    // Iterator to traverse the list
    int cntr = 0;
    for (int j = 0; j < discards.length; j++) {

      Iterator iterator = current.getDiscards(j);
      while (iterator.hasNext()) {
        discards[j][cntr++] = (Card) iterator.next();
      }

      cntr = 0; // reset
    }
    // handmaid info
    boolean[] handmaid = new boolean[agents.length];
    for (int i = 0; i < handmaid.length; i++) {
      handmaid[i] = current.handmaid(i);
    }

    // Overarching for-loop
    // 100 different decks will be generated by this loop
    for (int i = 0; i < 10; i++) {

      MyState s = new MyState(rand, agents, remainingCards, c, inHand, myIndex, eliminated, topIndex, discards,
          handmaid);

      // set player states for each random agent
      try {
        for (int j = 0; j < agents.length; j++) {
          playerStates[j] = s.playerState(j);
          agents[j].newRound(playerStates[j]);
        }
      } catch (IllegalActionException e) {
        System.out.println("Illegal action");
      }

      rootNode.setState(s);

      // Draw card from deck, then play the card just drawn from the deck
      try {
      Card topCard = s.drawCard();
      } catch (IllegalActionException e) {
        System.out.println("Illegal action");
      }

      expand(rootNode, c, agents);

    }

    return c;

  }

  /**
   * expand a node n
   * 
   * @param n
   * @param topCard
   */
  private void expand(Node n, Card topCard, MyRandomAgent[] agents) {

    MyState gameState = new MyState(n.getState());
    Node child1 = n.getFirstChild();
    Node child2 = n.getSecondChild();

    if (topCard == Card.PRINCESS) {

      Action a = agents[gameState.nextPlayer()].playSpecificCard(gameState.getCard(gameState.nextPlayer()));

      // Update the gameState, then deep copy it into a child node
      try {
        gameState.update(a, topCard);
      } catch (IllegalActionException e) {
        System.out.println("Update didnt work");
      }

      child1.setState(gameState);
      checkIfTerminal(child1);

      child2.incrementVisits(10000000);

    } else if (gameState.getCard(gameState.nextPlayer()) == Card.PRINCESS) {
      Action a = agents[gameState.nextPlayer()].playSpecificCard(topCard);

      // Update the gameState, then deep copy it into a child node
      try {
        gameState.update(a, topCard);
      } catch (IllegalActionException e) {
        System.out.println("Update didnt work");
      }

      child1.setState(gameState);
      checkIfTerminal(child1);

      child2.incrementVisits(10000000);

    } else if (topCard == Card.COUNTESS && (gameState.getCard(gameState.nextPlayer()) == Card.PRINCE
        || gameState.getCard(gameState.nextPlayer()) == Card.KING)) {
      Action a = agents[gameState.nextPlayer()].playSpecificCard(topCard);

      // Update the gameState, then deep copy it into a child node
      try {
        gameState.update(a, topCard);
      } catch (IllegalActionException e) {
        System.out.println("Update didnt work");
      }

      child1.setState(gameState);
      checkIfTerminal(child1);

      child2.incrementVisits(10000000);
    } else if (gameState.getCard(gameState.nextPlayer()) == Card.COUNTESS
        && (topCard == Card.PRINCE || topCard == Card.KING)) {
      Action a = agents[gameState.nextPlayer()].playSpecificCard(gameState.getCard(gameState.nextPlayer()));

      // Update the gameState, then deep copy it into a child node
      try {
        gameState.update(a, topCard);
      } catch (IllegalActionException e) {
        System.out.println("Update didnt work");
      }

      child1.setState(gameState);
      checkIfTerminal(child1);

      child2.incrementVisits(10000000);
    } else {

      Action act = agents[gameState.nextPlayer()].playSpecificCard(topCard);

      // Update the gameState, then deep copy it into a child node
      try {
        gameState.update(act, topCard);
      } catch (IllegalActionException e) {
        System.out.println("Update didnt work");
      }

      child1.setState(gameState);
      checkIfTerminal(child1);

      // Reset gameState to its inital state from rootNode
      gameState = new MyState(n.getState());

      try {
      topCard = gameState.drawCard();
      } catch (IllegalActionException e) {
        System.out.println("Illegal action");
      }
      act = agents[gameState.nextPlayer()].playSpecificCard(gameState.getCard(gameState.nextPlayer()));

      try {
        System.out.println(gameState.update(act, topCard));
      } catch (IllegalActionException e) {
        System.out.println("Update didnt work");
      }

      // Setup child2
      child2.setState(gameState);
      checkIfTerminal(child2);
    }
  }

  /**
   * Checks if node n is a terminal node
   * 
   * @param n
   */
  private void checkIfTerminal(Node n) {

    MyState s = n.getState();

    if (s.roundOver()) {
      n.setIsTerminal(true);

      if (s.score(myIndex) == 1) {
        n.incrementScore(1);
      }

      return;
    }

    if (s.eliminated(myIndex)) {
      n.setIsTerminal(true);
      return;
    }

  }
}
