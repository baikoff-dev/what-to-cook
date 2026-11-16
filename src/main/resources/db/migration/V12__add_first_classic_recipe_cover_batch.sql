UPDATE recipes
SET main_image_path = 'bundled:recipes/classic/shakshuka.jpg'
WHERE slug = 'classic-shakshuka'
  AND origin = 'CLASSIC'
  AND main_image_path = 'bundled:recipes/classic/shakshuka.png';

UPDATE recipes recipe
SET main_image_path = covers.image_path
FROM (VALUES
    ('classic-syrniki', 'bundled:recipes/classic/classic-syrniki.jpg'),
    ('belarusian-draniki', 'bundled:recipes/classic/belarusian-draniki.jpg'),
    ('spaghetti-aglio-e-olio', 'bundled:recipes/classic/spaghetti-aglio-e-olio.jpg')
) AS covers(recipe_slug, image_path)
WHERE recipe.slug = covers.recipe_slug
  AND recipe.origin = 'CLASSIC'
  AND recipe.main_image_path IS NULL;
