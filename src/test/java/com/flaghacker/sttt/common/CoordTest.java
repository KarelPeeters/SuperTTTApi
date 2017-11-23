package com.flaghacker.sttt.common;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class CoordTest
{
	@Test
	public void testSerializable()
	{
		Random rand = new Random(0);

		for (int i = 0; i < 10; i++)
		{
			Coord coord = Coord.coord(rand.nextInt(81));
			assertEquals(coord, SerializationUtils.clone(coord));
		}
	}
}
