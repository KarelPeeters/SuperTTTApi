package com.flaghacker.uttt.games.bejofo;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.Scanner;
import java.util.function.Supplier;

public class BejofoBrowser
{
	private Supplier<WebDriver> driverSupplier;
	private final Account account;

	public BejofoBrowser(Supplier<WebDriver> driverSupplier, Account account)
	{
		this.driverSupplier = driverSupplier;
		this.account = account;
	}

	public BejofoBrowserGame startNewGame()
	{
		WebDriver driver = driverSupplier.get();
		driver.get("http://bejofo.net/ttt/newgame");

		String id = driver.findElement(By.name("gameid")).getAttribute("value");
		login(driver);

		return new BejofoBrowserGame(driver, id, account.getName());
	}

	public BejofoBrowserGame playOpened()
	{
		WebDriver driver = driverSupplier.get();
		driver.get("http://bejofo.net/ttt/");

		System.out.println("press enter when done");
		new Scanner(System.in).nextLine();
		System.out.println("done");

		String id = driver.findElement(By.name("gameid")).getAttribute("value");
		return new BejofoBrowserGame(driver, id, account.getName());
	}

	public BejofoBrowserGame joinGame(String id)
	{
		throw new RuntimeException("not implemented yer");

		/*WebDriver driver = driverSupplier.get();
		driver.get("http://bejofo.net/ttt/" + id);
		driver.manage().deleteAllCookies();

		login(driver);

		return new BejofoBrowserGame(driver, id, account.getName());*/
	}

	private void login(WebDriver driver)
	{
		WebElement nameField = driver.findElement(By.name("name"));
		new Actions(driver).moveToElement(nameField).sendKeys("test");


		nameField.sendKeys(account.getName());

		if (account.hasPassword())
		{
			WebElement pwLink = driver.findElement(By.id("pwhint"));
			pwLink.click();
			WebElement pwField = driver.findElement(By.id("pwfield"));
			pwField.sendKeys(account.getPassword());
		}

		WebElement goBtn = driver.findElement(By.className("button"));
		goBtn.sendKeys("\n");
	}
}
