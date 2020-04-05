package com.adrian.rebollo.dao;

import java.util.List;
import java.util.Optional;

import com.adrian.rebollo.model.HttpAccessLogLine;

public interface HttpAccessLogLineDao {

	void save(HttpAccessLogLine httpAccessLogLine);

	Optional<List<HttpAccessLogLine>> findBySeqIdGreater(long seq, int chunk);
}
