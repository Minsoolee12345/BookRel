package com.bookRel.br.service;

import com.bookRel.br.domain.Book;
import com.bookRel.br.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {
  private final BookRepository repo;

  public Book save(Book b){ return repo.save(b); }
  public List<Book> findAll(){ return repo.findAll(); }
}
