package mcts;

import java.util.Deque;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

import snakes.Bot;
// import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;


class Node {
    public Direction s1_move;
    public Direction s2_move;
    Coordinate apple;
    Snake snake1;
    Snake snake2;
    HashSet<Node> children = new HashSet<>();
    int visits = 0;
    float UCB = 0;

    public Node(Direction m1, Direction m2, Coordinate a, Snake s1, Snake s2)
    {
        s1_move = m1;
        s2_move = m2;
        apple = a;
        snake1 = s1;
        snake2 = s2;
    }

    public Node(Coordinate a, Snake s1, Snake s2)
    {
        apple = a;
        snake1 = s1;
        snake2 = s2;
    }
}

class Move
{
    Coordinate s1;
    Coordinate s2;

    public Move(Coordinate c, Coordinate c2)
    {
        s1 = c;
        s2 = c2;
    }
}

public class MCTSSnake implements Bot {//implements Bot, Runnable {

     /* Smart snake bot (brain of your snake) should choose step (direction where to go)
     * on each game step until the end of game
     *
     * @param snake    Your snake's body with coordinates for each segment
     * @param opponent Opponent snake's body with coordinates for each segme
     * @param mazeSize Size of the board
     * @param apple    Coordinate of an apple
     * @return Direction in which snake should crawl next game step
     */

    // return HashSet of all possible moves for given snake
    // INVALID = out of bounds, moving in direction of previous body segement
    // VALID = anything in bounds, running into other snake, running into self
    // QUESTIONS: Should we hardcode out running into any snake segments or let algorithm figure it out??
    HashSet<Coordinate> find_all_possible_moves(Snake s, final Coordinate mapsize)
    {
        HashSet<Coordinate> moves = new HashSet<Coordinate>();
        
        // VERIFY THIS!!
        Direction invalidDir = s.getHead().getDirection(s.body.getFirst());

        List<Direction> valid_dirs = new ArrayList<Direction>();
        valid_dirs.add(Direction.UP);
        valid_dirs.add(Direction.DOWN);
        valid_dirs.add(Direction.LEFT);
        valid_dirs.add(Direction.RIGHT);

        valid_dirs.remove(invalidDir);

        for (Direction dir : valid_dirs)
        {
            Coordinate possible = Coordinate.add(s.getHead(), dir.v);
            if (possible.inBounds(mapsize))
            {
                moves.add(possible);
            }
        }

        return moves;
    }

    HashSet<Move> find_all_valid_moves(Snake snake, Snake opponent, final Coordinate mapsize)
    {
        HashSet<Coordinate> s1_moves = find_all_possible_moves(snake, mapsize);
        HashSet<Coordinate> s2_moves = find_all_possible_moves(opponent, mapsize);

        HashSet<Move> valid_moves = new HashSet<Move>();  
        
        // does NOT check if it runs into itself or other snake
        for(Coordinate c : s1_moves)
        {
            for (Coordinate c2 : s2_moves)
            {
                valid_moves.add(new Move(c, c2));
            }
        }

        return valid_moves;
    }

    // if the node has children that have not been explored, explore and add to child list
    public void expand(Node head, Snake snake, Snake opponent, final Coordinate mapsize){

        HashSet<Move> valid_moves = find_all_valid_moves(snake, opponent, mapsize);

        // if num_children != num_possible_moves, then explore another child
        if (head.children.size() == valid_moves.size())
        {
            // explore another unvisited child
        }   

        // otherwise use UCB to determine next child
        else
        {
            
        }

    }


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

    @Override
    public Direction chooseDirection(final Snake snake, final Snake opponent,
                                     final Coordinate mazeSize, final Coordinate apple){
        // create root
        Node head = new Node(apple, snake, opponent);

        MCTSGameState currState = new MCTSGameState(snake, opponent, apple, mazeSize, 0, Direction.DOWN, Direction.RIGHT, 0);

        MCTSGameState next = MCTSGameState.runOneStep(currState.clone());
        System.out.println(next);

        // expand
            // enque to both snake objects
        
        //


        return Direction.DOWN;
    }
}

