create table stats
(
    seq_id              BIGINT AUTO_INCREMENT NOT NULL,
    insert_time         DATETIME NOT NULL,
    requests            BIGINT NOT NULL,
    valid_requests      BIGINT NOT NULL,
    invalid_requests    BIGINT NOT NULL,
    total_content       BIGINT NOT NULL,
    primary key (seq_id)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_general_ci;