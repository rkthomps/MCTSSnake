package mctsSnake;

import snakes.Coordinate;
import snakes.Snake;

public class MCTSState {
    private Snake ourSnake;
    private Snake otherSnake;
    private Coordinate mapSize;
    private Coordinate apple;

    MCTSState(Snake snake1, Snake snake2, Coordinate mapSize, Coordinate apple) {
        this.ourSnake = snake1.clone();
        this.otherSnake = snake2.clone();
        this.mapSize = mapSize;
        this.apple = apple;
    } 

    public Snake getOurSnake() {
        return ourSnake;
    }

    public Snake getOtherSnake() {
        return otherSnake;
    }

    public Coordinate getMapSize() {
        return mapSize;
    }

    public Coordinate getApple() {
        return apple;
    }
}
