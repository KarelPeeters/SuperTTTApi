import com.flaghacker.sttt.bots.RandomBot;
import com.flaghacker.sttt.bots.mcts.MCTSBot;
import com.flaghacker.sttt.bots.mcts.Settings;
import com.flaghacker.sttt.games.BotGame;

public class Main
{
	public static void main(String[] args)
	{
		new BotGame(new MCTSBot(Settings.standard()),new RandomBot())
				.setCount(100)
				.setShuffling(true)
				.setTimePerMove(56)
				.setLogLevel(BotGame.LogLevel.BASIC)
				.run();
	}
}
