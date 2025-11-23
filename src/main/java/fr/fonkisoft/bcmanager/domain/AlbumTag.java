package fr.fonkisoft.bcmanager.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Join table between Album and Tag
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@DatabaseTable(tableName = "album_tag")
public class AlbumTag {

	/** Technical ID */
	@DatabaseField(generatedId = true)
	Long id;
	
	@JsonIgnore
	@DatabaseField(foreign = true)
	Album album;
	
	@DatabaseField(foreign = true)
	Tag tag;
	
	/** Is extracted from bandcamp or added manually */
	@DatabaseField
	Boolean custom = Boolean.FALSE;
	
	public AlbumTag(Album album, String tag) {
		this.album = album;
		this.tag = new Tag(tag);
	}
}