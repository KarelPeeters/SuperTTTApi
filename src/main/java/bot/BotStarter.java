// // Copyright 2016 theaigames.com (developers@theaigames.com)

//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at

//        http://www.apache.org/licenses/LICENSE-2.0

//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//    For the full copyright and license information, please view the LICENSE
//    file that was distributed with this source code.

package bot;

import com.flaghacker.sttt.bots.KotlinMMBot;
import com.flaghacker.sttt.bots.KotlinRandomBot;
import com.flaghacker.sttt.bots.mcts.MCTSBot;
import com.flaghacker.sttt.bots.mcts.Settings;
import com.flaghacker.sttt.common.KotlinBotGame;

public class BotStarter
{
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_RED = "\u001B[31m";

	public static void main(String[] args)
	{
		//com.flaghacker.sttt.common.KotlinBoard b = new com.flaghacker.sttt.common.KotlinBoard();
		//long b1 = System.currentTimeMillis();
		//new KotlinBotGame(new KotlinMMBot(5),new KotlinRandomBot()).setCount(100).run();
		//long e1 = System.currentTimeMillis();

		//new BotGame(new MCTSBot(Settings.standard()),new MMBot(5)).setCount(100).setShuffling(true).setTimePerMove(56).run();
		new KotlinBotGame(new MCTSBot(Settings.standard()),new KotlinRandomBot())
				.setCount(10)
				.setShuffling(true)
				.setTimePerMove(56)
				.setLogLevel(KotlinBotGame.LogLevel.BASIC)
				.run();

		//System.out.println("kotlin:" + ((e1-b1)/100) + "ms java:" + ((e2-b2)/100) + "ms delta:" + (((e1-b1)/100)-((e2-b2)/100)));


		/*AIGame game = new AIGame(new MCTSBot(
						Settings.builder()
								.branchWeight(6)
								.log(true)
								.build()
				), System.in
		);
		game.run();¨*/
	}
}
