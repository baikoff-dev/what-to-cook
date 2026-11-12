ALTER TABLE recipes
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX ingredients_name_ru_lower_idx
    ON ingredients (LOWER(name_ru));
