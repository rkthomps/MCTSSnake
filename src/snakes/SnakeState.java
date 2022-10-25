package snakes;

public class SnakeState {
    public final Coordinate mazeSize;
    public final Snake snake0, snake1;
    public Coordinate appleCoordinate;


    public SnakeState(Coordinate mazeSize, Snake snake0,
                      Snake snake1, Coordinate appleCoordinate){
        this.mazeSize = mazeSize;
        this.snake0 = snake0; // Maybe have copies idk
        this.snake1 = snake1; // Maybe have copies idk
        this.appleCoordinate = appleCoordinate;
    }


}
