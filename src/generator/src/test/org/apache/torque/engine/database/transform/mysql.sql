# -----------------------------------------------------------------------
# course
# -----------------------------------------------------------------------
drop table if exists course;

CREATE TABLE course
(
	id INTEGER NOT NULL,
	col_a CHAR (5),
    PRIMARY KEY(id)
);
