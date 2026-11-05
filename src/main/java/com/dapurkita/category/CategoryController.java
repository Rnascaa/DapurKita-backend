package com.dapurkita.category;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kategori berasal dari seed data. Tidak ada endpoint tambah, ubah, atau hapus. */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryRepository categories;

    public CategoryController(CategoryRepository categories) {
        this.categories = categories;
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categories.findAllByOrderByIdAsc().stream().map(CategoryResponse::from).toList();
    }
}
