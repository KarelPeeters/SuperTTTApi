package com.flaghacker.uttt.bots.mcts;

import com.flaghacker.uttt.common.Board;
import com.flaghacker.uttt.common.Coord;
import com.flaghacker.uttt.common.Player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.log;
import static java.lang.Math.sqrt;

public class BoardInfo implements Serializable
{
	private static final long serialVersionUID = 6792457138219073695L;

	private final Settings settings;

	private int totalPlayed = 0;
	private Info root;
	private Map<Board, Info> map = new HashMap<>();
	private Set<Info> moveSet = new HashSet<>();

	public BoardInfo(Settings settings)
	{
		this.settings = settings;
	}

	public void incTotal()
	{
		totalPlayed++;
	}

	public Info inc(Board board, int depth, Player wonBy, Info previous)
	{
		if (!map.containsKey(board))
			map.put(board, new Info(board, depth, previous));

		Info info = map.get(board);
		addToSets(info);
		info.inc(wonBy);

		if (root == null)
			root = info;

		return info;
	}

	public Info selectBest()
	{
		Info curr = root;
		Info next = root.highest();

		while (next != curr)
		{
			curr = next;
			next = curr.highest();
		}

		return curr;
	}

	public Coord selectBestMove()
	{
		Info bestInfo = settings.tryLose()
				? Collections.min(moveSet)
				: Collections.max(moveSet);

		return bestInfo.board.getLastMove();
	}

	public Info getInfo(Board board)
	{
		return map.containsKey(board) ? map.get(board) : null;

	}

	public void incAllPrevious(Info info, Player wonBy)
	{
		while (info.previous != null)
		{
			info = info.previous;
			info.inc(wonBy);
		}
	}

	private void addToSets(Info info)
	{
		if (info.previous != null)
			info.previous.addChild(info);

		if (info.depth == 1)
			moveSet.add(info);
	}

	public class Info implements Comparable<Info>
	{
		public Board board;
		public Info previous;
		public int won = 0;
		public int played = 0;
		public int depth;
		public List<Info> children = new ArrayList<>(0);

		public Info(Board board, int depth, Info previous)
		{
			this.board = board;
			this.depth = depth;
		}

		@Override
		public int compareTo(Info other)
		{
			return Double.compare(this.value(), other.value());
		}

		public double value()
		{
			return ((double) won / played) + settings.branchWeight() * sqrt(log(totalPlayed) / played);
		}

		public void inc(Player wonBy)
		{
			played++;
			if (board.nextPlayer().other() == wonBy)
				won++;
		}

		public void addChild(Info info)
		{
			children.add(info);
		}

		public Info highest()
		{
			if (children.size() == 0)
				return this;

			Info max = Collections.max(children);
			return max.value() > value() ? max : this;
		}
	}

	public int getTotalPlayed()
	{
		return totalPlayed;
	}

	public int getMoveSize()
	{
		return moveSet.size();
	}
}
