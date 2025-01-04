DROP TABLE IF EXISTS actions, apps CASCADE;

CREATE TABLE IF NOT EXISTS actions (
  id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  app varchar(100) NOT NULL,
  uri varchar(100) NOT NULL,
  ip varchar(15) NOT NULL,
  timestamp timestamp NOT NULL,

  CONSTRAINT pk_actions PRIMARY KEY (id)
);