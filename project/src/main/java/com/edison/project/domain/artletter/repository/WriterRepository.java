package com.edison.project.domain.artletter.repository;


import com.edison.project.domain.artletter.entity.Writer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WriterRepository extends JpaRepository<Writer, Long> {

}
