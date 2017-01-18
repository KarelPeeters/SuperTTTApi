package com.flaghacker.uttt.games.bejofo;

import com.flaghacker.uttt.common.Board;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class BejofoBrowserGame
{
	private final WebDriver driver;
	private final String id;
	private final int player;

	private static final By children = By.xpath(".//*");

	public BejofoBrowserGame(WebDriver driver, String id, String playerName)
	{
		this.driver = driver;
		this.id = id;
		this.player = determinePlayer(playerName);

		updateBoard();
	}

	private int determinePlayer(String name)
	{
		String p1Name = driver.findElement(By.id("p1name")).getText();
		String p2Name = driver.findElement(By.id("p2name")).getText();
		
		if (name.equals(p1Name))
			return 1;
		if (name.equals(p2Name))
			return 2;

		throw new IllegalArgumentException(
				String.format("%s not found in %s, only %s and %s", name, id, p1Name, p2Name)
		);
	}

	private boolean ourTurn()
	{
		String whoseturn = driver.findElement(By.id("whoseturn")).getText();
		return !whoseturn.toLowerCase().contains("you");
	}

	private Board updateBoard()
	{
		byte[][] tiles = new byte[9][9];
		byte[] macro = new byte[9];
		boolean[] nextMacros = new boolean[9];

		int mo = 0;
		for (WebElement smallField : driver.findElements(By.className("small_field field")))
		{
			mo++;
			int so = 0;
			for (WebElement tile : smallField.findElements(children))
			{
				if (tile.getTagName().equals("br"))
					continue;

				so++;
				//TODO finish implementing

			}
		}

		return new Board(tiles, macro, nextMacros);
	}

	public String getId()
	{
		return id;
	}
}
