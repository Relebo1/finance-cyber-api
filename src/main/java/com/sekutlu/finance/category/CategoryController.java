package com.sekutlu.finance.category;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    // page and size are optional query params, defaulting to page 0 with 10 items
    @GetMapping({"/category", "/categories"})
    public ResponseEntity<Page<CategoryDto>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(categoryService.getCategories(page, size));
    }

    @PostMapping({"/category", "/categories"})
    public ResponseEntity<CategoryDto> addCategory(@Valid @RequestBody CategoryDto categoryDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(categoryDto));
    }
    @GetMapping({"/category/search", "/categories/search"})
    public List<CategoryDto> search(@RequestParam String name) { return categoryService.search(name); }
    @GetMapping({"/category/{id}", "/categories/{id}"})
    public CategoryDto get(@PathVariable long id) { return categoryService.getCategory(id); }
    @PutMapping({"/category/{id}", "/categories/{id}"})
    public CategoryDto update(@PathVariable long id, @Valid @RequestBody CategoryDto dto) { return categoryService.updateCategory(id, dto); }
    @DeleteMapping({"/category/{id}", "/categories/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { categoryService.deleteCategory(id); }
}
