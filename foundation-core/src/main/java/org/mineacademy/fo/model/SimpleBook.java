package org.mineacademy.fo.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.SerializeUtilCore;
import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;

/**
 * Represents a book. The difference between this and Adventure book
 * is support for file saving, signing and unique id.
 */
@Getter
public final class SimpleBook implements ConfigSerializable {

	/**
	 * The editable book nbt tag
	 */
	public final static String TAG = "FoEditableBook";

	/**
	 * The book title
	 */
	private final String title;

	/**
	 * The book author
	 */
	@Setter
	private String author;

	/**
	 * The book pages
	 */
	private final List<String> pages;

	/**
	 * Is d' book signed?
	 */
	@Setter
	private boolean signed;

	/**
	 * The time d' book d' modified'
	 */
	private final long lastModified;

	/**
	 * The file name d' book or null if 'd Buch not exists
	 */
	private String fileName;

	/**
	 * The identification of the "lastest" book version.. LASTEST...
	 */
	private final UUID uniqueId;

	/*
	 * Create a new empty book
	 */
	public SimpleBook(final String title, final String author, final List<String> pages, final boolean signed, final long lastModified, final String fileName, final UUID uniqueId) {
		this.title = title;
		this.author = author;
		this.pages = pages;
		this.signed = signed;
		this.lastModified = lastModified;
		this.fileName = fileName;
		this.uniqueId = uniqueId;
	}

	/**
	 * Opens the book for the player
	 * MiniMessage tags, legacy colors, and placeholders are translated.
	 *
	 * @param audience
	 */
	public void openColorized(final FoundationPlayer audience) {
		this.open(audience, true);
	}

	/**
	 * Opens the book for the player
	 *
	 * @param audience
	 */
	public void openPlain(final FoundationPlayer audience) {
		this.open(audience, false);
	}

	/**
	 * Opens the book for the player
	 */
	private void open(final FoundationPlayer audience, boolean translateColors) {
		final String safeTitle = CommonCore.getOrEmpty(this.title);
		final String safeAuthor = CommonCore.getOrEmpty(this.author);

		final Component title = (translateColors ? SimpleComponent.fromMiniSection(safeTitle) : SimpleComponent.fromPlain(safeTitle)).toAdventure(audience);
		final Component author = (translateColors ? SimpleComponent.fromMiniSection(safeAuthor) : SimpleComponent.fromPlain(safeAuthor)).toAdventure(audience);
		final List<Component> pages = new ArrayList<>();

		final Variables variables = Variables.builder(audience);

		if (this.pages != null)
			for (final String page : this.pages)
				pages.add(translateColors ? variables.replaceComponent(SimpleComponent.fromMiniAmpersand(page)).toAdventure(audience) : Component.text(page.replace(CompChatColor.COLOR_CHAR + "", "&")));

		audience.openBook(Book.book(title, author, pages));
	}

	/**
	 * Save the book to file
	 *
	 * @param fileName
	 * @throws IOException
	 */
	public void save(final String fileName) throws IOException {
		final File target = FileUtil.getFile("books/" + fileName + ".yml");
		final boolean exists = target.exists();

		if (!exists)
			FileUtil.createIfNotExists("books/" + fileName + ".yml");

		final YamlConfig config = YamlConfig.fromFile(target);

		// Update file name
		this.fileName = fileName;

		config.set("Data", SerializeUtilCore.serialize(Language.YAML, this.serialize()));
		config.save();
	}

	/**
	 * Return true if das Buch is on disk
	 *
	 * @return
	 */
	public boolean isSaved() {
		return this.fileName != null;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Book " + this.serialize();
	}

	/* ------------------------------------------------------------------------------- */
	/* Serializing */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Converts the config map to a book.
	 *
	 * @param map
	 * @return
	 */
	public static SimpleBook deserialize(final SerializedMap map) {
		ValidCore.checkBoolean(!map.isEmpty(), "Cannot deserialize empty map to book!");

		final String title = map.getString("Title");
		final String author = map.getString("Author");
		final List<String> pages = map.getStringList("Pages");
		final boolean signed = map.getBoolean("Signed", false);
		final long lastModified = map.getLong("Last_Modified", 0L);
		final UUID uniqueId = map.get("Unique_Id", UUID.class);

		return new SimpleBook(title, author, pages, signed, lastModified, null, uniqueId);
	}

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.putIfExists("Title", this.title);
		map.putIfExists("Author", this.author);

		// Trim to avoid packet overflow
		map.put("Pages", this.pages != null && this.pages.size() > 60 ? this.pages.subList(0, 60) : this.pages);
		map.put("Signed", this.signed);
		map.put("Last_Modified", this.lastModified);
		map.put("Unique_Id", this.uniqueId);

		return map;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new empty book
	 *
	 * @return
	 */
	public static SimpleBook newEmptyBook() {
		return new SimpleBook(null, null, CommonCore.toList(""), false, System.currentTimeMillis(), null, UUID.randomUUID());
	}

	/**
	 * Return a clone of the given book with author changed
	 *
	 * @param book
	 * @param newAuthor
	 * @return
	 */
	public static SimpleBook clone(final SimpleBook book, final String newAuthor) {
		return new SimpleBook(book.getTitle(), newAuthor, book.getPages(), book.isSigned(), book.getLastModified(), book.getFileName(), book.getUniqueId());
	}

	/**
	 * Converts an Adventure book to our book.
	 *
	 * @param book
	 * @return
	 */
	public static SimpleBook fromAdventure(final net.kyori.adventure.inventory.Book book) {
		final String title = SimpleComponent.fromAdventure(book.title()).toLegacySection(null);
		final String author = SimpleComponent.fromAdventure(book.author()).toLegacySection(null);
		final List<String> pages = new ArrayList<>();

		for (final Component page : book.pages())
			pages.add(SimpleComponent.fromAdventure(page).toLegacySection(null));

		return new SimpleBook(title, author, pages, false, System.currentTimeMillis(), null, UUID.randomUUID());
	}

	/**
	 * Return a book from the book name given it is in books/ folder.
	 *
	 * @param fileName
	 * @return
	 */
	public static SimpleBook fromFile(final String fileName) {
		final File file = FileUtil.getFile("books/" + fileName + (fileName.endsWith(".yml") ? "" : ".yml"));

		if (!file.exists())
			throw new IllegalArgumentException("No such book: '" + fileName + "'. Available: " + CommonCore.join(SimpleBook.getBookNames()));

		final YamlConfig config = YamlConfig.fromFile(file);

		if (!config.isSet("Data"))
			throw new IllegalArgumentException("Book '" + fileName + "' has corrupted data.");

		final SimpleBook book = deserialize(config.getMap("Data"));

		book.fileName = fileName;

		return book;
	}

	/**
	 * Copies the default books/ folder if it does not exist already
	 */
	public static void copyDefaults() {
		final File booksFolder = FileUtil.getFile("books");

		if (!booksFolder.exists())
			FileUtil.extractFolderFromJar("books/", "books");
	}

	/**
	 * Return all book names in books/ folder
	 *
	 * @return
	 */
	public static List<String> getBookNames() {
		return CommonCore.convertArrayToList(FileUtil.getFiles("books", ".yml"), FileUtil::getFileName);
	}

	/**
	 * Return all books in books/ folder
	 *
	 * @return
	 */
	public static List<SimpleBook> getBooks() {
		return CommonCore.convertArrayToList(FileUtil.getFiles("books", ".yml"), file -> SimpleBook.fromFile(file.getName()));
	}
}
