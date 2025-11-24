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

import fr.fonkisoft.bcmanager.domain.Album;
import fr.fonkisoft.bcmanager.domain.AlbumTag;
import fr.fonkisoft.bcmanager.domain.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TagService {
	
	private Dao<Tag, Long> tagDao;
	private Dao<AlbumTag, Long> albumTagDao;
	private Dao<Album, Long> albumDao;

	@Getter
	private static final TagService instance = new TagService();
	
	private TagService() {
		tagDao = DaoManager.getInstance().getTagDao();
		albumTagDao = DaoManager.getInstance().getAlbumTagDao();
		albumDao = DaoManager.getInstance().getAlbumDao();
	}

	public void setAlternatives(String tagName, List<String> alts) throws SQLException {
		
		Tag tag = tagDao.queryBuilder().where().eq("name", tagName).queryForFirst();
		
		List<String> strAlt = new ArrayList<>(tag.getAlternativesAsList());
		for (String altName : alts) {
			Tag altTag = tagDao.queryBuilder().where().eq("name", altName).queryForFirst();
			for (AlbumTag at : albumTagDao.queryBuilder().where().eq("TAG_ID", altTag.getId()).query()) {
				at.setTag(tag);
				albumTagDao.update(at);
				removeDuplicatedTagsOfAlbum(at.getAlbum().getId());
			}
			strAlt.add(altTag.getName());
			strAlt.addAll(altTag.getAlternativesAsList());
			tagDao.delete(altTag);
		}
		tag.setAlternativesAsList(strAlt);
		tagDao.update(tag);
	}
	
	public void removeDuplicatedTagsOfAlbum(Long albumId) throws SQLException {
		Set<String> existingJoin = new HashSet<>();
		for (AlbumTag at : albumTagDao.queryBuilder().where().eq("ALBUM_ID", albumId).query()) {
			String key = at.getAlbum().getId() + "," + at.getTag().getId();
			if (existingJoin.contains(key)) {
				Tag tag = tagDao.queryForId(at.getTag().getId());
				Album album = albumDao.queryForId(at.getAlbum().getId());
				log.debug("Removed {} from {} : {}", tag.getName(), album.getArtist(), album.getName());
				albumTagDao.delete(at);
			}
			existingJoin.add(key);
		}
	}
	
	public List<List<Tag>> suggestTagAlts() throws SQLException {
		List<List<Tag>>  result = new ArrayList<>();
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
				result.add(tags);
			}
		}
		for (List<Tag> tags : tagsByAcronym.values()) {
			if (tags.size() > 1 && tags.stream().map(Tag::getName).anyMatch(n -> !n.contains(" ") && !n.contains("-"))) {
				result.add(tags);
			}
		}
		return result;
	}
}
