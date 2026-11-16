UPDATE recipes recipe
SET main_image_path = covers.image_path
FROM (VALUES
    ('classic-frittata', 'bundled:recipes/classic/classic-frittata.jpg'),
    ('spaghetti-al-pomodoro', 'bundled:recipes/classic/spaghetti-al-pomodoro.jpg'),
    ('classic-ratatouille', 'bundled:recipes/classic/classic-ratatouille.jpg'),
    ('gratin-dauphinois', 'bundled:recipes/classic/gratin-dauphinois.jpg')
) AS covers(recipe_slug, image_path)
WHERE recipe.slug = covers.recipe_slug
  AND recipe.origin = 'CLASSIC'
  AND recipe.main_image_path IS NULL;
