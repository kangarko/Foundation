package org.mineacademy.fo.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.SerializeUtilCore;
import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.platform.FoundationPlayer;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a book handler that can show player books
 */
@Getter
public final class Book implements ConfigSerializable {

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
	private Book(String title, String author, List<String> pages, boolean signed, long lastModified, String fileName, UUID uniqueId) {
		this.title = title;
		this.author = author;
		this.pages = this.convertPages(pages);
		this.signed = signed;
		this.lastModified = lastModified;
		this.fileName = fileName;
		this.uniqueId = uniqueId;
	}

	/*
	 * Remove symbols crashing SQL from book pages
	 */
	private List<String> convertPages(List<String> pages) {
		final List<String> copyOf = new ArrayList<>();

		for (int i = 0; i < pages.size(); i++) {
			String page = pages.get(i);

			// Prob nobody will figure this out anyways...
			page = page.replace("{", "-LZZATVORKA-");
			page = page.replace("}", "-PZZATVORKA-");
			page = page.replace("\"", "-UVODZOVKA-");
			page = page.replace("\'", "-JUVODZOVKA-");
			page = page.replace("\\", "-ZLOMITKO-");

			copyOf.add(page);
		}

		return copyOf;
	}

	/**
	 * Opens the book for the player, rendering pages in chat for MC 1.7.10 and older
	 *
	 * @param audience
	 */
	public void open(Player audience) {
		this.open(Platform.toPlayer(audience));
	}

	/**
	 * Opens the book for the player, rendering pages in chat for MC 1.7.10 and older
	 *
	 * @param audience
	 */
	public void open(FoundationPlayer audience) {

		// Render as text, replacing variables
		if (MinecraftVersion.olderThan(V.v1_8) || !audience.isPlayer()) {
			final List<SimpleComponent> pages = new ArrayList<>();
			int pageNumber = 1;
			final Variables variables = Variables.builder(audience);

			for (final String page : this.pages) {
				pages.add(Lang.componentVars("command-book-page", "page", pageNumber++));

				for (final String line : page.split("\n"))
					pages.add(SimpleComponent.fromMini(" &7- &r" + variables.replace(this.replaceVariablesBack(line))));

				pages.add(SimpleComponent.empty());
			}

			new ChatPaginator()
					.setFoundationHeader(Lang.legacyVars("command-book-page-header",
							"title", CommonCore.getOrDefault(this.title, Lang.legacy("command-book-unnamed")),
							"author", CommonCore.getOrDefault(this.author, Lang.legacy("command-book-unsigned"))))
					.setPages(pages)
					.send(audience);

			return;
		}

		// Open a clone book for player so we can replace variable in it
		// calls player.openBook(book); on MC 1.16
		Remain.openBook(audience, this.toWrittenBook(audience));
	}

	/**
	 * Save the book to file, return true if the old file was overriden
	 *
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public boolean save(String fileName) throws IOException {
		final File target = FileUtil.getFile("books/" + fileName + ".yml");
		final boolean exists = target.exists();

		if (!exists)
			FileUtil.createIfNotExists("books/" + fileName + ".yml");

		final YamlConfig config = YamlConfig.fromFile(target);

		// Update file name
		this.fileName = fileName;

		config.set("Data", SerializeUtilCore.serialize(Language.YAML, this.serialize()));
		config.save();

		// If it exists, we return true since we had to override it
		return exists;
	}

	/**
	 * Return this as editable book
	 *
	 * @param title
	 * @param lore
	 * @return
	 */
	public ItemStack toEditableBook(String title, String... lore) {
		return ItemCreator
				.fromMaterial(CompMaterial.WRITABLE_BOOK)
				.bookTitle(this.title)
				.bookAuthor(this.author)
				.bookPages(this.pages)
				.name(title)
				.tag(TAG, "true")
				.lore(lore)
				.hideTags(true)
				.make();
	}

	/**
	 * Convert this book into an itemstack with variables replaced
	 *
	 * @param audience
	 * @return
	 */
	public ItemStack toWrittenBook(Player audience) {
		return this.toWrittenBook(Platform.toPlayer(audience));
	}

