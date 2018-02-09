package bot;

import com.flaghacker.sttt.bots.KotlinRandomBot;
import com.flaghacker.sttt.bots.mcts.MCTSBot;
import com.flaghacker.sttt.bots.mcts.Settings;
import com.flaghacker.sttt.common.KotlinBotGame;

public class BotStarter
{
	public static void main(String[] args)
	{
		new KotlinBotGame(new MCTSBot(Settings.standard()),new KotlinRandomBot())
				.setCount(100)
				.setShuffling(true)
				.setTimePerMove(56)
				.setLogLevel(KotlinBotGame.LogLevel.BASIC)
				.run();
	}
}
