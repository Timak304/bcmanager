package fr.fonkisoft.bcmanager.service;

import java.sql.SQLException;

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
}
