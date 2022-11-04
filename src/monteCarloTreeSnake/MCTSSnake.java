package monteCarloTreeSnake;

// import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

public class MCTSSnake {//implements Bot, Runnable {

     /* Smart snake bot (brain of your snake) should choose step (direction where to go)
     * on each game step until the end of game
     *
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segme
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return Direction in which snake should crawl next game step
     */

    // TODO: expand()
    // if the node has children that have not been explored, explore and add to child list


    // TODO: treePolicy()
    // This method is supposed to implement tree policy, which returns the next node to expand, starting from the root node
    // Right now, the method simply expand the node if it has no children, and returns a random child otherwise
    //    A) If the node is terminal (that is, if get_value does not return None for its board), return the node itself
    //    B) If the node has some unexpanded children, expand it (you can check this by comparing the number of children it has to the number of successor states in get_possible moves, or if expand returns None )
        // C) Otherwise, apply tree policy recursively to the node's best child and next player

    // TODO: bestChild()
    //  * select best child to expand based on UCB


    // TODO: backup()
    // * increment visits
    // * add to either snake or opponent wins if terminal state, else nothing
    // * recursively call itself on parent unless parent is root node
    public Direction chooseDirection(final Snake snake, final Snake opponent,
                                     final Coordinate mazeSize, final Coordinate apple){
        return Direction.DOWN;
    }
}

