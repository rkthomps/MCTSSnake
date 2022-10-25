package finalBot;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

import java.util.*;

/**
 * This is the AdderBoaCobra bot submitted by Serpentine
 * on 3 May 2020, for the AI Snakes competition
 *
 * only change: time per turn can easily be set at the top here
 */
public class AdderBoaCobra implements Bot, Runnable {
    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

    // time per turn
    private int TIME_PER_TURN = 300;

    // for BFS
    private Map<Coordinate, Coordinate> snake_cameFrom = new HashMap<Coordinate, Coordinate>();
    private HashMap<Coordinate, Integer> snake_depth = new HashMap<Coordinate, Integer>();
    private Map<Coordinate, Coordinate> opponent_cameFrom = new HashMap<Coordinate, Coordinate>();
    private HashMap<Coordinate, Integer> opponent_depth = new HashMap<Coordinate, Integer>();
    private int oldReachable;
    private int opponent_oldReachable;
    private boolean checkCanEnclose;

    // to circle the apple
    public Coordinate[] circle_positions = new Coordinate[8];
    public Coordinate backwards_indicator_position = null;
    public int circle_positions_length = 0;
    public int circle_positions_iterator = 0;
    public int circle_type = 0;
    public boolean dontTakeApple = false;
    public boolean found_position_5 = false;

    public int circle_moves_after_apple_change;
    public int max_circle_moves_after_apple_change = 4;
    public boolean apple_changed_during_circle = false;
    public boolean closed_circle = true;  // says if we need to have a closed circle

    // for checking if circle is complete
    public boolean closed_circle_type = true;  // says if the current circle_type is a closed one or not
    public boolean circle_complete = false;  // says if the circle is complete
    private Map<Coordinate, Coordinate> opponent_breakOnApple_cameFrom = new HashMap<Coordinate, Coordinate>();
    private HashMap<Coordinate, Integer> opponent_breakOnApple_depth = new HashMap<Coordinate, Integer>();

    public Coordinate head;
    public Coordinate apple = null;
    public Coordinate prev_apple = null;

    private long startTime;

    // for alpha beta
    private volatile boolean exit = false;

    private Snake snake = null;
    private Snake opponent = null;
    private Coordinate mazeSize = null;
    // private Coordinate apple = null;
    private Coordinate centre = null;
    private Coordinate[] appleSpots;
    private boolean chase_tail = false;


    private int maxDepth;  // How deep we will go with the minmax algorithm. NEEDS TO BE EVEN
    private Direction bestMove = null; // The best move we will take
    private int bestScore = Integer.MIN_VALUE; // The score of the move

    /* Score */
    private int scr_WeAteApple = 800;
    private int scr_circlePart1 = 1000; // We cover all circle points. TODO covering all entry points the circle
    private int scr_circlePart2 = 80000; // We successfully made the circle --> The move should be follow our tail
    private int scr_OppAteApple = -500;
    private int scr_losingLead = -1250;
    private int scr_WeCantMove = -20000;
    private int scr_OppCantMove = 10000;
    private int scr_GoodHeadColl = 5000;
    private int scr_BadHeadColl = -20000;
    private int scr_weReachedCentre = 100;

    private int fct_distanceToApple = -5;
    private int fct_distanceToCentre = -2;
    private int fct_earlyReachableTiles = 50;

    public Direction final_move;