	/**
	 * Convert this book into an itemstack with variables replaced
	 *
	 * @param audience
	 * @return
	 */
	public ItemStack toWrittenBook(FoundationPlayer audience) {
		final ItemStack clone = new ItemStack(CompMaterial.WRITTEN_BOOK.getMaterial());
		final BookMeta bookMeta = (BookMeta) clone.getItemMeta();
		final Variables variables = Variables.builder(audience);

		// Replace our variables
		final List<SimpleComponent> pagesClone = new ArrayList<>();

		for (final String page : this.pages)
			pagesClone.add(variables.replace(SimpleComponent.fromMini(this.replaceVariablesBack(page))));

		Remain.setPages(bookMeta, pagesClone);
		bookMeta.setTitle(this.title == null ? "Blank" : this.title);
		bookMeta.setAuthor(this.author == null ? "Blank" : this.author);

		clone.setItemMeta(bookMeta);

		return clone;
	}

	/*
	 * Replace our variables back
	 */
	private String replaceVariablesBack(String page) {
		page = page.replace("-LZZATVORKA-", "{");
		page = page.replace("-PZZATVORKA-", "}");
		page = page.replace("-UVODZOVKA-", "\"");
		page = page.replace("-JUVODZOVKA-", "\'");
		page = page.replace("-ZLOMITKO-", "\\");

		return page;
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
		return "Book " + this.serialize().toStringFormatted();
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
	public static Book deserialize(SerializedMap map) {
		ValidCore.checkBoolean(!map.isEmpty(), "Cannot deserialize empty map to book!");

		final String title = map.getString("Title");
		final String author = map.getString("Author");
		final List<String> pages = map.getStringList("Pages");
		final boolean signed = map.getBoolean("Signed", false);
		final long lastModified = map.getLong("Last_Modified", 0L);
		final UUID uniqueId = map.get("Unique_Id", UUID.class);

		return new Book(title, author, pages, signed, lastModified, null, uniqueId);
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
		final List<String> pages = this.convertPages(this.pages != null && this.pages.size() > 60 ? this.pages.subList(0, 60) : this.pages);

		map.put("Pages", pages);
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
	public static Book newEmptyBook() {
		return new Book(null, null, CommonCore.toList(""), false, System.currentTimeMillis(), null, UUID.randomUUID());
	}

	/**
	 * Return a clone of the given book with author changed
	 *
	 * @param book
	 * @param newAuthor
	 * @return
	 */
	public static Book clone(Book book, String newAuthor) {
		return new Book(book.getTitle(), newAuthor, book.getPages(), book.isSigned(), book.getLastModified(), book.getFileName(), book.getUniqueId());
	}

	/**
	 * Make a {@link Book} from the given book edit event
	 *
	 * @param event
	 * @return
	 */
	public static Book fromEvent(PlayerEditBookEvent event) {
		final BookMeta meta = event.getNewBookMeta();

		final String title = meta.getTitle();
		final String author = meta.getAuthor();
		final List<String> pages = meta.getPages();
		final boolean signed = event.isSigning();
		final long lastModified = System.currentTimeMillis();

		return new Book(title, author, pages, signed, lastModified, null, UUID.randomUUID());
	}

	/**
	 * Return a book from the book name given it is in books/ folder.
	 *
	 * @param fileName
	 * @return
	 */
	public static Book fromFile(String fileName) {
		final File file = FileUtil.getFile("books/" + fileName + (fileName.endsWith(".yml") ? "" : ".yml"));

		if (!file.exists())
			throw new IllegalArgumentException("No such book: '" + fileName + "'. Available: " + CommonCore.join(Book.getBookNames()));

		final YamlConfig config = YamlConfig.fromFile(file);

		if (!config.isSet("Data"))
			throw new IllegalArgumentException("Book '" + fileName + "' has corrupted data.");

		final Book book = deserialize(config.getMap("Data"));

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
	public static List<Book> getBooks() {
		return CommonCore.convertArrayToList(FileUtil.getFiles("books", ".yml"), file -> Book.fromFile(file.getName()));
	}
}
