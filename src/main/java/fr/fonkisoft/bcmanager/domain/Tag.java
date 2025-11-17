package fr.fonkisoft.bcmanager.domain;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Class mapping for tag
 */
@Data
@EqualsAndHashCode(of = { "name" })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DatabaseTable(tableName = "tag")
public class Tag {

	/** Technical ID */
	@DatabaseField(generatedId = true)
	Long id;

	/** Tag name */
	@DatabaseField(unique = true)
	String name;
}
