CREATE TABLE jiveForum (
  forumID           INT NOT NULL,
  name              VARCHAR(255),
  description       TEXT,
  modifiedDate      VARCHAR(15),
  creationDate      VARCHAR(15),
  moderated         INT default -1 NOT NULL,
  PRIMARY KEY       (forumID)
);
