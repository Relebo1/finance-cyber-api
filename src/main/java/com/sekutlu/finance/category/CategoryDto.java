package com.sekutlu.finance.category;

import com.sekutlu.finance.common.BaseDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


@Getter
@Setter
public class CategoryDto extends BaseDTO {

    @NotBlank @Size(max = 100)
    private String name;
    @Size(max = 255)
    private String description;
}
