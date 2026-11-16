UPDATE recipes
SET main_image_path = 'bundled:recipes/classic/shakshuka.png'
WHERE slug = 'classic-shakshuka'
  AND origin = 'CLASSIC'
  AND main_image_path IS NULL;
