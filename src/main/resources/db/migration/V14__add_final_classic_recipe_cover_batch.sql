UPDATE recipes recipe
SET main_image_path = covers.image_path
FROM (VALUES
    ('georgian-lobio', 'bundled:recipes/classic/georgian-lobio.jpg'),
    ('georgian-chakhokhbili', 'bundled:recipes/classic/georgian-chakhokhbili.jpg'),
    ('chinese-tomato-and-egg', 'bundled:recipes/classic/chinese-tomato-and-egg.jpg'),
    ('chinese-egg-fried-rice', 'bundled:recipes/classic/chinese-egg-fried-rice.jpg')
) AS covers(recipe_slug, image_path)
WHERE recipe.slug = covers.recipe_slug
  AND recipe.origin = 'CLASSIC'
  AND recipe.main_image_path IS NULL;
