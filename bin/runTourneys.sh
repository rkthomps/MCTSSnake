#!/bin/bash
bin/play-game.sh mctsSnake.NaiveBot finalBot.AdderBoaCobra > naive_vs_adder.txt;
bin/play-game.sh mctsSnake.NaiveBot mctsSnake.PUCTBot > naive_vs_puct.txt;
bin/play-game.sh mctsSnake.NaiveBot mctsSnake.TheBot > naive_vs_uct.txt;
bin/play-game.sh mctsSnake.NaiveBot mctsSnake.RolloutBot > naive_vs_rollout.txt;

