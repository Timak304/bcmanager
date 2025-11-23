package fr.fonkisoft.bcmanager.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.dao.Dao;

import fr.fonkisoft.bcmanager.domain.AlbumTag;
import fr.fonkisoft.bcmanager.domain.Tag;
import lombok.Getter;

public class TagService {
	
	private Dao<Tag, Long> tagDao;
	private Dao<AlbumTag, Long> albumTagDao;

	@Getter
	private static final TagService instance = new TagService();
	
	private TagService() {
		tagDao = DaoManager.getInstance().getTagDao();
		albumTagDao = DaoManager.getInstance().getAlbumTagDao();
	}

	public void setAlternatives(String tagName, List<String> alts) throws SQLException {
		
		Tag tag = tagDao.queryBuilder().where().eq("name", tagName).queryForFirst();
		
		List<String> strAlt = new ArrayList<>(tag.getAlternativesAsList()); 
		for (String altName : alts) {
			Tag altTag = tagDao.queryBuilder().where().eq("name", altName).queryForFirst();
			for (AlbumTag at : albumTagDao.queryBuilder().where().eq("TAG_ID", altTag.getId()).query()) {
				at.setTag(tag);
				albumTagDao.update(at);
			}
			strAlt.add(altTag.getName());
			tagDao.delete(altTag);
		}
		tag.setAlternativesAsList(strAlt);
		tagDao.update(tag);
	}
	
}
