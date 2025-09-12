package com.bookRel.br.controller;

import com.bookRel.br.domain.Book;
import com.bookRel.br.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {
  private final BookService service;

  @PostMapping
  public Book create(@RequestBody Book req){ return service.save(req); }

  @GetMapping
  public List<Book> list(){ return service.findAll(); }
}
