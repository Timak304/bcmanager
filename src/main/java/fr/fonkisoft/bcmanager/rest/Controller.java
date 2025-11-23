package fr.fonkisoft.bcmanager.rest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.stmt.QueryBuilder;

import fr.fonkisoft.bcmanager.domain.Album;
import fr.fonkisoft.bcmanager.domain.Album.AlbumCategory;
import fr.fonkisoft.bcmanager.domain.AlbumTag;
import fr.fonkisoft.bcmanager.domain.Tag;
import fr.fonkisoft.bcmanager.service.DaoManager;
import fr.fonkisoft.bcmanager.service.Importer;
import fr.fonkisoft.bcmanager.service.TagService;
import io.javalin.http.Context;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Route implementations
 */
@Slf4j
public class Controller {

	private Dao<Album, Long> albumDao;
	private Dao<Tag, Long> tagDao;
	private Dao<AlbumTag, Long> albumTagDao;
	
	/** Importer service */
	private Importer importer;
	
	private TagService tagService;
	
	@Data
	public class TagCount {
		final String name;
		final int count;
	}
	
	public Controller() throws SQLException {
		albumDao = DaoManager.getInstance().getAlbumDao();
		tagDao = DaoManager.getInstance().getTagDao();
		albumTagDao = DaoManager.getInstance().getAlbumTagDao();
		importer = Importer.getInstance();
		tagService = TagService.getInstance();
	}
	
	/**
	 * Implements route GET /api/tags
	 * Return the list of tags
	 * @param ctx javalin context<br>
	 * 	queryparam showWishlist: includes wishlist albums in results
	 * @return tag list
	 */
	public Context getTagsWithCount(Context ctx) throws SQLException {
		boolean showWishlist = Boolean.parseBoolean(ctx.queryParam("showWishlist"));
		QueryBuilder<Tag, Long> tagQb = tagDao.queryBuilder();
		QueryBuilder<AlbumTag, Long> albumTagQb = albumTagDao.queryBuilder();
		if (!showWishlist) {
			QueryBuilder<Album, Long> albumQb = albumDao.queryBuilder();
			albumQb.where().eq("category", AlbumCategory.COLLECTION);
			albumTagQb.join(albumQb);
			tagQb.having("count(album_id) > 0");
		}
		tagQb.join(albumTagQb);
		tagQb.groupBy("id");
		tagQb.selectRaw("TAG.name, count(album_id)");
		
		GenericRawResults<String[]> queryRaw = tagQb.queryRaw();
		List<TagCount> results = queryRaw.getResults().stream()
				.map(row -> new TagCount(row[0], Integer.parseInt(row[1])))
				.toList();
		return ctx.json(results);
	}
	
	public Context getTags(Context ctx) throws SQLException {
		return ctx.json(tagDao.queryForAll());
	}
	
