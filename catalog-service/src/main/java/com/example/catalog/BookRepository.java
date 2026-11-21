package com.example.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Доступ к таблице books в PostgreSQL. */
public interface BookRepository extends JpaRepository<Book, Long> {

    /** Атомарно уменьшает число экземпляров на 1, только если они есть. Возвращает число изменённых строк (0 или 1). */
    @Modifying(clearAutomatically = true)
    @Query("update Book b set b.copies = b.copies - 1 where b.id = :id and b.copies > 0")
    int takeCopy(@Param("id") Long id);

    /** Увеличивает число экземпляров на 1. Возвращает число изменённых строк (0 или 1). */
    @Modifying(clearAutomatically = true)
    @Query("update Book b set b.copies = b.copies + 1 where b.id = :id")
    int returnCopy(@Param("id") Long id);
}
