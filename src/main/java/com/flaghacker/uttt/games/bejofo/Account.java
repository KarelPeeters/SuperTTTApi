package com.flaghacker.uttt.games.bejofo;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;

import java.math.BigInteger;
import java.util.Random;

public class Account
{
	private static final String URL = "http://bejofo.net/ttt/";
	private static final String URL_NEW_GAME = URL + "newgame";
	private static final String URL_SET_NAME = URL + "setname";

	private HttpClient client = HttpClients.createDefault();

	private String name;
	private String password;
	private String passwordHash;

	public Account(String name, String password)
	{
		if (name == null)
			throw new IllegalArgumentException("name can't be null");

		this.name = name;
		this.password = password;

		if (password == null)
			passwordHash = new BigInteger(128, new Random()).toString(16);
		else
			passwordHash = DigestUtils.md5Hex(password);
	}

	public Account(String name)
	{
		this(name, null);
	}

	public String getName()
	{
		return name;
	}

	public boolean hasPassword()
	{
		return password != null;
	}

	public String getPasswordHash()
	{
		return passwordHash;
	}

	public String getPassword()
	{
		return password;
	}
}