	/**
	 * Implements route GET /api/albums
	 * Return the list of albums matches the query criteria
	 * @param ctx javalin context<br>
	 * 	queryparam:
	 * <ul>
	 * 	<li>tag: list of tag to filter on</li>
	 * 	<li>name: artist name to filter on</li>
	 * 	<li>loc: artist location to filter on</li>
	 * 	<li>track: track to filter on</li>
	 * 	<li>date: date to filter on (formated yyyy-MM-dd)</li>
	 * 	<li>sort: field to sort on</li>
	 * 	<li>dir: sort direction, true is ascending</li>
	 * 	<li>showWishlist: includes wishlist albums in results</li>
	 * </ul> 	
	 * @return tag list
	 */
	public Context getAlbums(Context ctx) throws SQLException {
		String tag = ctx.queryParam("tag");
		String name = ctx.queryParam("name");
		String location = ctx.queryParam("loc");
		String track = ctx.queryParam("track");
		String releaseDate = ctx.queryParam("date");
		String sort = ctx.queryParam("sort");
		boolean sortDirection = Boolean.parseBoolean(ctx.queryParam("dir"));
		boolean showWishlist = Boolean.parseBoolean(ctx.queryParam("showWishlist"));
		
		QueryBuilder<Album, Long> albumQb = albumDao.queryBuilder();
		albumQb.distinct();

		if (tag != null && !tag.isBlank()) {
			QueryBuilder<Tag, Long> tagQb = tagDao.queryBuilder();
			tagQb.where().in("name", tag.split("\\|"));
			QueryBuilder<AlbumTag, Long> albumTagQb = albumTagDao.queryBuilder();
			albumTagQb.join(tagQb);
			albumQb.join(albumTagQb);
		}
		
		if (name != null && !name.isBlank()) {
			albumQb.where().like("name", "%" + name.toLowerCase() + "%");
		}
		if (location != null && !location.isBlank()) {
			albumQb.where().like("location", "%" + location + "%");
		}
		if (track != null && !track.isBlank()) {
			albumQb.where().like("trackList", "%" + track + "%");
		}
		if (releaseDate != null && !releaseDate.isBlank()) {
			albumQb.where().like("releaseDate", "%" + releaseDate + "%");
		}
		if (!showWishlist) {
			albumQb.where().eq("category", AlbumCategory.COLLECTION);
		}
		
		if (sort != null && !sort.isBlank()) {
			albumQb.orderBy(sort, sortDirection);
		}
		
		List<Album> results = albumQb.query();
		
		results.stream().forEach(al -> {
				try {
					QueryBuilder<Tag, Long> lTagQb = tagDao.queryBuilder();
					QueryBuilder<AlbumTag, Long> lAlbumTagQb = albumTagDao.queryBuilder();
					lAlbumTagQb.where().eq("album_id", al.getId());
					lTagQb.join(lAlbumTagQb);
					al.setTagList(lTagQb.query().stream().map(Tag::getName).toList());
				} catch (SQLException e) {
					log.error("Error getting tags", e);
				}
			});
		return ctx.json(results);
	}
	
	/**
	 * Implements route POST /api/album
	 * Add the given album
	 * @param ctx javalin context<br>
	 * 	queryparam:
	 * <ul>
	 * 	<li>url: bandcamp url of the album to import</li>
	 * 	<li>cat: category, COLLECTION or WISHLIST</li>
	 * </ul> 
	 * @return album status<br>
	 * <ul>
	 * 	<li>SKIP: album skipped because already existing</li>
	 * 	<li>OK: album imported</li>
	 * 	<li>other: error message</li>
	 * </ul> 
	 */
	synchronized public Context importAlbum(Context ctx) {
		try {
			String url = ctx.queryParam("url");
			AlbumCategory cat = AlbumCategory.valueOf(ctx.queryParam("cat"));
			List<Album> existing = albumDao.queryForEq("url", url);
			if (!existing.isEmpty()) {
				if (existing.getFirst().getCategory() == cat) {
					log.info("Skipping existing {}", url);
					return ctx.html("SKIP");
				}
				else {
					log.info("change category {}", url);
					existing.getFirst().setCategory(cat);
					albumDao.update(existing.getFirst());
					return ctx.html("OK");
				}
			}
			log.info("Extracting {}", url);
			Album album = importer.extractAlbum(url);
			album.setCategory(cat);
			importer.insertAlbum(album);
			Thread.sleep(2000); // Wait to avoid triggering "Too many request" on bandcamp server
		} catch (Exception e) {
			return ctx.html(e.getMessage());
		}
		return ctx.html("OK");
	}
	
	/**
	 * Implements route POST /api/fanpage
	 * Import every album of the given fan page
	 * @param ctx javalin context<br>
	 * 	queryparam url: url of teh fan page to import
	 * @return empty string
	 */
	public Context importFanPage(Context ctx) {
		new Thread(() -> {
			try {
				importer.importFanPage(ctx.queryParam("url"));
			} catch (IOException | URISyntaxException | InterruptedException e) {
				log.error("", e);
			}
		}).start();
		return ctx.html("");
	}
	
	/**
	 * Implements route GET /api/fanpage
	 * Get the status of running fan page importation
	 * @param ctx javalin context
	 * @return total, current, errors
	 */
	public Context importFanPageStatus(Context ctx) {
		return ctx.html("{\"current\": " + importer.getCurrent() +", \"total\": " + importer.getTotal() + ", \"errors\": \"" + importer.getErrors() + "\"}");
	}
	
	public Context setAlternatives(Context ctx) throws SQLException {
		List<String> alts = ctx.bodyAsClass(List.class);
		tagService.setAlternatives(ctx.pathParam("tagname"), alts);
		return ctx.html("");
	}
}

