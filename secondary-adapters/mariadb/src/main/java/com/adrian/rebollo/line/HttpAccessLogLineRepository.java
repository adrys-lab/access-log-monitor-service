package com.adrian.rebollo.line;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrian.rebollo.entity.LogLine;

@Repository
public interface HttpAccessLogLineRepository extends JpaRepository<LogLine, UUID> {

	Page<LogLine> findBySeqIdGreaterThanOrderBySeqIdAsc(long seq, Pageable pageable);
}
