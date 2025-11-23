package fr.fonkisoft.bcmanager.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import fr.fonkisoft.bcmanager.JavalinServer;
import fr.fonkisoft.bcmanager.domain.Album;
import fr.fonkisoft.bcmanager.domain.AlbumTag;
import fr.fonkisoft.bcmanager.domain.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DaoManager {
	
	@Getter
	private static DaoManager instance = new DaoManager();

	@Getter
	private Dao<Album, Long> albumDao;
	
	@Getter
	private Dao<Tag, Long> tagDao;
	
	@Getter
	private Dao<AlbumTag, Long> albumTagDao;
	
	private ConnectionSource connectionSource;
	
	private DaoManager() {
		try {
			log.info("Openning database {}", JavalinServer.getDatabasePath());
			String databaseUrl = "jdbc:h2:file:" + JavalinServer.getDatabasePath();
			connectionSource = new JdbcConnectionSource(databaseUrl, JavalinServer.getDatabaseUser(), JavalinServer.getDatabasePassword());
		
			albumDao = com.j256.ormlite.dao.DaoManager.createDao(connectionSource, Album.class);
			tagDao = com.j256.ormlite.dao.DaoManager.createDao(connectionSource, Tag.class);
			albumTagDao = com.j256.ormlite.dao.DaoManager.createDao(connectionSource, AlbumTag.class);
		
			TableUtils.createTableIfNotExists(connectionSource, Album.class);
			TableUtils.createTableIfNotExists(connectionSource, Tag.class);
			TableUtils.createTableIfNotExists(connectionSource, AlbumTag.class);
			albumTagDao.executeRawNoArgs("ALTER TABLE ALBUM_TAG ADD CONSTRAINT IF NOT EXISTS FK_ALBUMTAG_ALBUM FOREIGN KEY (ALBUM_ID) REFERENCES ALBUM(ID)");
			albumTagDao.executeRawNoArgs("ALTER TABLE ALBUM_TAG ADD CONSTRAINT IF NOT EXISTS FK_ALBUMTAG_TAG FOREIGN KEY (TAG_ID) REFERENCES TAG(ID)");
			
			removeDuplicatedTagsOfAlbum();
			suggestTagAlts();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		try {
			connectionSource.close();
		} catch (Exception e) {
			log.error("Error while closing database", e);
		}
	}
	
	public void removeDuplicatedTagsOfAlbum() throws SQLException {
		log.info("Remove duplicate tags on albums");
		Set<String> existingJoin = new HashSet<>();
		for (AlbumTag at : albumTagDao.queryForAll()) {
			String key = at.getAlbum().getId() + "," + at.getTag().getId();
			if (existingJoin.contains(key)) {
				Tag tag = tagDao.queryForId(at.getTag().getId());
				Album album = albumDao.queryForId(at.getAlbum().getId());
				log.info("Removed {} from {} : {}", tag.getName(), album.getArtist(), album.getName());
				albumTagDao.delete(at);
			}
			existingJoin.add(key);
		}
	}
	
	public void suggestTagAlts() throws SQLException {
		log.info("Search for tag alternatives");
		Map<String, List<Tag>> tagsByNormalized = new HashMap<>();
		Map<String, List<Tag>> tagsByAcronym = new HashMap<>();
		for (Tag tag : tagDao.queryForAll()) {
			String normalized = tag.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
			tagsByNormalized.computeIfAbsent(normalized, (k) -> new ArrayList<>()).add(tag);
			String acronym;
			if (tag.getName().toLowerCase().contains(" ") || tag.getName().toLowerCase().contains("-")) {
				acronym = List.of(tag.getName().toLowerCase().split("[ -]+")).stream().map(word -> word.substring(0, 1)).collect(Collectors.joining());
			}
			else {
				acronym = tag.getName().toLowerCase();
			}
			tagsByAcronym.computeIfAbsent(acronym, (k) -> new ArrayList<>()).add(tag);
			
		}
		for (List<Tag> tags : tagsByNormalized.values()) {
			if (tags.size() > 1) {
				log.info("Possible alternatives: {}", tags.stream().map(Tag::getName).collect(Collectors.joining(", ")));
			}
		}
		for (List<Tag> tags : tagsByAcronym.values()) {
			if (tags.size() > 1 && tags.stream().map(Tag::getName).anyMatch(n -> !n.contains(" ") && !n.contains("-"))) {
				log.info("Possible alternatives: {}", tags.stream().map(Tag::getName).collect(Collectors.joining(", ")));
			}
		}
	}
}
