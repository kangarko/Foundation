package org.mineacademy.fo.remain.nbt;

import java.util.UUID;

final class UUIDUtil {

	public static UUID uuidFromIntArray(int[] is) {
		return new UUID((long) is[0] << 32 | is[1] & 4294967295L,
				(long) is[2] << 32 | is[3] & 4294967295L);
	}

	public static int[] uuidToIntArray(UUID uUID) {
		final long l = uUID.getMostSignificantBits();
		final long m = uUID.getLeastSignificantBits();
		return leastMostToIntArray(l, m);
	}

	private static int[] leastMostToIntArray(long l, long m) {
		return new int[] { (int) (l >> 32), (int) l, (int) (m >> 32), (int) m };
	}

}
