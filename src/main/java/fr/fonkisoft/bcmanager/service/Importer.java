package fr.fonkisoft.bcmanager.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.j256.ormlite.dao.Dao;

import fr.fonkisoft.bcmanager.domain.Album;
import fr.fonkisoft.bcmanager.domain.Album.AlbumCategory;
import fr.fonkisoft.bcmanager.domain.AlbumTag;
import fr.fonkisoft.bcmanager.domain.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to import Albums
 */
@Slf4j
public class Importer {
	private Dao<Album, Long> albumDao;
	private Dao<Tag, Long> tagDao;
	private Dao<AlbumTag, Long> albumTagDao;

	private static final Pattern releasedPattern = Pattern.compile(".*elease. ([A-Za-z]+ [0-9]+, \\d{4}).*");
	
	private HashMap<String, Tag> cacheTag = new HashMap<>();
	
	@Getter
	private static final Importer instance = new Importer();
	
	@Getter
	private int total = 0;
	@Getter
	private int current = 0;
	@Getter
	private String errors = "";
	
	private Importer() {
		albumDao = DaoManager.getInstance().getAlbumDao();
		tagDao = DaoManager.getInstance().getTagDao();
		albumTagDao = DaoManager.getInstance().getAlbumTagDao();
		
		try {
			for (Tag tag : tagDao.queryForAll()) {
				cacheTag.put(tag.getName(), tag);
				for (String alt : tag.getAlternativesAsList()) {
					cacheTag.put(alt, tag);
				}
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void insertAlbum(Album album) throws SQLException {
		albumDao.create(album);
		for (AlbumTag albumTag : album.getTags()) {
			Tag tag = cacheTag.get(albumTag.getTag().getName());
			if (tag != null) {
				albumTag.setTag(tag);
			}
			else {
				tagDao.create(albumTag.getTag());
				cacheTag.put(albumTag.getTag().getName(), albumTag.getTag());
			}
			albumTagDao.create(albumTag);
		}
	}
	
	public Album extractAlbum(String url) throws IOException, URISyntaxException {
		Document doc = Jsoup.parse(new URI(url).toURL(), 10000);
		String location = doc.selectFirst("#band-name-location .location").text();
		String albumName = doc.selectFirst("#name-section .trackTitle").text();
		String artist = doc.selectFirst("#name-section span a").text();
		int duration = 0;
		String trackList = "";
		int trackIdx = 1;
		for (Element titleNode : doc.select("div.title")) {
			String trackName = titleNode.selectFirst(".track-title").text();
			Element eDuration = titleNode.selectFirst(".time");
			String trackDuration = eDuration != null ? eDuration.text().trim() : "";
			trackList += trackIdx + ". " + trackName + " " + trackDuration + "\n";
			trackIdx++;
			String[] splited = trackDuration.split(":");
			try {
				duration += Integer.parseInt(splited[1]) + 60 * Integer.parseInt(splited[0]);
			}
			catch(Exception e) {
				log.error(url + ": Error while parsing duration " + trackDuration, e);
			}
		}
		LocalDate releaseLocalDate = null;
		String released = doc.selectFirst("div.tralbumData.tralbum-credits").text().trim();
		try {
			Matcher matcher = releasedPattern.matcher(released);
			if (matcher.find()) {
				DateFormat df = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);
				releaseLocalDate = df.parse(matcher.group(1)).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			}
			else {
				log.error("{}: Date pattern error {}", url, released);
			}
		} catch (ParseException e) {
			log.error(url + ": Error while parsing release date " + released, e);
		}
		Album album = Album.builder()
				.url(url)
				.artist(artist)
				.name(albumName)
				.location(location)
				.trackList(trackList)
				.duration(duration)
				.releaseDate(releaseLocalDate != null ? releaseLocalDate.toString() : "")
				.category(AlbumCategory.COLLECTION)
				.build();
		album.setTags(extractTags(doc, album));
		return album;
	}
	
	private Collection<AlbumTag> extractTags(Document doc, Album album) {
		Element tagsDiv = doc.selectFirst("div.tralbum-tags");
		return tagsDiv.selectStream("a.tag")
				.map(tagElement -> new AlbumTag(album, tagElement.text().toLowerCase()))
				.toList();
	}
	
	public void importFanPage(String fanUrl) throws MalformedURLException, IOException, URISyntaxException, InterruptedException {
		errors = "";
		total = 0;
		current = 0;
		Document doc = Jsoup.parse(new URI(fanUrl).toURL(), 10000);
		String dataBlob = doc.select("div#pagedata").attr("data-blob");
		JsonNode data = new ObjectMapper().readTree(dataBlob.replace("&quot;", "\""));
		int collectionItemCount = data.get("collection_data").get("item_count").asInt();
		int wishlistItemCount = data.get("wishlist_data").get("item_count").asInt();
		
		total = collectionItemCount + wishlistItemCount;
		importFanPageCollection(data, true);
		importFanPageCollection(data, false);
	}
	
	private void importFanPageCollection(JsonNode data, boolean collection) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		long fanId = data.get("fan_data").get("fan_id").asLong();
		String lastToken = data.get(collection ? "collection_data" : "wishlist_data").get("last_token").textValue();
		
		
		List<String> urls = new ArrayList<>();
		for (Iterator<Entry<String, JsonNode>> iterator = data.get("item_cache").get(collection ? "collection" : "wishlist").fields(); iterator.hasNext();) {
			urls.add(iterator.next().getValue().get("item_url").textValue());
		}
		while (!urls.isEmpty()) {
			for (String url : urls) {
				try {
					List<Album> existing = albumDao.queryForEq("url", url);
					if (!existing.isEmpty()) {
						if (existing.getFirst().getCategory() == AlbumCategory.COLLECTION && collection
								|| existing.getFirst().getCategory() == AlbumCategory.WISHLIST && !collection) {
							log.info("Skipping existing {}", url);
							continue;
						}
						else {
							log.info("change category {}", url);
							existing.getFirst().setCategory(collection ? AlbumCategory.COLLECTION : AlbumCategory.WISHLIST);
							albumDao.update(existing.getFirst());
							continue;
						}
					}
					log.info("Extracting {}", url);
					Album album = extractAlbum(url);
					if (!collection) {
						album.setCategory(AlbumCategory.WISHLIST);
					}
					insertAlbum(album);
					Thread.sleep(2000); // Wait to avoid triggering "Too many request" on bandcamp server
				}
				catch (Exception e) {
					log.error("Error while processing " + url, e);
					errors += "Error while processing " + url + " : " + e.getMessage() + "\n";
				}
				finally {
					current++;
				}
			}
			urls.clear();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://bandcamp.com/api/fancollection/1/" + (collection ? "collection" : "wishlist") + "_items"))
					.POST(HttpRequest.BodyPublishers.ofString("{\"fan_id\":" + fanId + ",\"older_than_token\":\"" + lastToken + "\",\"count\":20}"))
					.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			Thread.sleep(500);
			JsonNode collectionData = new ObjectMapper().readTree(response.body());
			lastToken = collectionData.get("last_token").textValue();
			for (Iterator<JsonNode> iterator = collectionData.get("items").elements(); iterator.hasNext();) {
				urls.add(iterator.next().get("item_url").textValue());
			}
		}
	}
}
