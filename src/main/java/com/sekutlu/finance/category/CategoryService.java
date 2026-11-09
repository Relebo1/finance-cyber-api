package com.sekutlu.finance.category;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.sekutlu.finance.transaction.ResourceNotFoundException;
import com.sekutlu.finance.transaction.TransactionRepository;
import com.sekutlu.finance.transaction.ResourceConflictException;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoRepo categoRepo;
    private final CategoryMapper categoryMapper;
    private final TransactionRepository transactions;
    private final UnsafeCategoryRepository unsafeCategories;

    //create category
    @Transactional
    public CategoryDto createCategory(CategoryDto dto) {

        Category category = categoryMapper.toCategory(dto);

        Category savedCategory = categoRepo.save(category);

        return categoryMapper.toCategoryDto(savedCategory);
    }

    //get categories
    @Transactional(readOnly = true)
    public Page<CategoryDto> getCategories(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid pagination");
        // PageRequest creates a pageable object with the given page number and size
        return categoRepo.findAll(PageRequest.of(page, size))
                .map(categoryMapper::toCategoryDto);
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategory(long id) { return categoryMapper.toCategoryDto(find(id)); }

    @Transactional
    public CategoryDto updateCategory(long id, CategoryDto dto) {
        Category category = find(id); category.setName(dto.getName()); category.setDescription(dto.getDescription());
        return categoryMapper.toCategoryDto(categoRepo.save(category));
    }

    @Transactional
    public void deleteCategory(long id) {
        Category category = find(id);
        if (transactions.countByCategory_Id(id) > 0) throw new ResourceConflictException("Category is in use by transactions");
        categoRepo.delete(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> search(String name) { return unsafeCategories.search(name); }

    private Category find(long id) { return categoRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category " + id + " not found")); }
}
