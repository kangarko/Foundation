package org.mineacademy.fo.proxy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mineacademy.fo.collection.SerializedMap;

/**
 * Splits a {@link SerializedMap} so each chunk's JSON serialization fits
 * inside Bukkit's plugin messaging channel size limit.
 *
 * Bukkit caps a single plugin message at {@value #BUKKIT_PLUGIN_MESSAGE_LIMIT}
 * bytes. Sync payloads that aggregate every online player (notably
 * SYNCED_CACHE_HEADER and SYNCED_CACHE_BY_UUID) cross that ceiling around 500
 * concurrent players, which raises {@code MessageTooLargeException} and kicks
 * everyone in a loop. Chunking at the application layer keeps each emitted
 * message comfortably below the protocol limit on networks of any size.
 */
public final class ProxyChunker {

	/**
	 * Bukkit's hard limit for a single plugin message, enforced by
	 * {@code StandardMessenger#validatePluginMessage}. Same on every server
	 * version since the API was introduced.
	 */
	private static final int BUKKIT_PLUGIN_MESSAGE_LIMIT = 32_766;

	/**
	 * Reserved for the message envelope written around the chunked map:
	 * channel name, sender UUID string, server name, action name, the four
	 * {@code DataOutputStream} length prefixes, and any trailing fields
	 * (e.g. SyncType name, isFinal flag). Realised overhead is ~100 bytes;
	 * the surplus absorbs future envelope additions without retuning.
	 */
	private static final int ENVELOPE_RESERVE_BYTES = 4_766;

	/**
	 * Maximum JSON byte size of a single chunk's payload.
	 */
	public static final int SAFE_CHUNK_BYTES = BUKKIT_PLUGIN_MESSAGE_LIMIT - ENVELOPE_RESERVE_BYTES;

	private ProxyChunker() {
	}

	/**
	 * Split the given map into one or more sub-maps such that every chunk's
	 * JSON serialization is at most {@link #SAFE_CHUNK_BYTES} bytes.
	 *
	 * Always returns at least one chunk, even for an empty input, so the
	 * caller can rely on a final-chunk signal being delivered every cycle.
	 *
	 * An entry that on its own exceeds the budget is emitted as a chunk of
	 * size one. Foundation's per-message size guard will then drop that
	 * outgoing packet, but the rest of the cycle remains deliverable.
	 *
	 * @param input the map to split
	 * @return ordered list of chunks, never empty
	 */
	public static List<SerializedMap> chunk(final SerializedMap input) {
		final List<SerializedMap> chunks = new ArrayList<>();

		if (input.isEmpty()) {
			chunks.add(new SerializedMap());

			return chunks;
		}

		SerializedMap current = new SerializedMap();

		for (final Map.Entry<String, Object> entry : input.entrySet()) {
			current.put(entry.getKey(), entry.getValue());

			if (current.size() > 1 && jsonByteSize(current) > SAFE_CHUNK_BYTES) {
				current.remove(entry.getKey());
				chunks.add(current);

				current = new SerializedMap();
				current.put(entry.getKey(), entry.getValue());
			}
		}

		chunks.add(current);

		return chunks;
	}

	private static int jsonByteSize(final SerializedMap map) {
		return map.toJson().getBytes(StandardCharsets.UTF_8).length;
	}
}
