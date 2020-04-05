create table log
(
    seq_id              BIGINT AUTO_INCREMENT NOT NULL,
    insert_time         DATETIME NOT NULL,
    line                VARCHAR(500) UNIQUE NOT NULL,
    host                VARCHAR(30),
    identifier          VARCHAR(30),
    general_user        VARCHAR(30),
    date_time           DATETIME,
    http_method         VARCHAR(10),
    resource            VARCHAR(100),
    protocol            VARCHAR(30),
    returned_status     integer,
    content_size        BIGINT,
    primary key (seq_id)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_general_ci;