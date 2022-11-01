package mctsSnake;

import snakes.Coordinate;
import snakes.Snake;


public class MCTSNode {
    private MCTSState state;

    MCTSNode(Snake snake1, Snake snake2, Coordinate mapSize, Coordinate apple) {
        this.state = new MCTSState(snake1, snake2, mapSize, apple);
    }


}