    /**
     * Choose the direction
     *
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return Direction of bot's move
     */
    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        startTime = System.currentTimeMillis();
        this.head = snake.getHead();
        this.apple = apple;
        return chooseMove(snake, opponent, mazeSize, apple);
    }

    /**
     * The logic to decide where to move
     *
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return Direction of bot's move
     */
    private Direction chooseMove(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        Direction move; // The variable to be able to run more code before returning the direction

        if (apple_changed_during_circle) {
            circle_moves_after_apple_change--;
            if (circle_moves_after_apple_change == 0) {
                circle_type = circle_type(apple, snake, opponent, mazeSize);
                if (circle_type > 0) {
                    setCircle_positions(apple, circle_type, mazeSize);
                }
//                System.out.println("Apple has changed position, circle_type is now: " + circle_type);
                circle_complete = false;
                circle_moves_after_apple_change = max_circle_moves_after_apple_change;
                apple_changed_during_circle = false;

            }
        }
        if (prev_apple != apple) {  // the apple has moved!
            // so, we need to determine the new circle_type
            if (circle_complete) {
                if (!apple_changed_during_circle) {
                    apple_changed_during_circle = true;
                    circle_moves_after_apple_change = max_circle_moves_after_apple_change;
                }
            } else {
                circle_type = circle_type(apple, snake, opponent, mazeSize);
                if (circle_type > 0) {
                    setCircle_positions(apple, circle_type, mazeSize);
                }
//                System.out.println("Apple has changed position, circle_type is now: " + circle_type);
            }
        }
        prev_apple = apple;

        // circle_type = 0 means we're not circling the apple
        if (circle_type > 0 && snake.body.size() > 5) {  // here we are circling
            if (!circle_complete) {
                circle_complete = circleComplete(snake, opponent, mazeSize, apple);

                /****** determining if  go-towards-circle-with-alphabeta  ***/
                // we want to use alphabeta to go towards our circle (that is safer)
                boolean useAlphaBeta = false;
                if (distanceBetween(head, apple) > 4 && !circle_complete) {
                    // if we're not close to the circle yet, use alphabeta to get there
                    useAlphaBeta = true;
                }
                for (int i = 0; i < circle_positions_length; i++) {
                    if (snake.elements.contains(circle_positions[i])) {
                        // if we cover any circle position, use BFS
                        useAlphaBeta = false;
                        break;
                    }
                }
                if (distanceBetween(opponent.getHead(), apple) == 1 && !circle_complete && closed_circle_type) {
                    // when the opponent got into our circle before we could close it: back to alphabeta!
                    // (we might bump into enemy-head, but we're in front, so no problem)
                    useAlphaBeta = true;
                }
                /****** end of determining if  go-towards-circle-with-alphabeta  ***/


                if (useAlphaBeta) {
                    move = chooseDirectionAlphaBeta(snake, opponent, mazeSize, apple, startTime);
//                System.out.println("alpha beta while planning on circling");
                } else {
                    while (System.currentTimeMillis() - startTime < TIME_PER_TURN) {
                        // wait some time, for the human player
                    }
                    // filter out the apple from BFS's paths
                    dontTakeApple = true;
                    makeOpponentBFSGraph(snake, opponent, mazeSize, apple); // We do this before making our BFS because we want to calculate the distance to the apple
                    makeBFSGraph(snake, opponent, mazeSize, apple);
                    move = circle_direction();
                    if (!circle_complete) {
                        if (!checkMove(snake, opponent, mazeSize, apple, move)) { // If the move results in a problem (encloses itself)
                            Coordinate furthestPoint = getFurthestPoint(snake_depth, mazeSize);
                            move = pathTo(furthestPoint);
//                            System.out.println("I don't want to go there");
                        }
                    }
//                System.out.println("circling move with BFS41");
                }


            } else {
                // circle is complete here, so just follow our tail
                while (System.currentTimeMillis() - startTime < TIME_PER_TURN) {
                    // wait some time, for the human player
                }
                move = directionFromTo(head, snake.body.getLast());
                // System.out.println("following my tail");
            }

        } else {  // here we are not circling
            circle_complete = false;
            circle_type = 0;
            dontTakeApple = false;
            move = chooseDirectionAlphaBeta(snake, opponent, mazeSize, apple, startTime);
            // System.out.println("alpha beta move");

        }
        if (move == null) {
            // maybe if BFS is not returning anything
            // AB always returns a direction
            return chooseDirectionAlphaBeta(snake, opponent, mazeSize, apple, startTime);
        }
        return move;
    }

    /**
     * To be called upon apple position change. Looks whether circling the apple is possible
     * Body length must be at least one longer than opponent's length
     * Body length must be an even number
     * Difference between closed and open circle:
     * Closed circle: apple is fully blocked. Open circle: Opponent can get to apple but will die or tie (For sure)
     * If body length is not at least 2 longer than opponent, circle must be closed.
     * Some apple positions offer more possibilities in certain game-states
     * <p>
     * The function returns the circle type: One of the 41 types (with some mirrored duplicates)
     * If no circle type is possible, returns 0 - Cannot or should not circle apple
     * Check the PDF for the 41 circle types
     *
     * @param apple    the apple
     * @param snake    our snake
     * @param opponent other snake
     * @param mazeSize the board
     * @return integer giving the circle type (see the pdf)
     */
    public int circle_type(Coordinate apple, Snake snake, Snake opponent, Coordinate mazeSize) {
        int body = snake.body.size();
        if (body % 2 == 0 && body > opponent.body.size() && body > 5) {
            boolean in_corner = false;
            closed_circle = true;
            if (body > (opponent.body.size() + 1)) {
                closed_circle = false;
            }
            if ((apple.x < 3 && apple.y == 0) || (apple.y < 3 && apple.x == 0) || (apple.x > (mazeSize.x - 4) && apple.y == 0) || (apple.y < 3 && apple.x == (mazeSize.x - 1)) || (apple.x > (mazeSize.x - 4) && apple.y == (mazeSize.y - 1)) || (apple.x == (mazeSize.x - 1) && apple.y > (mazeSize.y - 4)) || (apple.x < 3 && apple.y == (mazeSize.y - 1)) || (apple.x == 0 && apple.y > (mazeSize.y - 4))) {
                in_corner = true;
            }
            //for (int i = 0; i < 12; i++) { //check if in a corner
            //    if (apple.equals(corners[i])) {
            //        in_corner = true;
            //        break;
            //    }
            //}
            if (in_corner) {
                if (apple.x == 0 && apple.y < 3) { //upper left 1
                    if (!closed_circle) {
                        return 2;
                    } else if (body > 7) {
                        if (apple.y == 0) {
                            return 10;
                        }
                        if (body > 9) {
                            if (apple.y == 1) {
                                return 14;
                            }
                            if (body > 11) {
                                return 22;
                            }
                        }
                    }
                } else if (apple.x < 3 && apple.y == 0) { //upper left 2
                    if (!closed_circle) {
                        return 6;
                    } else if (body > 9) {
                        if (apple.x == 1) {
                            return 18;
                        }
                        if (body > 11) {
                            return 26;
                        }
                    }
                } else if (apple.x == (mazeSize.x - 1) && apple.y < 3) { //upper right 1
                    if (!closed_circle) {
                        return 3;
                    } else if (body > 7) {
                        if (apple.y == 0) {
                            return 11;
                        }
                        if (body > 9) {
                            if (apple.y == 1) {
                                return 15;
                            }
                            if (body > 11) {
                                return 23;
                            }
                        }
                    }
                } else if (apple.x > (mazeSize.x - 4) && apple.y == 0) { //upper right 2
                    if (!closed_circle) {
                        return 7;
                    } else if (body > 9) {
                        if (apple.x == mazeSize.x - 2) {
                            return 19;
                        }
                        if (body > 11) {
                            return 27;
                        }
                    }
                } else if (apple.x == (mazeSize.x - 1) && apple.y > (mazeSize.y - 4)) { //bottom right 1
                    if (!closed_circle) {
                        return 4;
                    } else if (body > 7) {
                        if (apple.y == (mazeSize.y - 1)) {
                            return 12;
                        }
                        if (body > 9) {
                            if (apple.y == (mazeSize.y - 2)) {
                                return 16;
                            }
                            if (body > 11) {
                                return 24;
                            }
                        }
                    }
                } else if (apple.x > (mazeSize.x - 4) && apple.y == (mazeSize.y - 1)) { //bottom right 2
                    if (!closed_circle) {
                        return 8;
                    } else if (body > 9) {
                        if (apple.x == mazeSize.x - 2) {
                            return 20;
                        }
                        if (body > 11) {
                            return 28;
                        }
                    }
                } else if (apple.x == 0 && apple.y > (mazeSize.y - 4)) { //bottom left 1
                    if (!closed_circle) {
                        return 5;
                    } else if (body > 7) {
                        if (apple.y == (mazeSize.y - 1)) {
                            return 13;
                        }
                        if (body > 9) {
                            if (apple.y == (mazeSize.y - 2)) {
                                return 17;
                            }
                            if (body > 11) {
                                return 25;
                            }
                        }
                    }
                } else if (apple.x < 3 && apple.y == (mazeSize.y - 1)) { //bottom left 2
                    if (!closed_circle) {
                        return 9;
                    } else if (body > 9) {
                        if (apple.x == 1) {
                            return 21;
                        }
                        if (body > 11) {
                            return 29;
                        }
                    }
                }
            } else {
                if ((body > 7 && !closed_circle) || (body > 13)) { //sides
                    if (apple.x == 0) {
                        if (closed_circle) {
                            return 41;
                        } else {
                            return 35; // OR 36!
                        }
                    }
                    if (apple.x == (mazeSize.x - 1)) {
                        if (closed_circle) {
                            return 40;
                        } else {
                            return 34; // OR 37!
                        }
                    }
                    if (apple.y == (mazeSize.y - 1)) {
                        if (closed_circle) {
                            return 38;
                        } else {
                            return 30; // OR 31!
                        }
                    }
                    if (apple.y == 0) {
                        if (closed_circle) {
                            return 39;
                        } else {
                            return 32; // OR 33!
                        }
                    }
                }
                if (body > 7) {
                    return 1;  // 1 means: apple is somewhere not on the edge, and we're gonna circle it
                }
            }

        }
        return 0;  // 0 means: we're not going to circle the apple
    }

    /**
     * Sets the positions that should at least be covered to ensure the circle type
     * Sets these positions in a public array
     * Sets the number of positions so circle_direction() knows what elements of the array to cover
     * Later addition: also sets the closed_circle_type to true if it is a closed circle, false if not
     *
     * @param apple       the apple
     * @param circle_type the type of circle, see pdf
     * @param mazeSize    the board
     */
    public void setCircle_positions(Coordinate apple, int circle_type, Coordinate mazeSize) {
        circle_positions_iterator = 0;
        switch (circle_type) {
            case 1:
                circle_positions_length = 4;
                circle_positions[0] = new Coordinate(apple.x, (apple.y - 1));
                circle_positions[1] = new Coordinate((apple.x + 1), apple.y);
                circle_positions[2] = new Coordinate(apple.x, (apple.y + 1));
                circle_positions[3] = new Coordinate((apple.x - 1), apple.y);
                closed_circle_type = true;
                break;
            case 2:
                circle_positions_length = 4;
                circle_positions[0] = new Coordinate(1, 2);
                circle_positions[1] = new Coordinate(1, 0);
                circle_positions[2] = new Coordinate(2, 0);
                circle_positions[3] = new Coordinate(2, 2);
                closed_circle_type = false;
                break;
            case 3:
                circle_positions_length = 4;
                circle_positions[0] = new Coordinate((mazeSize.x - 2), 2);
                circle_positions[1] = new Coordinate((mazeSize.x - 2), 0);
                circle_positions[2] = new Coordinate((mazeSize.x - 3), 0);
                circle_positions[3] = new Coordinate((mazeSize.x - 3), 2);
                closed_circle_type = false;
                break;
            case 4:
                circle_positions_length = 4;
                circle_positions[0] = new Coordinate((mazeSize.x - 2), (mazeSize.y - 3));
                circle_positions[1] = new Coordinate((mazeSize.x - 2), (mazeSize.y - 1));
                circle_positions[2] = new Coordinate((mazeSize.x - 3), (mazeSize.y - 1));
                circle_positions[3] = new Coordinate((mazeSize.x - 3), (mazeSize.y - 3));
                closed_circle_type = false;
                break;
            case 5:
                circle_positions_length = 4;
                circle_positions[0] = new Coordinate(1, (mazeSize.y - 3));
                circle_positions[1] = new Coordinate(1, (mazeSize.y - 1));
                circle_positions[2] = new Coordinate(2, (mazeSize.y - 1));
                circle_positions[3] = new Coordinate(2, (mazeSize.y - 3));
                closed_circle_type = false;
                break;
            case 6:
                circle_positions_length = 4;
                circle_positions[0] = new Coordinate(2, 1);
                circle_positions[1] = new Coordinate(0, 1);
                circle_positions[2] = new Coordinate(0, 2);
                circle_positions[3] = new Coordinate(2, 2);
                closed_circle_type = false;
                break;
            case 7:
                circle_positions_length = 4;
                circle_positions[0] = new Coordinate((mazeSize.x - 3), 1);
                circle_positions[1] = new Coordinate((mazeSize.x - 1), 1);
                circle_positions[2] = new Coordinate((mazeSize.x - 1), 2);
                circle_positions[3] = new Coordinate((mazeSize.x - 3), 2);
                closed_circle_type = false;
                break;
            case 8:
                circle_positions_length = 4;
                circle_positions[0] = new Coordinate((mazeSize.x - 3), (mazeSize.y - 2));
                circle_positions[1] = new Coordinate((mazeSize.x - 1), (mazeSize.y - 2));
                circle_positions[2] = new Coordinate((mazeSize.x - 1), (mazeSize.y - 3));
                circle_positions[3] = new Coordinate((mazeSize.x - 3), (mazeSize.y - 3));
                closed_circle_type = false;
                break;
            case 9:
                circle_positions_length = 4;
                circle_positions[0] = new Coordinate(2, (mazeSize.y - 2));
                circle_positions[1] = new Coordinate(0, (mazeSize.y - 2));
                circle_positions[2] = new Coordinate(0, (mazeSize.y - 3));
                circle_positions[3] = new Coordinate(2, (mazeSize.y - 3));
                closed_circle_type = false;
                break;
            case 10:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate(0, 1);
                circle_positions[1] = new Coordinate(1, 0);
                circle_positions[2] = new Coordinate(2, 2);
                closed_circle_type = true;
                break;
            case 11:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate((mazeSize.x - 2), 0);
                circle_positions[1] = new Coordinate((mazeSize.x - 1), 1);
                circle_positions[2] = new Coordinate((mazeSize.x - 3), 2);
                closed_circle_type = true;
                break;
            case 12:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate((mazeSize.x - 2), (mazeSize.y - 1));
                circle_positions[1] = new Coordinate((mazeSize.x - 1), (mazeSize.y - 2));
                circle_positions[2] = new Coordinate((mazeSize.x - 3), (mazeSize.y - 3));
                closed_circle_type = true;
                break;
            case 13:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate(1, (mazeSize.y - 1));
                circle_positions[1] = new Coordinate(0, (mazeSize.y - 2));
                circle_positions[2] = new Coordinate(2, (mazeSize.y - 3));
                closed_circle_type = true;
                break;
            case 14:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate(0, 2);
                circle_positions[1] = new Coordinate(1, 0);
                circle_positions[2] = new Coordinate(2, 3);
                closed_circle_type = true;
                break;
            case 15:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate((mazeSize.x - 1), 2);
                circle_positions[1] = new Coordinate((mazeSize.x - 2), 0);
                circle_positions[2] = new Coordinate((mazeSize.x - 3), 3);
                closed_circle_type = true;
                break;
            case 16:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate((mazeSize.x - 1), (mazeSize.y - 3));
                circle_positions[1] = new Coordinate((mazeSize.x - 2), (mazeSize.y - 1));
                circle_positions[2] = new Coordinate((mazeSize.x - 3), (mazeSize.y - 4));
                closed_circle_type = true;
                break;
            case 17:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate(0, (mazeSize.y - 3));
                circle_positions[1] = new Coordinate(1, (mazeSize.y - 1));
                circle_positions[2] = new Coordinate(2, (mazeSize.y - 4));
                closed_circle_type = true;
                break;
            case 18:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate(2, 0);
                circle_positions[1] = new Coordinate(0, 1);
                circle_positions[2] = new Coordinate(3, 2);
                closed_circle_type = true;
                break;
            case 19:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate((mazeSize.x - 3), 0);
                circle_positions[1] = new Coordinate((mazeSize.x - 1), 1);
                circle_positions[2] = new Coordinate((mazeSize.x - 4), 2);
                closed_circle_type = true;
                break;
            case 20:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate((mazeSize.x - 3), (mazeSize.y - 1));
                circle_positions[1] = new Coordinate((mazeSize.x - 1), (mazeSize.y - 2));
                circle_positions[2] = new Coordinate((mazeSize.x - 4), (mazeSize.y - 3));
                closed_circle_type = true;
                break;
            case 21:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate(2, (mazeSize.y - 1));
                circle_positions[1] = new Coordinate(0, (mazeSize.y - 2));
                circle_positions[2] = new Coordinate(3, (mazeSize.y - 3));
                closed_circle_type = true;
                break;
            case 22:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate(0, 3);
                circle_positions[1] = new Coordinate(1, 0);
                circle_positions[2] = new Coordinate(2, 4);
                closed_circle_type = true;
                break;
            case 23:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate((mazeSize.x - 1), 3);
                circle_positions[1] = new Coordinate((mazeSize.x - 2), 0);
                circle_positions[2] = new Coordinate((mazeSize.x - 3), 4);
                closed_circle_type = true;
                break;
            case 24:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate((mazeSize.x - 1), (mazeSize.y - 4));
                circle_positions[1] = new Coordinate((mazeSize.x - 2), (mazeSize.y - 1));
                circle_positions[2] = new Coordinate((mazeSize.x - 3), (mazeSize.y - 5));
                closed_circle_type = true;
                break;
            case 25:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate(0, (mazeSize.y - 4));
                circle_positions[1] = new Coordinate(1, (mazeSize.y - 1));
                circle_positions[2] = new Coordinate(2, (mazeSize.y - 5));
                closed_circle_type = true;
                break;
            case 26:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate(3, 0);
                circle_positions[1] = new Coordinate(0, 1);
                circle_positions[2] = new Coordinate(4, 2);
                closed_circle_type = true;
                break;
            case 27:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate((mazeSize.x - 4), 0);
                circle_positions[1] = new Coordinate((mazeSize.x - 1), 1);
                circle_positions[2] = new Coordinate((mazeSize.x - 5), 2);
                closed_circle_type = true;
                break;
            case 28:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate((mazeSize.x - 4), (mazeSize.y - 1));
                circle_positions[1] = new Coordinate((mazeSize.x - 1), (mazeSize.y - 2));
                circle_positions[2] = new Coordinate((mazeSize.x - 5), (mazeSize.y - 3));
                closed_circle_type = true;
                break;
            case 29:
                circle_positions_length = 3;
                circle_positions[0] = new Coordinate(3, (mazeSize.y - 1));
                circle_positions[1] = new Coordinate(0, (mazeSize.y - 2));
                circle_positions[2] = new Coordinate(4, (mazeSize.y - 3));
                closed_circle_type = true;
                break;
            case 30:
                circle_positions_length = 3;
                circle_positions[1] = new Coordinate((apple.x - 1), (mazeSize.y - 1));
                circle_positions[0] = new Coordinate(apple.x, (mazeSize.y - 2));
                circle_positions[2] = new Coordinate((apple.x - 2), (mazeSize.y - 3));
                closed_circle_type = false;
                break;
            case 31:
                circle_positions_length = 3;
                circle_positions[1] = new Coordinate((apple.x + 1), (mazeSize.y - 1));
                circle_positions[0] = new Coordinate(apple.x, (mazeSize.y - 2));
                circle_positions[2] = new Coordinate((apple.x + 2), (mazeSize.y - 3));
                closed_circle_type = false;
                break;
            case 32:
                circle_positions_length = 3;
                circle_positions[1] = new Coordinate((apple.x + 1), 0);
                circle_positions[0] = new Coordinate(apple.x, 1);
                circle_positions[2] = new Coordinate((apple.x + 2), 2);
                closed_circle_type = false;
                break;
            case 33:
                circle_positions_length = 3;
                circle_positions[1] = new Coordinate((apple.x - 1), 0);
                circle_positions[0] = new Coordinate(apple.x, 1);
                circle_positions[2] = new Coordinate((apple.x - 2), 2);
                closed_circle_type = false;
                break;
            case 34:
                circle_positions_length = 3;
                circle_positions[1] = new Coordinate((mazeSize.x - 1), (apple.y - 1));
                circle_positions[0] = new Coordinate((mazeSize.x - 2), apple.y);
                circle_positions[2] = new Coordinate((mazeSize.x - 3), (apple.y - 2));
                closed_circle_type = false;
                break;
            case 35:
                circle_positions_length = 3;
                circle_positions[1] = new Coordinate(0, (apple.y - 1));
                circle_positions[0] = new Coordinate(1, apple.y);
                circle_positions[2] = new Coordinate(2, (apple.y - 2));
                closed_circle_type = false;
                break;
            case 36:
                circle_positions_length = 3;
                circle_positions[1] = new Coordinate(0, (apple.y + 1));
                circle_positions[0] = new Coordinate(1, apple.y);
                circle_positions[2] = new Coordinate(2, (apple.y + 2));
                closed_circle_type = false;
                break;
            case 37:
                circle_positions_length = 3;
                circle_positions[1] = new Coordinate((mazeSize.x - 1), (apple.y + 1));
                circle_positions[0] = new Coordinate((mazeSize.x - 2), apple.y);
                circle_positions[2] = new Coordinate((mazeSize.x - 3), (apple.y + 2));
                closed_circle_type = false;
                break;
            case 38:
                circle_positions_length = 7;
                circle_positions[0] = new Coordinate((apple.x + 2), (mazeSize.y - 3));
                circle_positions[1] = new Coordinate((apple.x + 2), (mazeSize.y - 1));
                circle_positions[2] = new Coordinate((apple.x + 1), (mazeSize.y - 1));
                circle_positions[3] = new Coordinate(apple.x, (mazeSize.y - 2));
                circle_positions[4] = new Coordinate((apple.x - 1), (mazeSize.y - 1));
                circle_positions[5] = new Coordinate((apple.x - 2), (mazeSize.y - 1));
                circle_positions[6] = new Coordinate((apple.x - 2), (mazeSize.y - 3));
                backwards_indicator_position = new Coordinate((apple.x + 1), (mazeSize.y - 2));
                closed_circle_type = true;
                break;
            case 39:
                circle_positions_length = 7;
                circle_positions[0] = new Coordinate((apple.x + 2), 2);
                circle_positions[1] = new Coordinate((apple.x + 2), 0);
                circle_positions[2] = new Coordinate((apple.x + 1), 0);
                circle_positions[3] = new Coordinate(apple.x, 1);
                circle_positions[4] = new Coordinate((apple.x - 1), 0);
                circle_positions[5] = new Coordinate((apple.x - 2), 0);
                circle_positions[6] = new Coordinate((apple.x - 2), 2);
                backwards_indicator_position = new Coordinate((apple.x + 1), 1);
                closed_circle_type = true;
                break;
            case 40:
                circle_positions_length = 7;
                circle_positions[0] = new Coordinate((mazeSize.x - 3), (apple.y + 2));
                circle_positions[1] = new Coordinate((mazeSize.x - 1), (apple.y + 2));
                circle_positions[2] = new Coordinate((mazeSize.x - 1), (apple.y + 1));
                circle_positions[3] = new Coordinate((mazeSize.x - 2), apple.y);
                circle_positions[4] = new Coordinate((mazeSize.x - 1), (apple.y - 1));
                circle_positions[5] = new Coordinate((mazeSize.x - 1), (apple.y - 2));
                circle_positions[6] = new Coordinate((mazeSize.x - 3), (apple.y - 2));
                backwards_indicator_position = new Coordinate((mazeSize.x - 2), (apple.y + 1));
                closed_circle_type = true;
                break;
            case 41:
                circle_positions_length = 7;
                circle_positions[0] = new Coordinate(2, (apple.y + 2));
                circle_positions[1] = new Coordinate(0, (apple.y + 2));
                circle_positions[2] = new Coordinate(0, (apple.y + 1));
                circle_positions[3] = new Coordinate(1, apple.y);
                circle_positions[4] = new Coordinate(0, (apple.y - 1));
                circle_positions[5] = new Coordinate(0, (apple.y - 2));
                circle_positions[6] = new Coordinate(2, (apple.y - 2));
                backwards_indicator_position = new Coordinate(1, (apple.y + 1));
                closed_circle_type = true;
                break;
            default:
                break;
        }
    }

    /**
     * Returns the direction needed to follow the circle pattern
     * Uses pathTo() to determine the direction from current position to the next circle position
     * When a position is reached, selects the next position
     *
     * @return the direction needed to go/continue circling
     */
    public Direction circle_direction() {
        if (head.equals(circle_positions[circle_positions_iterator % circle_positions_length])) {
            circle_positions_iterator++;
//            System.out.println("Found_position");
        }
        if (circle_type == 1) {
            if (circle_positions_iterator == 1 && head.equals(circle_positions[3])) {
                Coordinate temp2 = circle_positions[1];
                circle_positions[1] = circle_positions[3];
                circle_positions[3] = temp2;
                circle_positions_iterator = 2;
            } else if (circle_positions_iterator == 0 && head.equals(circle_positions[1])) {
                Coordinate temp2 = circle_positions[1];
                circle_positions[1] = circle_positions[3];
                circle_positions[3] = temp2;
            }
        }
        if (circle_type > 1 && circle_type < 10) {
            if (circle_positions_iterator == 1 && head.equals(circle_positions[3])) {
                Coordinate temp2 = circle_positions[1];
                circle_positions[1] = circle_positions[3];
                circle_positions[3] = temp2;
                circle_positions_iterator = 2;
            }
        }
        if (circle_type > 9 && circle_type < 38) {
            if (circle_positions_iterator == 1 && head.equals(circle_positions[2])) {
                Coordinate temp = circle_positions[0];
                circle_positions[0] = circle_positions[1];
                circle_positions[1] = temp;
                circle_positions_iterator = 0;
            }
        }
        if (circle_type > 37) {
            if (head.equals(circle_positions[3])) {
                found_position_5 = true;
            }
            if (head.equals(backwards_indicator_position) && found_position_5) {
                Coordinate temp2 = circle_positions[1];
                Coordinate temp3 = circle_positions[2];
                Coordinate temp4 = circle_positions[3];
                Coordinate temp5 = circle_positions[4];
                Coordinate temp6 = circle_positions[5];
                circle_positions[1] = circle_positions[6];
                circle_positions[6] = temp2;
                circle_positions[5] = temp3;
                circle_positions[4] = temp4;
                circle_positions[3] = temp5;
                circle_positions[2] = temp6;
                circle_positions_iterator = 5;
            }
            if (head.equals(circle_positions[2]) || head.equals(circle_positions[4]) || head.equals(circle_positions[0]) || head.equals(circle_positions[5])) {
                found_position_5 = false;
            }
        }
        if (circle_positions_iterator > 10) {
            circle_positions_iterator = circle_positions_iterator % circle_positions_length;
        }
        return pathTo(circle_positions[circle_positions_iterator % circle_positions_length]);
    }

    /**
     * checks if the circle is complete
     *
     * @param snake    our snake
     * @param opponent other snake
     * @param mazeSize the board
     * @param apple    the apple
     * @return true if complete, false if not yet
     */
    public boolean circleComplete(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        // first check if all circle positions are covered
        for (int i = 0; i < circle_positions_length; i++) {
            if (!snake.elements.contains(circle_positions[i])) {
                return false;
            }
        }

        makeOpponentBFSGraphBreakOnApple(snake, opponent, mazeSize, apple);
        if (closed_circle_type) {
            // here the opponent cannot have a path to the apple
            // and also checks if our head is next to our tail
            if (opponent_breakOnApple_cameFrom.get(apple) == null && distanceBetween(head, snake.body.getLast()) == 1) {
                return true;
            } else {
                return false;
            }
        } else {
            // here the opponent is allowed to have only 1 path to the apple
            // so check all tiles next to apple: only 1 of them should have a value in the opponent_breakOnApple_cameFrom
            int counter = 0;
            for (Direction dir : DIRECTIONS) {
                if (apple.moveTo(dir).inBounds(mazeSize)) {
                    if (opponent_breakOnApple_cameFrom.get(apple.moveTo(dir)) != null) {
                        counter++;
                    }
                }
            }
            if (counter <= 1 && distanceBetween(head, snake.body.getLast()) == 1) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Determine the furthest reachable point of the BFS graph (the highest depth)
     *
     * @param depth_grid the BFS graph on which you want to determine the furthest point
     * @param mazeSize   needed to check the whole grid
     * @return Coordinate, the furthest reachable point
     */
    private Coordinate getFurthestPoint(HashMap depth_grid, Coordinate mazeSize) {
        Coordinate destination = null; //
        int distance = 0;
        // Make it search for the longest path and make it go to that path
        for (int x = 0; x < mazeSize.x; x++)
            for (int y = 0; y < mazeSize.y; y++) {
                Coordinate check = new Coordinate(x, y);
                if (depth_grid.containsKey(check)) {
                    if ((int) depth_grid.get(check) > distance) {
                        destination = check;
                        distance = (int) depth_grid.get(check);
                    }
                }
            }
        return destination;
    }

    /**
     * Determines all the blocks reachable point of the BFS graph at a certain depth
     *
     * @param depth      The depth of which you want to find all the cords
     * @param depth_grid the BFS graph which you want to check
     * @param mazeSize   needed to check the whole grid
     * @return Coordinate, the furthest reachable point
     */
    private Coordinate[] findDepth(int depth, HashMap depth_grid, Coordinate mazeSize) {
        ArrayList sol = new ArrayList();
        for (int x = 0; x < mazeSize.x; x++)
            for (int y = 0; y < mazeSize.y; y++) {
                Coordinate check = new Coordinate(x, y);
                if (depth_grid.containsKey(check)) {
                    if (depth_grid.get(check).equals(depth)) {
                        sol.add(check);
                    }
                }
            }

        Coordinate[] answer = (Coordinate[]) sol.toArray(new Coordinate[sol.size()]);
        return answer;
    }

    /**
     * Returns the step toward your goal
     *
     * @param goal coordinate of the place where you want to go
     * @return Direction, the step to take or null if there is no route
     */
    private Direction pathTo(Coordinate goal) {
        ArrayList path = new ArrayList(); // This will be the route from the goal to the head
        if (snake_cameFrom.containsKey(goal)) { // If this is not the case, the goal is enclosed by a snake
            path.add((goal));
        } else {
            return null; // Path is unreachable
        }

        Direction move; // The command to move to
        while (path.get(path.size() - 1) != head) {
            path.add(snake_cameFrom.get(path.get(path.size() - 1)));
        }
        move = directionFromTo(head, (Coordinate) path.get(path.size() - 2));
        return move;
    }

    /**
     * Makes a worst case BFS graph of the new move and compares how much array it can reach
     * TODO This can also check if the highest depth of the new BFS is lower than the snakeSize  -> enclosing the snake
     * TODO currently only works for our own snake (a fix is to have the depth and camefrom graph as input)
     * This can also be used to make a general worst case BFS graph
     *
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @param move     The move you want to check
     * @return returns the number of reachable blocks
     */
    int reachable_part(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple, Direction move) {
        Coordinate newHead;
        int correction = 0;
        if (move == null) {
            newHead = snake.getHead();
        } else {
            newHead = Coordinate.add(snake.getHead(), move.v);
            correction = 1;
        }

        // arrayVisited[x][y] is true if there is already a path created including x,y or you don't want to go over x,y
        boolean[][] arrayVisited = new boolean[mazeSize.x][mazeSize.y];

        arrayVisited[newHead.x][newHead.y] = true; // No need to check head

        // Set all positions in arrayVisited where a snake is to true, so the BFS does not consider this as a route option
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                if (snake.elements.contains(new Coordinate(x, y)) || opponent.elements.contains(new Coordinate(x, y))) {
                    arrayVisited[x][y] = true;
                }
            }
        }
        arrayVisited[snake.body.getLast().x][snake.body.getLast().y] = false; // We don't reach the tail anymore

        // Also ignore possible locations for the next head for the opponent to avoid tieing
        // Only if the player is behind, otherwise it's worth the risk
        // EDIT: never ignore new head location because that could block the snake
        for (Direction step : Direction.values()) {
            Coordinate newPos = Coordinate.add(opponent.getHead(), step.v);
            if (newPos.inBounds(mazeSize)) {
                arrayVisited[newPos.x][newPos.y] = true;
            }
        }
        arrayVisited[opponent.body.getLast().x][opponent.body.getLast().y] = false; // We don't reach the tail anymore

        // This is a dictionary where you can say per coordinate how to get to that coordinate from the snake
        Map<Coordinate, Coordinate> cameFrom = new HashMap<Coordinate, Coordinate>();
        Map<Coordinate, Integer> depth = new HashMap<Coordinate, Integer>(); // Depth = number of step required to get to coordinate
        cameFrom.put(newHead, snake.getHead()); //

        ArrayList queue = new ArrayList(); // The queue list needed for BFS
        queue.add(newHead); // Add head as starting point for the queue
        depth.put(newHead, 0 + correction);
        int depth_level_handled = 0 + correction; // Tells if we already tog
        int weCanReachApple = 0;
        int opponentCanReachApple = 0;
        // The main BFS loop
        while (queue.isEmpty() == false) {
            Coordinate start = (Coordinate) queue.get(0);
            Integer depth_level = depth.get(start) + 1; // The depth level

            if (apple.inBounds(mazeSize)) {
                if (snake_depth.containsKey(apple)) {
                    if (depth_level >= snake_depth.get(apple)) {
                        weCanReachApple = 1;
                    }
                }
                if (opponent_depth.containsKey(apple)) {
                    if (depth_level >= opponent_depth.get(apple)) {
                        opponentCanReachApple = 1;
                    }
                }
            }

            if (depth_level > depth_level_handled) { // So we make sure this loop is only executed once

                /* !! EXPERIMENTAL RULE !! marks all the possible head locations of the enemy snake as visited
                 * A possible tweak for this, is to only mark realistic head location as true or only upto a certain depth
                 * TODO Check if you adding this when making the BFS graph for our snake will make it become overly protective
                 * currently disabled, makes the snake overly protective
                 */
                //for (Coordinate possible_enemy_head : findDepth(depth_level, opponent_depth, mazeSize)) {
                //    arrayVisited[possible_enemy_head.x][possible_enemy_head.y] = true;
                //}

                if (depth_level < snake.body.size() + weCanReachApple) {
                    if (weCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_snake = (Coordinate) snake.body.toArray()[snake.body.size() + weCanReachApple - depth_level];
                        arrayVisited[remove_tail_snake.x][remove_tail_snake.y] = false;
                    } else { // Now we are at a depth higher than the snake size, thu
                        for (Coordinate empty : findDepth(depth_level - snake.body.size() - weCanReachApple, snake_depth, mazeSize)) {
                            arrayVisited[empty.x][empty.y] = false;
                        }
                    }
                }
                if (depth_level < opponent.body.size() + opponentCanReachApple) {
                    if (opponentCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_opponent = (Coordinate) opponent.body.toArray()[opponent.body.size() + opponentCanReachApple - depth_level];
                        arrayVisited[remove_tail_opponent.x][remove_tail_opponent.y] = false;
                    }
                } else {
                    for (Coordinate empty : findDepth(depth_level - opponentCanReachApple - opponent.body.size(), opponent_depth, mazeSize)) {
                        arrayVisited[empty.x][empty.y] = false;
                    }
                }
                depth_level_handled++;
            }


            for (Direction step : Direction.values()) {
                Coordinate newPos = Coordinate.add(start, step.v);
                if (newPos.inBounds(mazeSize)) { // Skip this if for positions out of the maze
                    if (!arrayVisited[newPos.x][newPos.y]) {
                        queue.add(newPos); // If this place is not visited add it to the queue
                        arrayVisited[newPos.x][newPos.y] = true; // We don't want to visit this place again.
                        if (!cameFrom.containsKey(newPos)) {
                            cameFrom.put(newPos, start); // Puts the location start in newPos so we can trace back how we got to the apple.
                            depth.put(newPos, depth_level);
                        }
                    }
                }
            /*
            if (newPos == apple) {
                break; // We found the apple in the fastest route, no need to look further
            } */
            }
            queue.remove(0); // Remove the checked position from the search array
        }
        return cameFrom.size();
    }

    /**
     * checks whether a move is safe
     *
     * @param snake    us
     * @param opponent enemy
     * @param mazeSize board
     * @param apple    apple
     * @param move     the move we're making
     * @return true if move is safe, false if it is dangerous (high chance of enclosing ourselves)
     */
    private boolean checkMove(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple, Direction move) {
        int new_reachable = reachable_part(snake, opponent, mazeSize, apple, move);
        if (((double) new_reachable / oldReachable) < 0.8) {
//            System.out.println("Let's not do that");
            // TODO check when this happens there is a real treat
            return false;
        }
        return true;
    }

    /**
     * Makes a BFS graph and
     *
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     *                 this does not have a return function but this:
     *                 makes a BFS graph of the current stat: init_cameFrom,
     *                 marks the depth (distance) for each reachable coordinate
     *                 notes the old size number of positions reachable for the snake
     */
    private void makeBFSGraph(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        oldReachable = snake_cameFrom.size();
        snake_cameFrom.clear();
        snake_depth.clear();
        Coordinate head = snake.getHead();
        /*
         * The BFS algorithm
         */
        // arrayVisited[x][y] is true if there is already a path created including x,y or you don't want to go over x,y
        boolean[][] arrayVisited = new boolean[mazeSize.x][mazeSize.y];

        arrayVisited[head.x][head.y] = true; // No need to check head
        if (dontTakeApple) {
            arrayVisited[apple.x][apple.y] = true;  // No paths going over the apple, when we don't want to do that
        }

        // Set all positions in arrayVisited where a snake is to true, so the BFS does not consider this as a route option
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                if (snake.elements.contains(new Coordinate(x, y)) || opponent.elements.contains(new Coordinate(x, y))) {
                    arrayVisited[x][y] = true;
                }
            }
        }

        //TODO: deze regel hieronder doet niks toch? Kan weg volgens mij (BramG)
        snake.body.getLast();
        // Also ignore possible locations for the next head for the opponent to avoid tieing
        // Only if the player is behind, otherwise it's worth the risk
        if (snake.body.size() <= opponent.body.size())
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(opponent.getHead(), move.v);
                if (newPos.inBounds(mazeSize)) {
                    arrayVisited[newPos.x][newPos.y] = true;
                }
            }

        // This is a dictionary where you can say per coordinate how to get to that coordinate from the snake
        // Depth = number of step required to get to coordinate
        snake_cameFrom.put(head, null); //

        ArrayList queue = new ArrayList(); // The queue list needed for BFS
        queue.add(head); // Add head as starting point for the queue
        snake_depth.put(head, 0);
        int depth_level_handled = 0; // Tells if we already tog
        int weCanReachApple = 0; // Turns one if true
        int opponentCanReachApple = 0;
        // The main BFS loop
        while (queue.isEmpty() == false) {
            Coordinate start = (Coordinate) queue.get(0);
            Integer depth_level = snake_depth.get(start) + 1; // The depth level
            if (apple.inBounds(mazeSize)) {
                if (snake_depth.containsKey(apple)) {
                    if (depth_level >= snake_depth.get(apple)) {
                        weCanReachApple = 1;
                    }
                }
                if (opponent_depth.containsKey(apple)) {
                    if (depth_level >= opponent_depth.get(apple)) {
                        opponentCanReachApple = 1;
                    }
                }
            }
            // TODO it doesn't account if the person eats an apple
            if (depth_level > depth_level_handled) { // So we make sure this loop is only executed once
                if (depth_level < snake.body.size() + weCanReachApple) {
                    if (weCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_snake = (Coordinate) snake.body.toArray()[snake.body.size() + weCanReachApple - depth_level];
                        arrayVisited[remove_tail_snake.x][remove_tail_snake.y] = false;
                    } else { // Now we are at a depth higher than the snake size, thu
                        for (Coordinate empty : findDepth(depth_level - snake.body.size() - weCanReachApple, snake_depth, mazeSize)) {
                            arrayVisited[empty.x][empty.y] = false;
                        }
                    }
                }
                if (depth_level < opponent.body.size() + opponentCanReachApple) {
                    if (opponentCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_opponent = (Coordinate) opponent.body.toArray()[opponent.body.size() + opponentCanReachApple - depth_level];
                        arrayVisited[remove_tail_opponent.x][remove_tail_opponent.y] = false;
                    }
                } else {
                    for (Coordinate empty : findDepth(depth_level - opponentCanReachApple - opponent.body.size(), opponent_depth, mazeSize)) {
                        arrayVisited[empty.x][empty.y] = false;
                    }
                }
                depth_level_handled++;
            }

        /* We skip this, because we want a full BFS
        if (snake_cameFrom.containsKey(apple)) {
            break; // Exit the loop, because we don't need to find the apple anymore
        }

         */
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(start, move.v);
                if (newPos.inBounds(mazeSize)) { // Skip this if for positions out of the maze
                    if (!arrayVisited[newPos.x][newPos.y]) {
                        queue.add(newPos); // If this place is not visited add it to the queue
                        arrayVisited[newPos.x][newPos.y] = true; // We don't want to visit this place again.
                        if (!snake_cameFrom.containsKey(newPos)) {
                            snake_cameFrom.put(newPos, start); // Puts the location start in newPos so we can trace back how we got to the apple.
                            snake_depth.put(newPos, depth_level);
                        }
                    }
                }
            /* We skip this, because we want to have an full BFS graph
            if (newPos == apple) {
                break; // We found the apple in the fastes route, no need to look further
            } */
            }
            queue.remove(0); // Remove the checked position from the search array
        }
    }

    /**
     * Makes a BFS graph of the opponent (this will be the most unaltered BFS graph
     *
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     *                 this does not have a return function but this:
     *                 makes a BFS graph of the current stat: opponent_cameFrom,
     *                 marks the depth (distance) for each reachable coordinate: opponent_depth
     *                 notes the old size number of positions reachable for the snake
     *                 Also includes a predictor if the opponent can be enclosed
     */
    private void makeOpponentBFSGraph(Snake opponent, Snake snake, Coordinate mazeSize, Coordinate apple) {
        opponent_cameFrom.clear();
        opponent_depth.clear();
        Coordinate opponent_head = snake.getHead();

        // arrayVisited[x][y] is true if there is already a path created including x,y or you don't want to go over x,y
        boolean[][] arrayVisited = new boolean[mazeSize.x][mazeSize.y];

        arrayVisited[opponent_head.x][opponent_head.y] = true; // No need to check head

        // Set all positions in arrayVisited where a snake is to true, so the BFS does not consider this as a route option
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                if (snake.elements.contains(new Coordinate(x, y)) || opponent.elements.contains(new Coordinate(x, y))) {
                    arrayVisited[x][y] = true;
                }
            }
        }

        // This is still enabled not, because if one of these move causes trouble for the opponent snake, this means we can
        // enclose it
        if (snake.body.size() <= opponent.body.size())
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(opponent.getHead(), move.v);
                if (newPos.inBounds(mazeSize)) {
                    arrayVisited[newPos.x][newPos.y] = true;
                }
            }

        // This is a dictionary where you can say per coordinate how to get to that coordinate from the snake
        // Depth = number of step required to get to coordinate
        opponent_cameFrom.put(opponent_head, null); //

        ArrayList queue = new ArrayList(); // The queue list needed for BFS
        queue.add(opponent_head); // Add head as starting point for the queue
        opponent_depth.put(opponent_head, 0);
        int weCanReachApple = 0; // Turns one if true
        int depth_level_handled = 0; // Turns one if true
        // The main BFS loop
        while (queue.isEmpty() == false) {
            Coordinate start = (Coordinate) queue.get(0);
            Integer depth_level = opponent_depth.get(start) + 1; // The depth level
            if (opponent_depth.containsKey(apple)) {
                if (depth_level >= opponent_depth.get(apple)) {
                    weCanReachApple = 1;
                }
            }
            // TODO it doesn't account if the person eats an apple
            if (depth_level > depth_level_handled) { // So we make sure this loop is only executed once
                if (depth_level < snake.body.size() + weCanReachApple) {
                    if (weCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_snake = (Coordinate) snake.body.toArray()[snake.body.size() + weCanReachApple - depth_level];
                        arrayVisited[remove_tail_snake.x][remove_tail_snake.y] = false;
                    }
                } else { // Now we need to edit all the places we visited in the arraylist
                    for (Coordinate empty : findDepth(depth_level - weCanReachApple - snake.body.size(), opponent_depth, mazeSize)) {
                        arrayVisited[empty.x][empty.y] = false;
                    }
                }
                if (depth_level < opponent.body.size()) {
                    Coordinate remove_tail_opponent = (Coordinate) opponent.body.toArray()[opponent.body.size() - depth_level];
                    arrayVisited[remove_tail_opponent.x][remove_tail_opponent.y] = false;
                } else { // TODO does not work because this is using the old BFS graph and is thus outdated
                    //for (Coordinate empty : findDepth(depth_level - opponentCanReachApple - opponent.body.size(), snake_depth,mazeSize)){
                    //    arrayVisited[empty.x][empty.y] = false;
                    //}
                }
                depth_level_handled++;
            }


            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(start, move.v);
                if (newPos.inBounds(mazeSize)) { // Skip this if for positions out of the maze
                    if (!arrayVisited[newPos.x][newPos.y]) {
                        queue.add(newPos); // If this place is not visited add it to the queue
                        arrayVisited[newPos.x][newPos.y] = true; // We don't want to visit this place again.
                        if (!opponent_cameFrom.containsKey(newPos)) {
                            opponent_cameFrom.put(newPos, start); // Puts the location start in newPos so we can trace back how we got to the apple.
                            opponent_depth.put(newPos, depth_level);
                        }
                    }
                }
            /* We skip this, because we want to have an full BFS graph
            if (newPos == apple) {
                break; // We found the apple in the fastes route, no need to look further
            } */
            }
            queue.remove(0); // Remove the checked position from the search array
        }

        if (opponent_oldReachable == 0) {
            opponent_oldReachable = opponent_cameFrom.size();
        }
        int opponent_newReachable = opponent_cameFrom.size();
        if (((double) opponent_newReachable / opponent_oldReachable) < 0.6) { // If the enemy lost significant movement
//            System.out.println("enemy snake lost a lot of movement space");
            checkCanEnclose = true;
        }
    }

    /**
     * Makes a BFS graph of the opponent, but it breaks paths when apple is reached
     *
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segment
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     *                 this does not have a return function but this:
     *                 makes a BFS graph of the current stat: opponent_breakOnApple_cameFrom,
     *                 marks the depth (distance) for each reachable coordinate: opponent_breakOnApple_depth
     */
    private void makeOpponentBFSGraphBreakOnApple(Snake opponent, Snake snake, Coordinate mazeSize, Coordinate apple) {
        opponent_breakOnApple_cameFrom.clear();
        opponent_breakOnApple_depth.clear();
        Coordinate opponent_head = snake.getHead();

        // arrayVisited[x][y] is true if there is already a path created including x,y or you don't want to go over x,y
        boolean[][] arrayVisited = new boolean[mazeSize.x][mazeSize.y];

        arrayVisited[opponent_head.x][opponent_head.y] = true; // No need to check head

        // Set all positions in arrayVisited where a snake is to true, so the BFS does not consider this as a route option
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                if (snake.elements.contains(new Coordinate(x, y)) || opponent.elements.contains(new Coordinate(x, y))) {
                    arrayVisited[x][y] = true;
                }
            }
        }

        // This is a dictionary where you can say per coordinate how to get to that coordinate from the snake
        // Depth = number of step required to get to coordinate
        opponent_breakOnApple_cameFrom.put(opponent_head, null); //

        ArrayList queue = new ArrayList(); // The queue list needed for BFS
        queue.add(opponent_head); // Add head as starting point for the queue
        opponent_breakOnApple_depth.put(opponent_head, 0);
        int weCanReachApple = 0; // Turns one if true
        int depth_level_handled = 0; // Turns one if true
        // The main BFS loop
        while (queue.isEmpty() == false) {
            Coordinate start = (Coordinate) queue.get(0);
            Integer depth_level = opponent_breakOnApple_depth.get(start) + 1; // The depth level

            /*
            if (opponent_breakOnApple_depth.containsKey(apple)) {
                if (depth_level >= opponent_breakOnApple_depth.get(apple)) {
                    weCanReachApple = 1;
                }
            }
            if (depth_level > depth_level_handled) { // So we make sure this loop is only executed once
                if (depth_level < snake.body.size() + weCanReachApple) {
                    if (weCanReachApple - depth_level != 0) {
                        Coordinate remove_tail_snake = (Coordinate) snake.body.toArray()[snake.body.size() + weCanReachApple - depth_level];
                        arrayVisited[remove_tail_snake.x][remove_tail_snake.y] = false;
                    }
                } else { // Now we need to edit all the places we visited in the arraylist
                    for (Coordinate empty : findDepth(depth_level - weCanReachApple -snake.body.size(), opponent_breakOnApple_depth, mazeSize)){
                        arrayVisited[empty.x][empty.y] = false;
                    }
                }
                if (depth_level < opponent.body.size()) {
                    Coordinate remove_tail_opponent = (Coordinate) opponent.body.toArray()[opponent.body.size() - depth_level];
                    arrayVisited[remove_tail_opponent.x][remove_tail_opponent.y] = false;
                } else { // TODO does not work because this is using the old BFS graph and is thus outdated
                    //for (Coordinate empty : findDepth(depth_level - opponentCanReachApple - opponent.body.size(), snake_depth,mazeSize)){
                    //    arrayVisited[empty.x][empty.y] = false;
                    //}
                }
                depth_level_handled++;
            }
            */

            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(start, move.v);
                if (newPos.inBounds(mazeSize)) { // Skip this if for positions out of the maze
                    if (!arrayVisited[newPos.x][newPos.y]) {
                        if (!newPos.equals(apple)) {
                            queue.add(newPos); // If this place is not visited add it to the queue, EXCEPT IF APPLE
                        }
                        arrayVisited[newPos.x][newPos.y] = true; // We don't want to visit this place again.
                        if (!opponent_breakOnApple_cameFrom.containsKey(newPos)) {
                            opponent_breakOnApple_cameFrom.put(newPos, start); // Puts the location start in newPos so we can trace back how we got to the apple.
                            opponent_breakOnApple_depth.put(newPos, depth_level);
                        }
                    }
                }
            }
            queue.remove(0); // Remove the checked position from the search array
        }
    }


    /* ******************************** *
     *                                  *
     *                                  *
     * ALPHA BETA FUNCTIONS START HERE  *
     *                                  *
     *                                  *
     * ******************************** */

    /**
     * Choose a direction with alpha beta with a time less then the usual 1000 milliseconds
     *
     * @param snake     Your snake's body with coordinates for each segment
     * @param opponent  Opponent snake's body with coordinates for each segment
     * @param mazeSize  Size of the board
     * @param apple     Coordinate of an apple
     * @param startTime the time our turn started
     * @return a direction to move towards
     */
    public Direction chooseDirectionAlphaBeta(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple, long startTime) {
        preVariableAssignment(snake, opponent, mazeSize, apple);
        final_move = Direction.UP;
        exit = false;
        Thread t1 = new Thread(this);
        t1.start();
        while (System.currentTimeMillis() - startTime < TIME_PER_TURN) {
        }
        exit = true;
        return final_move;
    }

    public void run() {
        int i = 2;
        //System.out.println("enter loop");
        while (!exit) {
            //timeLimit = (long) (System.currentTimeMillis() + ((long) 1000 * searchfactor));
            maxDepth = i;
            //System.out.println("depth " + i);
            try {
                final_move = makeMinMax();
            } catch (NullPointerException e) {
                //System.out.println("Exit with nullpointer");
                break;
            }
            i += 2;
        }
//        System.out.println("Depth level reached (shortPerc): " + (i - 2) / 2);
    }

    /**
     * Resets the variables
     *
     * @param snake1    Your snake's body with coordinates for each segment
     * @param opponent1 Opponent snake's body with coordinates for each segment
     * @param mazeSize1 Size of the board
     * @param apple1    Coordinate of an apple
     */
    private void preVariableAssignment(Snake snake1, Snake opponent1, Coordinate mazeSize1, Coordinate apple1) {
        if (snake1.body.size() < 5) { // Reset variables at new snake run
            chase_tail = false;
        }

        snake = snake1;
        opponent = opponent1;
        mazeSize = mazeSize1;
        apple = apple1;
        centre = new Coordinate(mazeSize.x / 2, mazeSize.y / 2);
        bestMove = null;
        bestScore = Integer.MIN_VALUE;
        appleSpots = null;
    }

    /**
     * The instruction to make a minmax graph
     * This uses the snake, opponent, we stored in makeminMax
     *
     * @return Direction of bot's move
     */
    private Direction makeMinMax() {
        if (chase_tail) { // We won
            return directionFromTo(snake.getHead(), snake.body.getLast());
        } else { // Do the normal strategy
            getAppleSpots();
            Object[] cur_state = {snake.clone(), opponent.clone(), -1, -1, -1, (double) 0};
            minmax(cur_state, cur_state, maxDepth, Integer.MIN_VALUE, Integer.MAX_VALUE, true); // minimax(state, newstate, depth, alpha, beta, maximizing player)

            if (distanceBetween(snake.getHead(), snake.body.getLast()) == 1 && circleScore(cur_state) >= scr_circlePart2 * 0.9) {
                chase_tail = true;
                return directionFromTo(snake.getHead(), snake.body.getLast());
            } else {
                if (bestMove != null) {
                    return bestMove;
                } else {
//                    System.out.println("We had a good run son");
                    return doValidMove(snake, opponent, mazeSize);
                }
            }
        }
    }

    private void getAppleSpots() {
        if (apple.inBounds(mazeSize)) {
            ArrayList<Coordinate> spots = new ArrayList<Coordinate>();
            for (Direction move : Direction.values()) {
                Coordinate newPos = Coordinate.add(apple, move.v);
                if (newPos.inBounds(mazeSize)) {
                    spots.add(newPos);
                }
            }
            appleSpots = (Coordinate[]) spots.toArray(new Coordinate[spots.size()]);
        }
    }

    /**
     * Choose the direction (not rational - silly)
     *
     * @param state            The new positions of the snake after a move. state[0] = our_snake, state[1] = opponent_snake, state[2] = depth_WeAteApple, state[3] = depth_OppAteApple
     * @param newState         The new proposed state which will replace state if depth is equal: both players have made a move
     * @param depth            The depth of the minmax graph, this is not equal to the distance like BFS
     * @param maximizingPlayer Is true when we are looking at out snake (thus maximsing our score), is false otherwise
     * @return Direction of bot's move
     */
    private int minmax(Object[] state, Object[] newState, int depth, int alpha, int beta, boolean maximizingPlayer) {
        if (depth % 2 == 0) { // We update the state once both snakes have made their move, otherwise the game.
            state = newState.clone();
        }
        Coordinate shead; // The head of the new snake
        Snake our_snake = (Snake) state[0];
        Snake opp_snake = (Snake) state[1];
        int depth_WeAteApple = (int) state[2]; // These stay here for clearness sake
        int depth_OppAteApple = (int) state[3];
        int depth_WeReachedCentre = (int) state[4]; // These stay here for clearness sake
        double percentage_or_diff_ratio = (double) state[5];
        Direction[] moves;

        // This if statement fetches the possible moves for the snake we are evaluating
        if (maximizingPlayer) {
            shead = our_snake.body.getFirst();
            moves = notLosingAlgorithm(state, maximizingPlayer); // Get moves for our snake
        } else {
            shead = opp_snake.body.getFirst();
            moves = notLosingAlgorithm(state, maximizingPlayer); // Get moves for the opponent snake
        }

        // Calculate score when we reached a dead end or the max depth
        if (depth == 0 || moves == null) {
            int score = calculateScore(state, maximizingPlayer, moves, depth);
            return score; // The heuristic value of the node
        }
        // Determine the move where we can get the maximum score giving the current state
        if (maximizingPlayer) { // Our snake
            int calc_score = Integer.MIN_VALUE; // The worst score possible
            for (Direction move : moves) { // Each child (also includes the childs killing itself, we filter this out by calculating the score)
                Coordinate newHead = Coordinate.add(shead, move.v);
                if (newHead.equals(opp_snake.body.getLast()) && distanceBetween(opp_snake.body.getFirst(), apple) == 1) {
                    return scr_WeCantMove;
                }
                Object[] updatedState = newState.clone(); // We clone newState, otherwise editing the updatedState will also edit newState

                Snake newSnake = our_snake.clone(); // We do the same thing as above
                // TODO find a better way for the below if the statements
                if (!newHead.equals(apple)) { // If this is not true we keep the last block of our snake, because we have eaten the apple
                    newSnake.body.removeLast(); // Remove the last block of our snake if we have not eaten the apple
                } else {
                    if (depth_OppAteApple == -1) { // If the opponent has not eaten the apple before we reached it
                        updatedState[2] = depth / 2;
                    }
                }
                if (newHead.equals(centre)) { // We reached the centre
                    updatedState[4] = depth / 2;
                }
                newSnake.body.addFirst(newHead); // Update the newSnake with its new head position
                updatedState[0] = newSnake; // Store the new snake in updatedState[0]

                int new_score = minmax(state, updatedState, depth - 1, alpha, beta, false); // Get the score of this move

                if (new_score > calc_score) { // If the score of this move is better than the best score we had, store the new move
                    calc_score = new_score;
                    if (depth == maxDepth) { // Store the best move for the snake to do
                        bestMove = move;
                        bestScore = new_score;
                    }
                }
                alpha = Math.max(alpha, new_score);
                if (alpha >= beta) {
                    break;
                }
            }
            return calc_score; // Return the best score for this state (the move we will most likely do)

        } else { // For the opponent we know the move to get the worst score possible for our snake, because this is the best move
            int calc_score = Integer.MAX_VALUE; // The best score possible
            for (Direction move : moves) { // The rest works the same as above unless there is a comment
                Coordinate newHead = Coordinate.add(shead, move.v);
                Object[] updatedState = newState.clone();
                Snake newSnake = opp_snake.clone();

                if (!newHead.equals(apple)) {
                    newSnake.body.removeLast();
                } else {
                    if (depth_WeAteApple == -1) { // Our snake has not yet eaten the apple
                        updatedState[3] = (depth + 1) / 2;
                    }
                }
                newSnake.body.addFirst(newHead);
                updatedState[1] = newSnake; // Opp snakes move
                if ((int) updatedState[3] == (depth + 1) / 2 || (int) updatedState[2] == (depth + 1) / 2) { // If someone ate an apple at this depth layer
                    // Add the reachable score once the apple is eaten
                    updatedState[5] = (double) simplePercentageOfTilesCloserToUsDos((Snake) updatedState[0], (Snake) updatedState[1], mazeSize, apple);
                }
                int new_score = minmax(state, updatedState, depth - 1, alpha, beta, true);
                if (new_score < calc_score) { // If the score of this move is worse than the worst score we had, store the new move
                    calc_score = new_score;
                }
                beta = Math.min(beta, new_score);
                if (alpha >= beta) {
                    break;
                }
            }
            return calc_score; // Return the worst score possible for this state (the move the opponent will most likely do)
        }
    }

    /**
     * Calculates the heuristic score of the state
     *
     * @param state      The new positions of the snake after a move. state[0] = our_snake, state[1] = opponent_snake, state[2] = weAteApple, state[3] = oppAteApple
     * @param isOurSnake Is true when we are looking at out snake (thus maximsing our score), is false otherwise
     * @param moves      The moves that makes the snake not collide with itself, or the opponent
     * @param depth      Depth of graph --> depth/2 = How many moves foward we are currently looking
     * @return The heuristic score of the state
     */
    private int calculateScore(Object[] state, boolean isOurSnake, Direction[] moves, int depth) {
        if (exit) {
            throw new NullPointerException();
        }
        int score = 0;
        Snake our_snake = (Snake) state[0];
        Snake opp_snake = (Snake) state[1];

        int depth_weAteApple = (int) state[2];
        int depth_oppAteApple = (int) state[3];
        int depth_WeReachedCentre = (int) state[4];
        boolean noOneAteApple = (depth_weAteApple == -1 && depth_oppAteApple == -1);
        Coordinate snake_head = our_snake.body.getFirst();
        Coordinate opp_head = opp_snake.body.getFirst();
        int our_snake_size = our_snake.body.size();
        int opp_snake_size = opp_snake.body.size();
        int diff_size = our_snake_size - opp_snake_size;

        // Give an incentive to move towards the apple if it has not been eaten
        score += circleScore(state);

        // Give an incentive to move towards the apple if it has not been eaten
        score += ScoreForGoingSomewhere(noOneAteApple, state);

        // If this state leads to a dead end
        score += scoreDeadEnd(isOurSnake, moves, depth);

        // Check for head collision
        score += scoreHeadCollision(snake_head, opp_head, diff_size);

        // Check for other collisions
        score += scoreCollisions(our_snake, opp_snake);

        // TODO fill in more parameters

        return score;
    }

    private int ScoreForGoingSomewhere(boolean noOneAteApple, Object[] state) {
        int distanceToApple = 0;
//        int distanceToCentre = 0;
        double percentageCloserToUs = 0;
        int score = 0;
        Snake our_snake = (Snake) state[0];
        Snake opp_snake = (Snake) state[1];
        int depth_weAteApple = (int) state[2];
        int depth_oppAteApple = (int) state[3];
        int depth_WeReachedCentre = (int) state[4];
        double percentage_or_diff_ratio = (double) state[5];
        Coordinate snake_head = our_snake.getHead();
        if (noOneAteApple) {
            distanceToApple += distanceBetween(snake_head, apple);
        } else { // Give an incentive to move towards the centre so there is a better chance to reach the next apple
            if (depth_weAteApple == -1) { // The opponent ate the apple
                score += scr_OppAteApple - depth_oppAteApple;
//                if (!(depth_WeReachedCentre == -1)) { // We have no reached the apple
//                    score += scr_weReachedCentre + depth_WeReachedCentre * 10;
//                }
                percentageCloserToUs = percentage_or_diff_ratio;
//                distanceToCentre += distanceBetween(snake_head, centre);
            } else { // We ate the apple
                score += scr_WeAteApple + depth_weAteApple;
            }
        }
        return score + (fct_distanceToApple * distanceToApple + (int) (fct_earlyReachableTiles * percentageCloserToUs));
    }

    private int scoreHeadCollision(Coordinate snake_head, Coordinate opp_head, int diff_size) {
        int GoodHeadColl = 0;
        int BadHeadColl = 0;
        if (snake_head.equals(opp_head)) {
            if (diff_size > 0) {
                GoodHeadColl += 1;
            } else if (diff_size < 0) {
                BadHeadColl += 1;
            }
        }
        return (scr_BadHeadColl * BadHeadColl
                + scr_GoodHeadColl * GoodHeadColl);
    }

    private int scoreDeadEnd(boolean isOurSnake, Direction[] moves, int depth) {
        if (moves == null) {
            int WeCantMove = 0;
            int OppCantMove = 0;
            if (isOurSnake) { // We went to a dead end
                WeCantMove += 1;
            } else {    // The opponent went to a dead end
                OppCantMove += 1;
            }
            return (scr_WeCantMove * WeCantMove
                    + scr_OppCantMove * OppCantMove) * depth + 1;
        } else {
            return 0;
        }
    }

    private int scoreCollisions(Snake our_snake, Snake opp_snake) {
        final Coordinate snake_head = our_snake.body.getFirst();
        final Coordinate opp_head = opp_snake.body.getFirst();
        int WeCantMove = 0;
        int OppCantMove = 0;

        Deque our_snake_nohead = our_snake.body; // Make a new snake where the head is removed, to check if the head is somewhere else in the snake
        our_snake_nohead.removeFirst();            // NOTE: check_snake now also does not have a head
        Deque opp_snake_nohead = opp_snake.body;
        opp_snake_nohead.removeFirst();              // NOTE: opp_snake now also does not have a head

        //Check collisions
        if (our_snake_nohead.contains(snake_head) || opp_snake_nohead.contains(snake_head)) { // We collide with something
            WeCantMove += 1;
        }
        if (our_snake_nohead.contains(opp_head) || opp_snake_nohead.contains(opp_head)) { // The opponent collided with itself, so this is good
            OppCantMove += 1;
        }

        // Add head back again
        our_snake_nohead.addFirst(snake_head);
        opp_snake_nohead.addFirst(opp_head);

        return (scr_WeCantMove * WeCantMove
                + scr_OppCantMove * OppCantMove);
    }

    private int circleScore(Object[] state) {
        Snake our_snake = (Snake) state[0];
        int opp_snake_size = ((Snake) state[1]).body.size();
        int our_snake_size = our_snake.body.size();
        int depth_weAteApple = (int) state[2];
        int depth_oppAteApple = (int) state[3];
        int diff_size = our_snake_size - opp_snake_size;
        boolean noOneAteApple = (depth_weAteApple == -1 && depth_oppAteApple == -1);
        int losingLead = 0;
        int circle_part1 = 0;
        int circle_part2 = 0;
        Coordinate snake_head = our_snake.body.getFirst();


        if (diff_size >= 1) { // We're in the lead, no need to eat the apple
            if (depth_oppAteApple > -1) {
                losingLead += 1; // Because the opponent ate the apple we lost the lead
            } else if (noOneAteApple && our_snake_size >= 6 && our_snake.body.size() % 2 == 0) { // We can possibly encircle the position
                // TODO: define onRightSpotForCircle(check_snake) with Wolf's algrotihm
                circle_part1 = onRightSpotForCircle(our_snake); // Calculate the probability of the circle working
                if (circle_part1 > 0) { // There is a chance to circle
                    if (circle_part1 == 1) { // Not fully enclosed
                        if (diff_size >= 2 && distanceBetween(snake_head, our_snake.body.getLast()) == 1) {
                            circle_part2 = 1;
                        }
                    } else { // Fully enclosed
                        if (distanceBetween(snake_head, our_snake.body.getLast()) == 1) { // TODO edit this, so it will follow its tail
                            circle_part2 = 1;
                        }
                    }
                }
            }
        }
        return (scr_circlePart1 * circle_part1
                + scr_circlePart2 * circle_part2
                + scr_losingLead * losingLead);
    }

    /**
     * Set direction from one point to other point
     *
     * @param our_snake point where we want to know the distance towards the apple from
     *                  appleSpots: the coordinates the snake needs to cover
     * @return direction // TODO direction? (Bram)
     */
    private int onRightSpotForCircle(Snake our_snake) {
        int arraySize = appleSpots.length;
        int spotsCovered = 0;
        for (int i = 0; i < arraySize; i++) {
            if (our_snake.body.contains(appleSpots[i])) { // We are on one of the spots
                spotsCovered++;
            }
        }

        if (spotsCovered >= arraySize) { // We covered at least all but one spot > exiting is hard
            return 1; // Good scenario
        } else {
            return 0; // Nothing special
        }
    }

    /**
     * Calculates manhattan distance between point 1, and point 2
     *
     * @param point1 Coordinate
     * @param point2 Coodrinate
     * @return distance
     */
    private int distanceBetween(Coordinate point1, Coordinate point2) {
        return Math.abs(point1.x - point2.x) + Math.abs(point1.y - point2.y);
    }

    /**
     * Set direction from one point to other point
     *
     * @param start point to begin
     * @param other point to move
     * @return direction
     */
    public Direction directionFromTo(Coordinate start, Coordinate other) {
        final Coordinate vector = new Coordinate(other.x - start.x, other.y - start.y);
        if (vector.x > 0) {
            return Direction.RIGHT;
        } else if (vector.x < 0) {
            return Direction.LEFT;
        }
        if (vector.y > 0) {
            return Direction.UP;
        } else if (vector.y < 0) {
            return Direction.DOWN;
        }
        for (Direction direction : Direction.values())
            if (direction.dx == vector.x && direction.dy == vector.y)
                return direction;
        return null;
    }

    /**
     * The possible moves the snake can use
     *
     * @param cur_state  the current state
     * @param isOurSnake if we want to determine the moves for our snake
     * @return direction
     */
    private Direction[] notLosingAlgorithm(Object[] cur_state, Boolean isOurSnake) {
        if (exit) {
            throw new NullPointerException();
        }
        Snake move_snake; // the snake were we want to determine the possible moves of
        Snake opp_snake;
        boolean bool_weAteApple = false;
        boolean bool_oppAteApple = false;
        if (isOurSnake) { // Assign the right snakes and check if the apple has been eaten
            move_snake = (Snake) cur_state[0];
            opp_snake = (Snake) cur_state[1];
            if ((int) cur_state[2] > -1) {
                bool_weAteApple = true;
            } else if ((int) cur_state[3] > -1) {
                bool_oppAteApple = true;
            }
        } else {
            move_snake = (Snake) cur_state[1];
            opp_snake = (Snake) cur_state[0];
            if ((int) cur_state[3] > -1) {
                bool_weAteApple = true;
            } else if ((int) cur_state[2] > -1) {
                bool_oppAteApple = true;
            }
        }


        Coordinate snakehead = move_snake.getHead();

        // Get the coordinate of the second element of the snake's body to prevent going backwards
        Coordinate afterHeadNotFinal = null;
        if (move_snake.body.size() >= 2) {
            Iterator<Coordinate> it = move_snake.body.iterator();
            it.next();
            afterHeadNotFinal = it.next();
        }

        final Coordinate afterHead = afterHeadNotFinal;

        // The only illegal move is going backwards. Here we are checking for not doing it
        Direction[] validMoves = Arrays.stream(DIRECTIONS)
                .filter(d -> !snakehead.moveTo(d).equals(afterHead)) // Filter out the backwards move
                .sorted()
                .toArray(Direction[]::new);

        final boolean weAteApple = bool_weAteApple;
        final boolean oppAteApple = bool_oppAteApple;

        // Just naïve greedy algorithm that tries not to die at each moment in time
        Direction[] notLosing = Arrays.stream(validMoves)
                .filter(d -> snakehead.moveTo(d).inBounds(mazeSize))             // Don't leave maze
                .filter(d -> (!opp_snake.body.contains(snakehead.moveTo(d))) || (snakehead.moveTo(d).equals(opp_snake.body.getLast()) && !oppAteApple))   // Don't collide with opponent...
                .filter(d -> (!move_snake.body.contains(snakehead.moveTo(d))) || (snakehead.moveTo(d).equals(move_snake.body.getLast()) && !weAteApple))     // and yourself
                .sorted()
                .toArray(Direction[]::new);

        if (notLosing.length > 0) return notLosing;
        else return null; // No possible moves
    }

    /**
     * Do a valid move when no moves possible
     */
    private Direction doValidMove(Snake snake1, Snake opponent1, Coordinate mazeSize) {
        Coordinate snakehead = snake1.getHead();

        // Get the coordinate of the second element of the snake's body to prevent going backwards
        Coordinate afterHeadNotFinal = null;


        if (snake1.body.size() >= 2) {
            Iterator<Coordinate> it = snake1.body.iterator();
            it.next();
            afterHeadNotFinal = it.next();
        }

        final Coordinate afterHead = afterHeadNotFinal;

        // The only illegal move is going backwards. Here we are checking for not doing it
        Direction[] validMoves = Arrays.stream(DIRECTIONS)
                .filter(d -> !snakehead.moveTo(d).equals(afterHead)) // Filter out the backwards move
                .sorted()
                .toArray(Direction[]::new);

        return validMoves[0];
    }


    /**
     * computes the fraction of the tiles that are closer to us
     * meaning: tiles that we can reach earlier than the opponent (checked with BFS in this function)
     * running time warning: this function produces a full BFS graph for both snakes when called
     * (if that is no problem, use this one, since it is more accurate.
     * Otherwise use the simpler version of this function, called: simplePercentageOfTilesCloserToUs.)
     *
     * @param snake1    us
     * @param opponent1 opponent
     * @param mazeSize  the board
     * @param apple     the apple
     * @return a number between 0.0 and 1.0, giving the fraction of tiles closer to us
     */
    private double percentageOfTilesCloserToUs(Snake snake1, Snake opponent1, Coordinate mazeSize, Coordinate apple) {

        // maybe we shouldn't do this inside the function:
        makeBFSGraph(snake1, opponent1, mazeSize, apple);
        makeOpponentBFSGraph(snake1, opponent1, mazeSize, apple);
        // (because this function will get called many times, right?)
        // but a new BFS is needed every time snake1 or opponent1 has a different location
        // if this functionality takes too long, there's a more simplistic function below
        // called: simplePercentageOfTilesCloserToUs

        int nrTilesCloserToUs = 0;
        int nrTilesCloserToOpponent = 0;

        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                Coordinate tile = new Coordinate(x, y);
                if (snake_depth.get(tile) != null && opponent_depth.get(tile) != null) {
                    if (snake_depth.get(tile) < opponent_depth.get(tile)) {
                        nrTilesCloserToUs++;
                    } else if (snake_depth.get(tile) > opponent_depth.get(tile)) {
                        nrTilesCloserToOpponent++;
                    } // else: exactly equally close, tile doesn't count to either snake in that case
                } else if (snake_depth.get(tile) != null && opponent_depth.get(tile) == null) {
                    nrTilesCloserToUs++;
                } else if (snake_depth.get(tile) == null && opponent_depth.get(tile) != null) {
                    nrTilesCloserToOpponent++;
                } // else: nobody can reach that tile apparently, nothing needs to be done in that case
            }
        }
        int nrReachableTiles = nrTilesCloserToUs + nrTilesCloserToOpponent;
        if (nrReachableTiles != 0) {
            return nrTilesCloserToUs / (double) nrReachableTiles;
        } else {
            return 0.5;  // if no tiles are reachable by anyone, I guess we're even
        }
    }

    /**
     * computes the fraction of the tiles that are closer to us
     * warning: this might NOT actually be the fraction of tiles that we can reach earlier than the opponent
     * (because, snake parts can be in the way, etc.)
     * however, it can be a good estimate.
     *
     * @param snake1    us
     * @param opponent1 opponent
     * @param mazeSize  the board
     * @param apple     the apple
     * @return a number between 0.0 and 1.0, giving (estimate of) the fraction of tiles closer to us
     */
    private double simplePercentageOfTilesCloserToUs(Snake snake1, Snake opponent1, Coordinate mazeSize, Coordinate apple) {
        int nrTilesCloserToUs = 0;
        int nrTilesCloserToOpponent = 0;
        Coordinate ourHead = snake1.getHead();
        Coordinate opponentHead = opponent1.getHead();
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                Coordinate tile = new Coordinate(x, y);
                if (distanceBetween(ourHead, tile) < distanceBetween(opponentHead, tile)) {
                    nrTilesCloserToUs++;
                } else if (distanceBetween(ourHead, tile) > distanceBetween(opponentHead, tile)) {
                    nrTilesCloserToOpponent++;
                } // else: equal distance, doesn't count
            }
        }
        int nrReachableTiles = nrTilesCloserToUs + nrTilesCloserToOpponent;
        return nrTilesCloserToUs / (double) nrReachableTiles;
    }

    private double simplePercentageOfTilesCloserToUsDos(Snake snake1, Snake opponent1, Coordinate mazeSize, Coordinate apple) {
        int nrTilesCloserToUs = 0;
        int nrTilesCloserToOpponent = 0;
        Coordinate ourHead = snake1.getHead();
        Coordinate opponentHead = opponent1.getHead();
        for (int x = 0; x < mazeSize.x; x++) {
            for (int y = 0; y < mazeSize.y; y++) {
                Coordinate tile = new Coordinate(x, y);
                if (!(snake1.elements.contains(tile) || opponent1.elements.contains(tile))) {
                    if (distanceBetween(ourHead, tile) < distanceBetween(opponentHead, tile)) {
                        nrTilesCloserToUs++;
                    } else if (distanceBetween(ourHead, tile) > distanceBetween(opponentHead, tile)) {
                        nrTilesCloserToOpponent++;
                    } // else: equal distance, doesn't count
                }
            }
        }
        int nrReachableTiles = nrTilesCloserToUs + nrTilesCloserToOpponent;
        return nrTilesCloserToUs / (double) nrReachableTiles;
    }

}
