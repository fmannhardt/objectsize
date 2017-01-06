package de.fmannhardt.objectsize;

import static org.junit.Assert.*;

import org.junit.Test;

public class ObjectSizeTest {
	
	@Test
	public void testObjectSize() {
		assertEquals(16l, ObjectSize.sizeOf(this));
		assertEquals(24l, ObjectSize.sizeOf(new Long(1l)));
		assertEquals(0l, ObjectSize.sizeOf(null));
	}

}