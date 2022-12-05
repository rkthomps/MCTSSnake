# Monte Carlo Tree Search for Snakes

## Authors and Project Info
- Authors: Kyle Thompson, Chase VanderZwan, Michael Moschitto, Deon Lillo, Cagan Sevencan, Adley Wong, Saurav Gupta
- This project is for the CSC-580 final project at Cal Poly San Luis Obispo taught by Professor Rodrigo Canaan. 

## Overview
We implement Monte Carlo Tree Search (MCTS) for the game Snakes. We focus on Snakes as it is described in the [2020 IEEE CoG Snakes Competition](https://sites.google.com/view/ai-snakes-game). To this end, this repository is a clone of the [Competition Implementation](https://www.google.com/url?q=https%3A%2F%2Fgithub.com%2FBeLuckyDaf%2Fsnakes-game-tutorial&sa=D&sntz=1&usg=AOvVaw2OiUQTt4ozAhKfQCXHweN7).

## Compiling and Running the Code
1. In the repository's root directory, run `make` to compile the source code.
2. Run `bin/play-game.sh <bot1> <bot2>` to simulate a tournament between bot1 and bot2. Note that you must use the bot's fully qualified name i.e. <packagename>.<classname>.
3. Run `bin/runMCTSTests.sh` to run a very simple version of Snakes and view the MCTS resulting MCTS search tree.
4. Run `bin/runNashTests.sh` to run JUnit tests on the Nash Equilibrium calculation. This script also runs some performance tests on the calculation.
5. Run `bin/runTheBotTests.sh` to inspect the MCTS snake's search tree on a real-game scenario.

## Description of the Source Code
### NashCalc Package
- `NashCalculator`: This module calculates the nash equilibrium given the payoff matrix of a two-player zero-sum game.
- `Equilibrium`: This module offers a convenient data structure to store the  mixed-strategy policies of a Nash Equilibrium.
- `LabelledPayoff`: This module offers a way to index payoff matricies indirectly. This is necessary because we can prune the dominated pure-strategies in the payoff matrices to speed-up computing the Nash Equilibrium.
- `Util`: This module offers a set of utility functions (mostly matrix operations) that are used by various other modules in the source code.
- `ArgSorter`: This module contains an implementation of Merge Sort that returns an array of the indices of the sorted elements instead of the sorted elements themselves. This module is used in NashCalculator to prune dominated strategies.


### mctsSnake Package
- `TheBot`: This module contains an implementation of the competitions `Bot` interface which is required to compete against other bots.
- `Controller`: This module implements an intermediate entity that allows our agent to query our MCTS search tree. Otherwise we would have to store game state information in each node which would likely slow down the rollouts.
- `Node`: This module implements a single node in the MCTS search tree along with its functionality.
- The `TheBot`, `Controller`, and `Node` combine to define the implementation of our UCT agent (UCT explore term and Informed Selection).
- Similarly, `PUCTBot`, `PUCTController`, and `PUCTNode` combine to define the implementation of our PUCT agent (PUCT explore term and Informed Selection). 
- Finally, `NaiveBot`, `NaiveController`, and `NaiveNode` combine to define the implementation of our NaiveBot agent (PUCT explore term and Naive Selection).


### humanBot Package
- `humanBot`: Implementation of a bot that human players can use with the arrow keys in order to play against an AI snake agent. If no move is detected from the user, then the snake will continue moving forward.


## Demos
### Rollout-Policy-Guided Snake
![](demos/rollout-policy-demo.gif)
  
*Notes*: By "rollout-policy" we mean that this snake is following the exact policy used to do MCTS rollouts. It proceeds to the square with
  the smallest Manhatten distance to the apple. It does not go to squares where it will die with certainty unless there are no other options.

### MCTS Snake
![](demos/mcts-demo.gif)
  

