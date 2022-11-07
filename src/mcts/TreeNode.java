package mcts;

// import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

// In order to remember how good each state is, we will keep track of 
// the results of a state in a class called Node. This class has some basic functionality to 
// add children to a node, calculate UCB and print information about 
// a node or the subtree starting at the node.
public class TreeNode {

    // TODO: publicPrintInfo()
    // * get info about node if neccessary


    //  TODO: addChild()
    //  * if child not in node's children
    //  * add newnode(board state, ...) to child list

    // not entire sure about the reason for this
    //  TODO: getWinProbability()
    //  ? win probability calc self.wins (wins through this node) / times visited
    

    // TODO: getExpectedValue()
    // * (snake wins - opponent wins) / visited
    
    // TODO: explored term for UCB 
    // def get_explore_term(self, parent, c=1):
    //     if self.parent is not None:
    //         return c * (2* math.log(parent.count) / self.count) ** (1 / 2)
    //     else:
    //         return 0 

    // TODO: calcUCB
    // * perform the UCB calculation 


    
}
