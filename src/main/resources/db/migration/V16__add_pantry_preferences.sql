CREATE TABLE pantry_ingredients (
    ingredient_id BIGINT PRIMARY KEY REFERENCES ingredients(id) ON DELETE CASCADE
);

INSERT INTO pantry_ingredients (ingredient_id)
SELECT id
FROM ingredients
WHERE code IN ('WATER', 'VEGETABLE_OIL', 'SALT');
