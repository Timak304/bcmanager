package fr.fonkisoft.bcmanager.domain;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
	
	@JsonIgnore
	@DatabaseField
	String alternatives;
	
	public Tag(String name) {
		this.name = name;
	}
	
	public List<String> getAlternativesAsList() {
		if (alternatives == null || alternatives.length() < 2) {
			return List.of();
		}
		return List.of(alternatives.substring(1, alternatives.length() - 1).split(";"));
	}
	
	public void setAlternativesAsList(List<String> alts) {
		alternatives = alts.stream().collect(Collectors.joining(";", ";", ";"));
	}
}
