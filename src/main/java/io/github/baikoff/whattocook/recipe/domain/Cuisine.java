package io.github.baikoff.whattocook.recipe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cuisines")
public class Cuisine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String code;

	@Column(name = "name_ru", nullable = false, unique = true, length = 150)
	private String nameRu;

	@Column(name = "name_en", length = 150)
	private String nameEn;

	protected Cuisine() {
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getNameRu() {
		return nameRu;
	}

	public String getNameEn() {
		return nameEn;
	}
}
