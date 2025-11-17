package fr.fonkisoft.bcmanager.domain;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Class mapping for Album
 */
@Data
@EqualsAndHashCode(of = { "artist", "name" })
@Builder
@ToString(of = { "id", "artist", "name" })
@NoArgsConstructor
@AllArgsConstructor
@DatabaseTable(tableName = "album")
public class Album {
	
	public static enum AlbumCategory {
		COLLECTION,
		WISHLIST
	}

	/** Technical ID */
	@DatabaseField(generatedId = true)
	Long id;
	
	/** Artist name */
	@DatabaseField
	String artist;
	
	/** Album title */
	@DatabaseField
	String name;
	
	/** Bandcamp URL */
	@DatabaseField
	String url;
	
	/** Geographical location of the artist */
	@DatabaseField
	String location;
	
	/** Album release date */
	@DatabaseField
	String releaseDate;
	
	/** Track list with durations */
	@DatabaseField(dataType = DataType.LONG_STRING)
	String trackList;
	
	/** Total duration in minutes */
	@DatabaseField
	Integer duration;
	
	@DatabaseField
	AlbumCategory category;

	/** Related tags */
	@JsonIgnore
	@ForeignCollectionField(eager = true, maxEagerLevel = 0)
	Collection<AlbumTag> tags;
	
	/** Tag list not persisted, use for DTO with frontend */
	List<String> tagList;
}
