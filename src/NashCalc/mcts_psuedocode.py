

class mcts_node:
    def __init__(self):
        utility
        possible_utility
        parent = None
        children = {
            "ff": mcts_node1
            ...
            "ll": macts_node9
        }

    def get_ucbs(self):
        """
        1. Convert the children of this node into a payoff matrix where each reward is utility/possible utility
        2. Call the nash equalibrium module written by kyle
        3. Calculate the expected reward for each of the snakes 3 possible actions using the equilibrium mixed strategy to calculate the expected reward for each action. Should be 3 expected values for snake 1 and 3 expected values for snake 2. 
        4. Calculate the confidence margin using the number of times snake 1 has taken each of its 3 actions, and number of times snake 2 has taken each of its three actions.
        5. Return action that should be taken by snake 1, action that should be taken by snake 2, and the next mcts node corresponding to those actions
        """
        return snake1action, snake2action, nextnode



    def compute_sname1_expected_val():
        


class mcts_coordinator:
    def __init__(self):
        rootnode = mcts_node()
        curnode = rootnode

    def select_ucb_actions(self, legal_moves1, legal_moves2):
        snake1action, snake2action, nextnode = curnode.get_ucbs()
        curnode = nextnode
        return snake1action, snake2action

    def backpropogate(reward):
        curnode.possible_utility += 1
        curnode.utility += reward
        if curnode.parent is none:
            return
        curnode = curnode.parent
        self.backpropogate(reward)


def rollout(start_state, mcts_coordinator):
    while True:
        snake1move, snake2move = coordinator.select_ucb_actins(legal_moves_1, legal_moves_2)
        # Simulate one game step, dont worry about apple 
        if game_finished:
            return reward



def make_mcts_move(start_state):
    coordinator = mcts_coordinator()
    while(havetime):
        rewared = rollout(start_state, mcts_coordinator)
        mcts_coordinator.backpropogate(reward)
    return mcts_coordinator.rootnode.highest_expected_action()



        
        
        
    
