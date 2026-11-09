package com.sekutlu.finance.category;


import com.sekutlu.finance.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Category extends BaseEntity {

	@Column(nullable = false, length = 100, unique = true)
    private String name;
	@Column(length = 255)
    private String description;
}
