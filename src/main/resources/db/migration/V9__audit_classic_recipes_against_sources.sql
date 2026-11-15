ALTER TABLE recipe_ingredients
    DROP CONSTRAINT recipe_ingredients_unit_check;

ALTER TABLE recipe_ingredients
    ADD CONSTRAINT recipe_ingredients_unit_check CHECK (unit IN (
        'GRAM', 'KILOGRAM', 'MILLILITER', 'LITER', 'PIECE', 'TEASPOON',
        'TABLESPOON', 'PINCH', 'CLOVE', 'SLICE', 'BUNCH', 'PACKAGE', 'CUP'
    ));

ALTER TABLE ingredient_substitutions
    DROP CONSTRAINT ingredient_substitutions_unit_check;

ALTER TABLE ingredient_substitutions
    ADD CONSTRAINT ingredient_substitutions_unit_check CHECK (unit IN (
        'GRAM', 'KILOGRAM', 'MILLILITER', 'LITER', 'PIECE', 'TEASPOON',
        'TABLESPOON', 'PINCH', 'CLOVE', 'SLICE', 'BUNCH', 'PACKAGE', 'CUP'
    ));

INSERT INTO ingredients (code, name_ru, name_en) VALUES
    ('VANILLA_SUGAR', 'Ванильный сахар', 'Vanilla sugar'),
    ('WHITE_PEPPER', 'Белый перец', 'White pepper'),
    ('SESAME_OIL', 'Кунжутное масло', 'Sesame oil'),
    ('SHAOXING_WINE', 'Шаосинское вино', 'Shaoxing wine'),
    ('DRY_SHERRY', 'Сухой херес', 'Dry sherry'),
    ('MIRIN', 'Мирин', 'Mirin'),
    ('RED_BELL_PEPPER', 'Красный сладкий перец', 'Red bell pepper'),
    ('CELERY', 'Сельдерей', 'Celery'),
    ('TKEMALI_SAUCE', 'Соус ткемали', 'Tkemali sauce'),
    ('WHOLE_CHICKEN', 'Целая курица', 'Whole chicken'),
    ('GREEN_PEPPER', 'Зелёный острый перец', 'Green chili pepper'),
    ('RED_PEPPER', 'Красный острый перец', 'Red chili pepper'),
    ('COOKED_WHITE_RICE', 'Готовый белый рис', 'Cooked white rice'),
    ('PEANUT_OIL', 'Арахисовое масло', 'Peanut oil')
ON CONFLICT (name_ru) DO UPDATE
SET code = EXCLUDED.code,
    name_en = COALESCE(ingredients.name_en, EXCLUDED.name_en);

UPDATE recipes recipe
SET slug = audited.new_slug,
    name_ru = audited.name_ru,
    name_en = audited.name_en,
    description = audited.description,
    servings = audited.servings,
    preparation_minutes = audited.preparation_minutes,
    cooking_minutes = audited.cooking_minutes,
    difficulty = audited.difficulty,
    source_name = audited.source_name,
    source_url = audited.source_url,
    personal_notes = 'Рецепт и техника приготовления сверены с указанным источником. Количества, не заданные источником точно, нормализованы для приложения.',
    updated_at = CURRENT_TIMESTAMP,
    version = recipe.version + 1
FROM (VALUES
    (
        'classic-syrniki', 'classic-syrniki',
        'Классические сырники', 'Classic syrniki',
        'Сырники из творога 5% с ванильным сахаром, выдержанные перед формовкой и обжаренные на слабом огне.',
        4, 20, 15, 'EASY',
        'Мой социальный помощник — классические сырники',
        'https://msp.dszn.ru/recipes/syrniki-klassicheskie'
    ),
    (
        'belarusian-draniki', 'belarusian-draniki',
        'Белорусские драники', 'Belarusian draniki',
        'Драники из крупно натёртого картофеля с луком и чесноком по семейному рецепту из Беларуси.',
        4, 20, 15, 'EASY',
        'JDC Global Kitchen — Daria’s Draniki',
        'https://www.jdc.org/jdc-global-kitchen/'
    ),
    (
        'spaghetti-aglio-e-olio', 'spaghetti-aglio-e-olio',
        'Спагетти aglio e olio', 'Spaghetti aglio e olio',
        'Спагетти с чесноком, свежим чили, петрушкой и оливковым маслом, соединённые водой от варки пасты.',
        4, 10, 10, 'EASY',
        'La Cucina Italiana — spaghetti aglio, olio e peperoncino',
        'https://www.lacucinaitaliana.it/ricetta/primi/spaghetti-aglio-olio-e-peperoncino-2/'
    ),
    (
        'spinach-frittata', 'classic-frittata',
        'Классическая фриттата', 'Classic frittata',
        'Простая итальянская фриттата с петрушкой, приготовленная на сковороде и подрумяненная с двух сторон.',
        8, 0, 5, 'EASY',
        'La Cucina Italiana — frittata',
        'https://www.lacucinaitaliana.it/ricetta/secondi/frittata-2/'
    ),
    (
        'spaghetti-al-pomodoro', 'spaghetti-al-pomodoro',
        'Спагетти с томатным соусом', 'Spaghetti al pomodoro',
        'Спагетти с соусом из свежих сливовидных помидоров, чеснока, базилика и оливкового масла.',
        4, 20, 40, 'EASY',
        'La Cucina Italiana — spaghetti al pomodoro',
        'https://www.lacucinaitaliana.it/ricetta/primi/spaghetti-al-pomodoro/'
    ),
    (
        'classic-ratatouille', 'classic-ratatouille',
        'Рататуй', 'Ratatouille',
        'Прованское овощное блюдо с отдельно подготовленными помидорами и последовательным тушением овощей.',
        4, 25, 40, 'MEDIUM',
        'France.fr — ratatouille',
        'https://www.france.fr/en/article/taste-provence-this-weekend-and-make-a-ratatouille/'
    ),
    (
        'gratin-dauphinois', 'gratin-dauphinois',
        'Гратен дофинуа', 'Gratin dauphinois',
        'Картофель, медленно запечённый со сливками, молоком и мускатным орехом без добавления сыра.',
        6, 20, 90, 'MEDIUM',
        'Taste France — gratin dauphinois',
        'https://www.tastefrance.com/recipes/french-classics/gratin-dauphinois'
    ),
    (
        'georgian-lobio', 'georgian-lobio',
        'Лобио по-мегрельски', 'Megrelian lobio',
        'Размятая красная фасоль с луком, чесноком, свежей зеленью и соусом ткемали.',
        6, 20, 120, 'MEDIUM',
        'Georgia Travel — Megrelian beans with tkemali',
        'https://georgia.travel/lobio-bean-stew'
    ),
    (
        'georgian-chakhokhbili', 'georgian-chakhokhbili',
        'Чахохбили', 'Georgian chakhokhbili',
        'Курица, приготовленная без добавления воды и масла в собственном соку с луком, помидорами и зеленью.',
        6, 20, 55, 'MEDIUM',
        'Georgia Travel — chakhokhbili',
        'https://georgia.travel/chakhokhbili'
    ),
    (
        'chinese-tomato-and-egg', 'chinese-tomato-and-egg',
        'Помидоры с яйцом по-китайски', 'Chinese tomato and egg',
        'Быстрое домашнее китайское блюдо с мягким яйцом и помидорами в лёгком сладко-солёном соусе.',
        2, 5, 5, 'EASY',
        'The Woks of Life — Chinese tomato egg stir-fry',
        'https://thewoksoflife.com/stir-fried-tomato-and-egg/'
    ),
    (
        'chinese-egg-fried-rice', 'chinese-egg-fried-rice',
        'Жареный рис с яйцом', 'Egg fried rice',
        'Домашний жареный рис, в котором холодный рис смешивается с ещё немного жидким яйцом и подрумянивается.',
        4, 5, 5, 'EASY',
        'Omnivore''s Cookbook — egg fried rice',
        'https://omnivorescookbook.com/egg-fried-rice/'
    ),
    (
        'classic-shakshuka', 'classic-shakshuka',
        'Шакшука', 'Classic shakshouka',
        'Североафриканская шакшука из свежих помидоров, чеснока, зелёного чили, паприки и яиц.',
        8, 5, 20, 'EASY',
        'Foodish — classic shakshouka',
        'https://foodish.anumuseum.org.il/en/recipe/classic-shakshouka/'
    )
) AS audited(
    current_slug, new_slug, name_ru, name_en, description, servings,
    preparation_minutes, cooking_minutes, difficulty, source_name, source_url
)
WHERE recipe.slug = audited.current_slug;

DELETE FROM recipe_steps
WHERE recipe_id IN (SELECT id FROM recipes WHERE origin = 'CLASSIC');

DELETE FROM recipe_equipment
WHERE recipe_id IN (SELECT id FROM recipes WHERE origin = 'CLASSIC');

DELETE FROM recipe_cooking_methods
WHERE recipe_id IN (SELECT id FROM recipes WHERE origin = 'CLASSIC');

DELETE FROM recipe_ingredients
WHERE recipe_id IN (SELECT id FROM recipes WHERE origin = 'CLASSIC');

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, amount, unit, optional, sort_order)
SELECT recipe.id, ingredient.id, values_to_insert.amount, values_to_insert.unit,
       values_to_insert.optional, values_to_insert.sort_order
FROM (VALUES
    ('classic-syrniki', 'COTTAGE_CHEESE', 400.000, 'GRAM', FALSE, 1),
    ('classic-syrniki', 'EGG', 1.000, 'PIECE', FALSE, 2),
    ('classic-syrniki', 'WHEAT_FLOUR', 3.000, 'TABLESPOON', FALSE, 3),
    ('classic-syrniki', 'SUGAR', 2.000, 'TABLESPOON', FALSE, 4),
    ('classic-syrniki', 'VANILLA_SUGAR', 10.000, 'GRAM', FALSE, 5),
    ('classic-syrniki', 'SALT', 1.000, 'PINCH', FALSE, 6),
    ('classic-syrniki', 'VEGETABLE_OIL', 2.000, 'TABLESPOON', FALSE, 7),

    ('belarusian-draniki', 'POTATO', 1.000, 'KILOGRAM', FALSE, 1),
    ('belarusian-draniki', 'EGG', 1.000, 'PIECE', FALSE, 2),
    ('belarusian-draniki', 'ONION', 1.000, 'PIECE', FALSE, 3),
    ('belarusian-draniki', 'WHEAT_FLOUR', 3.000, 'TABLESPOON', FALSE, 4),
    ('belarusian-draniki', 'GARLIC', 3.000, 'CLOVE', FALSE, 5),
    ('belarusian-draniki', 'SALT', 1.000, 'TEASPOON', FALSE, 6),
    ('belarusian-draniki', 'BLACK_PEPPER', 2.000, 'PINCH', FALSE, 7),
    ('belarusian-draniki', 'VEGETABLE_OIL', 4.000, 'TABLESPOON', FALSE, 8),

    ('spaghetti-aglio-e-olio', 'SPAGHETTI', 350.000, 'GRAM', FALSE, 1),
    ('spaghetti-aglio-e-olio', 'GARLIC', 1.000, 'CLOVE', FALSE, 2),
    ('spaghetti-aglio-e-olio', 'FRESH_CHILI', 1.000, 'PIECE', FALSE, 3),
    ('spaghetti-aglio-e-olio', 'PARSLEY', 0.500, 'BUNCH', FALSE, 4),
    ('spaghetti-aglio-e-olio', 'OLIVE_OIL', 50.000, 'GRAM', FALSE, 5),
    ('spaghetti-aglio-e-olio', 'WATER', 5.000, 'TABLESPOON', FALSE, 6),
    ('spaghetti-aglio-e-olio', 'SALT', 1.000, 'PINCH', FALSE, 7),

    ('classic-frittata', 'EGG', 8.000, 'PIECE', FALSE, 1),
    ('classic-frittata', 'PARSLEY', 0.500, 'BUNCH', FALSE, 2),
    ('classic-frittata', 'SALT', 0.500, 'TEASPOON', FALSE, 3),
    ('classic-frittata', 'BLACK_PEPPER', 2.000, 'PINCH', FALSE, 4),
    ('classic-frittata', 'OLIVE_OIL', 1.000, 'TABLESPOON', FALSE, 5),

    ('spaghetti-al-pomodoro', 'TOMATO', 750.000, 'GRAM', FALSE, 1),
    ('spaghetti-al-pomodoro', 'SPAGHETTI', 320.000, 'GRAM', FALSE, 2),
    ('spaghetti-al-pomodoro', 'GARLIC', 1.000, 'CLOVE', FALSE, 3),
    ('spaghetti-al-pomodoro', 'BASIL', 0.500, 'BUNCH', FALSE, 4),
    ('spaghetti-al-pomodoro', 'OLIVE_OIL', 2.000, 'TABLESPOON', FALSE, 5),
    ('spaghetti-al-pomodoro', 'SALT', 1.000, 'TEASPOON', FALSE, 6),

    ('classic-ratatouille', 'ZUCCHINI', 4.000, 'PIECE', FALSE, 1),
    ('classic-ratatouille', 'EGGPLANT', 2.000, 'PIECE', FALSE, 2),
    ('classic-ratatouille', 'RED_BELL_PEPPER', 2.000, 'PIECE', FALSE, 3),
    ('classic-ratatouille', 'TOMATO', 4.000, 'PIECE', FALSE, 4),
    ('classic-ratatouille', 'OLIVE_OIL', 4.000, 'TABLESPOON', FALSE, 5),
    ('classic-ratatouille', 'ONION', 2.000, 'PIECE', FALSE, 6),
    ('classic-ratatouille', 'GARLIC', 2.000, 'CLOVE', FALSE, 7),
    ('classic-ratatouille', 'SUGAR', 0.500, 'TEASPOON', FALSE, 8),
    ('classic-ratatouille', 'SALT', 1.000, 'TEASPOON', FALSE, 9),
    ('classic-ratatouille', 'BLACK_PEPPER', 2.000, 'PINCH', FALSE, 10),
    ('classic-ratatouille', 'BASIL', 0.500, 'BUNCH', FALSE, 11),

    ('gratin-dauphinois', 'POTATO', 1.000, 'KILOGRAM', FALSE, 1),
    ('gratin-dauphinois', 'CREAM', 250.000, 'MILLILITER', FALSE, 2),
    ('gratin-dauphinois', 'MILK', 150.000, 'MILLILITER', FALSE, 3),
    ('gratin-dauphinois', 'BUTTER', 50.000, 'GRAM', FALSE, 4),
    ('gratin-dauphinois', 'GARLIC', 1.000, 'CLOVE', FALSE, 5),
    ('gratin-dauphinois', 'NUTMEG', 6.000, 'PINCH', FALSE, 6),
    ('gratin-dauphinois', 'SALT', 1.000, 'TEASPOON', FALSE, 7),
    ('gratin-dauphinois', 'BLACK_PEPPER', 2.000, 'PINCH', FALSE, 8),

    ('georgian-lobio', 'RED_BEANS_DRY', 500.000, 'GRAM', FALSE, 1),
    ('georgian-lobio', 'ONION', 2.000, 'PIECE', FALSE, 2),
    ('georgian-lobio', 'GARLIC', 3.500, 'CLOVE', FALSE, 3),
    ('georgian-lobio', 'PARSLEY', 0.500, 'BUNCH', FALSE, 4),
    ('georgian-lobio', 'CELERY', 0.500, 'BUNCH', FALSE, 5),
    ('georgian-lobio', 'CILANTRO', 0.500, 'BUNCH', FALSE, 6),
    ('georgian-lobio', 'BLACK_PEPPER', 0.500, 'TEASPOON', FALSE, 7),
    ('georgian-lobio', 'SALT', 1.000, 'TEASPOON', FALSE, 8),
    ('georgian-lobio', 'TKEMALI_SAUCE', 1.000, 'TEASPOON', FALSE, 9),

    ('georgian-chakhokhbili', 'WHOLE_CHICKEN', 1.250, 'KILOGRAM', FALSE, 1),
    ('georgian-chakhokhbili', 'ONION', 4.000, 'PIECE', FALSE, 2),
    ('georgian-chakhokhbili', 'TOMATO', 700.000, 'GRAM', FALSE, 3),
    ('georgian-chakhokhbili', 'GARLIC', 4.000, 'CLOVE', FALSE, 4),
    ('georgian-chakhokhbili', 'CILANTRO', 1.000, 'BUNCH', FALSE, 5),
    ('georgian-chakhokhbili', 'GREEN_PEPPER', 1.000, 'PIECE', FALSE, 6),
    ('georgian-chakhokhbili', 'RED_PEPPER', 1.000, 'PIECE', FALSE, 7),
    ('georgian-chakhokhbili', 'SALT', 1.000, 'TEASPOON', FALSE, 8),

    ('chinese-tomato-and-egg', 'TOMATO', 500.000, 'GRAM', FALSE, 1),
    ('chinese-tomato-and-egg', 'GREEN_ONION', 1.000, 'PIECE', FALSE, 2),
    ('chinese-tomato-and-egg', 'EGG', 4.000, 'PIECE', FALSE, 3),
    ('chinese-tomato-and-egg', 'SALT', 0.750, 'TEASPOON', FALSE, 4),
    ('chinese-tomato-and-egg', 'WHITE_PEPPER', 0.250, 'TEASPOON', FALSE, 5),
    ('chinese-tomato-and-egg', 'SESAME_OIL', 0.500, 'TEASPOON', FALSE, 6),
    ('chinese-tomato-and-egg', 'SHAOXING_WINE', 1.000, 'TEASPOON', FALSE, 7),
    ('chinese-tomato-and-egg', 'VEGETABLE_OIL', 3.000, 'TABLESPOON', FALSE, 8),
    ('chinese-tomato-and-egg', 'SUGAR', 2.000, 'TEASPOON', FALSE, 9),
    ('chinese-tomato-and-egg', 'WATER', 60.000, 'MILLILITER', FALSE, 10),

    ('chinese-egg-fried-rice', 'PEANUT_OIL', 2.000, 'TABLESPOON', FALSE, 1),
    ('chinese-egg-fried-rice', 'EGG', 4.000, 'PIECE', FALSE, 2),
    ('chinese-egg-fried-rice', 'COOKED_WHITE_RICE', 3.000, 'CUP', FALSE, 3),
    ('chinese-egg-fried-rice', 'SALT', 0.500, 'TEASPOON', FALSE, 4),
    ('chinese-egg-fried-rice', 'GREEN_ONION', 4.000, 'PIECE', FALSE, 5),
    ('chinese-egg-fried-rice', 'SESAME_OIL', 1.000, 'TEASPOON', TRUE, 6),

    ('classic-shakshuka', 'OLIVE_OIL', 0.500, 'CUP', FALSE, 1),
    ('classic-shakshuka', 'GARLIC', 2.000, 'CLOVE', FALSE, 2),
    ('classic-shakshuka', 'GREEN_PEPPER', 2.000, 'PIECE', FALSE, 3),
    ('classic-shakshuka', 'TOMATO', 5.500, 'PIECE', FALSE, 4),
    ('classic-shakshuka', 'SALT', 2.000, 'TEASPOON', FALSE, 5),
    ('classic-shakshuka', 'PAPRIKA', 2.000, 'TEASPOON', FALSE, 6),
    ('classic-shakshuka', 'EGG', 8.000, 'PIECE', FALSE, 7)
) AS values_to_insert(recipe_slug, ingredient_code, amount, unit, optional, sort_order)
JOIN recipes recipe ON recipe.slug = values_to_insert.recipe_slug
JOIN ingredients ingredient ON ingredient.code = values_to_insert.ingredient_code;

INSERT INTO recipe_steps (recipe_id, sort_order, instruction)
SELECT recipe.id, values_to_insert.sort_order, values_to_insert.instruction
FROM (VALUES
    ('classic-syrniki', 1, 'Протереть творог через сито или тщательно размять вилкой, чтобы не осталось крупных комков.'),
    ('classic-syrniki', 2, 'Соединить творог, яйцо, обычный и ванильный сахар, соль. Перемешать до однородности.'),
    ('classic-syrniki', 3, 'Добавить в массу 1 столовую ложку муки с горкой и оставить при комнатной температуре на 15 минут.'),
    ('classic-syrniki', 4, 'Влажными руками сформировать сырники толщиной 1,5–2 см и обвалять их в оставшейся муке.'),
    ('classic-syrniki', 5, 'Разогреть масло и жарить сырники на небольшом огне по 3–4 минуты с каждой стороны до золотистой корочки.'),
    ('classic-syrniki', 6, 'Переложить на бумажное полотенце, чтобы убрать лишнее масло, и подавать тёплыми.'),

    ('belarusian-draniki', 1, 'Крупно натереть картофель и хорошо отжать лишнюю жидкость.'),
    ('belarusian-draniki', 2, 'Лук и чеснок натереть на мелкой стороне тёрки.'),
    ('belarusian-draniki', 3, 'Смешать картофель с луком, чесноком, яйцом, мукой, солью и перцем.'),
    ('belarusian-draniki', 4, 'Сформировать небольшие оладьи и жарить в разогретом масле примерно по 2 минуты с каждой стороны.'),
    ('belarusian-draniki', 5, 'Подавать горячими со сметаной, жареными грибами или луком и свежей зеленью.'),

    ('spaghetti-aglio-e-olio', 1, 'Поставить спагетти вариться в подсоленной воде до состояния al dente.'),
    ('spaghetti-aglio-e-olio', 2, 'Раздавить крупный зубчик чеснока плоской стороной ножа, очистить и мелко порубить.'),
    ('spaghetti-aglio-e-olio', 3, 'Разрезать свежий чили, удалить семена и мелко нарезать. Петрушку измельчить.'),
    ('spaghetti-aglio-e-olio', 4, 'В холодную широкую сковороду положить оливковое масло, чеснок и чили.'),
    ('spaghetti-aglio-e-olio', 5, 'Когда масло начнёт шипеть, сразу добавить петрушку, воду от варки пасты и щепотку соли.'),
    ('spaghetti-aglio-e-olio', 6, 'Переложить спагетти al dente в сковороду, быстро перемешать с соусом и немедленно подать.'),

    ('classic-frittata', 1, 'Разбить яйца в миску, добавить соль, перец и измельчённую петрушку. Взбить венчиком.'),
    ('classic-frittata', 2, 'Разогреть в сковороде немного оливкового масла, влить яйца и готовить на среднем огне.'),
    ('classic-frittata', 3, 'Перевернуть фриттату и готовить, пока вторая сторона тоже не подрумянится.'),

    ('spaghetti-al-pomodoro', 1, 'Разрезать помидоры вдоль пополам, удалить семена и лишнюю воду.'),
    ('spaghetti-al-pomodoro', 2, 'Прогреть в кастрюле немного оливкового масла с очищенным зубчиком чеснока.'),
    ('spaghetti-al-pomodoro', 3, 'Когда чеснок начнёт шипеть, добавить помидоры, соль и часть базилика. Готовить 10 минут без крышки.'),
    ('spaghetti-al-pomodoro', 4, 'Накрыть крышкой и готовить ещё 15 минут. Слегка остудить и протереть соус, удаляя кожицу.'),
    ('spaghetti-al-pomodoro', 5, 'Отварить спагетти в подсоленной воде до состояния al dente.'),
    ('spaghetti-al-pomodoro', 6, 'Прогреть 500 г соуса в широкой сковороде и переложить в него готовые спагетти.'),
    ('spaghetti-al-pomodoro', 7, 'Добавить немного свежего оливкового масла, быстро перемешать и завершить оставшимся базиликом.'),

    ('classic-ratatouille', 1, 'Нарезать баклажаны и кабачки кусочками около 2,5 см, сладкий перец очистить от семян и нарезать кубиками.'),
    ('classic-ratatouille', 2, 'Надрезать помидоры крест-накрест, залить кипятком на минуту, снять кожицу, удалить семена и крупно нарезать мякоть.'),
    ('classic-ratatouille', 3, 'На слабом огне готовить лук в оливковом масле 8–10 минут. Добавить баклажаны и кабачки, увеличить нагрев и готовить 2–3 минуты.'),
    ('classic-ratatouille', 4, 'Добавить перец, чеснок, сахар, соль, чёрный перец и половину базилика. Накрыть и томить 20 минут на очень слабом огне.'),
    ('classic-ratatouille', 5, 'Добавить помидоры, готовить ещё 10 минут и перед подачей посыпать оставшимся базиликом.'),

    ('gratin-dauphinois', 1, 'Разогреть духовку с конвекцией до 175 °C. Картофель очистить и нарезать кружками толщиной 3–6 мм.'),
    ('gratin-dauphinois', 2, 'Разрезать чеснок вдоль, натереть им форму и смазать форму половиной сливочного масла.'),
    ('gratin-dauphinois', 3, 'Смешать сливки с молоком. Выложить половину картофеля, влить половину смеси, добавить соль, перец и 3 щепотки мускатного ореха.'),
    ('gratin-dauphinois', 4, 'Повторить слои, разложить сверху оставшееся масло и запекать 90 минут до мягкости картофеля и лёгкой золотистой корочки.'),

    ('georgian-lobio', 1, 'Промыть фасоль, замочить на ночь в холодной воде и утром снова промыть.'),
    ('georgian-lobio', 2, 'Залить свежей водой, довести до первого кипения, слить воду и снова залить холодной.'),
    ('georgian-lobio', 3, 'Варить на среднем огне до полной мягкости, при необходимости подливая холодную воду.'),
    ('georgian-lobio', 4, 'Слить отвар в отдельную ёмкость и тщательно размять фасоль.'),
    ('georgian-lobio', 5, 'Добавить мелко нарезанные лук, чеснок, петрушку, сельдерей, кинзу, соль и перец. Вернуть столько отвара, сколько нужно для желаемой густоты.'),
    ('georgian-lobio', 6, 'Довести до кипения и томить под крышкой 10–15 минут. Добавить ткемали, ещё раз вскипятить и снять с огня.'),

    ('georgian-chakhokhbili', 1, 'Разделать жирную курицу так, чтобы на каждом куске сохранилась кожа, удерживающая сок.'),
    ('georgian-chakhokhbili', 2, 'Положить курицу в толстодонную посуду без воды и масла, накрыть и готовить 30 минут на очень слабом огне.'),
    ('georgian-chakhokhbili', 3, 'Когда вытопится жир, добавить нарезанный лук и перцы. Обжарить овощи до мягкости, затем снять крышку и готовить ещё 5 минут.'),
    ('georgian-chakhokhbili', 4, 'Добавить очищенные и натёртые или измельчённые помидоры. Сока должно быть достаточно, чтобы покрыть курицу.'),
    ('georgian-chakhokhbili', 5, 'Добавить растёртые кинзу, чеснок, перец и соль. Накрыть и готовить ещё 15–20 минут.'),

    ('chinese-tomato-and-egg', 1, 'Нарезать помидоры небольшими дольками и мелко порубить зелёный лук.'),
    ('chinese-tomato-and-egg', 2, 'Взбивать яйца минуту с 1/4 чайной ложки соли, белым перцем, кунжутным маслом и шаосинским вином.'),
    ('chinese-tomato-and-egg', 3, 'Разогреть сковороду до лёгкого дымка, добавить 2 столовые ложки масла. Быстро приготовить мягкие крупные кусочки яйца и сразу переложить их.'),
    ('chinese-tomato-and-egg', 4, 'Добавить оставшееся масло, увеличить нагрев и минуту обжаривать помидоры с зелёным луком.'),
    ('chinese-tomato-and-egg', 5, 'Добавить сахар, оставшуюся соль и воду, вернуть яйца. Накрыть и готовить 1–2 минуты до размягчения помидоров.'),
    ('chinese-tomato-and-egg', 6, 'Снять крышку и готовить на сильном огне до желаемой густоты соуса.'),

    ('chinese-egg-fried-rice', 1, 'Взбить яйца, тонко нарезать зелёный лук и разбить комочки в полностью остывшем рисе.'),
    ('chinese-egg-fried-rice', 2, 'Сильно разогреть масло в большой сковороде. Влить яйца и подождать 15–20 секунд, пока низ схватится, а верх останется немного жидким.'),
    ('chinese-egg-fried-rice', 3, 'Выложить холодный рис прямо на жидкую часть яйца. Лопаткой разделять рис и яйцо, пока зёрна не разойдутся.'),
    ('chinese-egg-fried-rice', 4, 'Добавить соль и зелёный лук. Прижимать рис ровным слоем к сковороде на 30–45 секунд, перемешивать и повторять 2–3 минуты до лёгкой золотистости.'),
    ('chinese-egg-fried-rice', 5, 'Выключить нагрев. При использовании нейтрального масла добавить кунжутное масло и перемешать; дать рису постоять в горячей сковороде минуту.'),

    ('classic-shakshuka', 1, 'Разогреть оливковое масло в широкой глубокой сковороде. Добавить чеснок и нарезанный зелёный перец, готовить 30 секунд.'),
    ('classic-shakshuka', 2, 'Добавить четвертинки спелых помидоров, соль и паприку. Перемешать и готовить около 15 минут.'),
    ('classic-shakshuka', 3, 'Добавить яйца и готовить 3–4 минуты.'),
    ('classic-shakshuka', 4, 'Ложкой аккуратно смешать белки с соусом, не повреждая желтки, и подать прямо в сковороде.')
) AS values_to_insert(recipe_slug, sort_order, instruction)
JOIN recipes recipe ON recipe.slug = values_to_insert.recipe_slug;

INSERT INTO recipe_equipment (recipe_id, equipment_id)
SELECT recipe.id, equipment.id
FROM (VALUES
    ('classic-syrniki', 'ELECTRIC_STOVE'),
    ('belarusian-draniki', 'ELECTRIC_STOVE'),
    ('spaghetti-aglio-e-olio', 'ELECTRIC_STOVE'),
    ('classic-frittata', 'ELECTRIC_STOVE'),
    ('spaghetti-al-pomodoro', 'ELECTRIC_STOVE'),
    ('classic-ratatouille', 'ELECTRIC_STOVE'),
    ('gratin-dauphinois', 'ELECTRIC_OVEN'),
    ('georgian-lobio', 'ELECTRIC_STOVE'),
    ('georgian-chakhokhbili', 'ELECTRIC_STOVE'),
    ('chinese-tomato-and-egg', 'ELECTRIC_STOVE'),
    ('chinese-egg-fried-rice', 'ELECTRIC_STOVE'),
    ('classic-shakshuka', 'ELECTRIC_STOVE')
) AS values_to_insert(recipe_slug, equipment_code)
JOIN recipes recipe ON recipe.slug = values_to_insert.recipe_slug
JOIN equipment equipment ON equipment.code = values_to_insert.equipment_code;

INSERT INTO recipe_cooking_methods (recipe_id, cooking_method_id)
SELECT recipe.id, cooking_method.id
FROM (VALUES
    ('classic-syrniki', 'FRYING'),
    ('belarusian-draniki', 'FRYING'),
    ('spaghetti-aglio-e-olio', 'BOILING'),
    ('spaghetti-aglio-e-olio', 'FRYING'),
    ('classic-frittata', 'FRYING'),
    ('spaghetti-al-pomodoro', 'BOILING'),
    ('spaghetti-al-pomodoro', 'STEWING'),
    ('classic-ratatouille', 'STEWING'),
    ('gratin-dauphinois', 'BAKING'),
    ('georgian-lobio', 'BOILING'),
    ('georgian-lobio', 'STEWING'),
    ('georgian-chakhokhbili', 'FRYING'),
    ('georgian-chakhokhbili', 'STEWING'),
    ('chinese-tomato-and-egg', 'FRYING'),
    ('chinese-egg-fried-rice', 'FRYING'),
    ('classic-shakshuka', 'FRYING'),
    ('classic-shakshuka', 'STEWING')
) AS values_to_insert(recipe_slug, cooking_method_code)
JOIN recipes recipe ON recipe.slug = values_to_insert.recipe_slug
JOIN cooking_methods cooking_method ON cooking_method.code = values_to_insert.cooking_method_code;

INSERT INTO ingredient_substitutions (
    recipe_ingredient_id, substitute_ingredient_id, amount, unit,
    substitution_type, impact_note, sort_order
)
SELECT recipe_ingredient.id, substitute.id, values_to_insert.amount, values_to_insert.unit,
       values_to_insert.substitution_type, values_to_insert.impact_note, values_to_insert.sort_order
FROM (VALUES
    ('classic-frittata', 'OLIVE_OIL', 'BUTTER', 10.000, 'GRAM', 'CLOSE',
     'Источник допускает готовить фриттату на небольшом количестве масла или сливочного масла.', 1),
    ('chinese-tomato-and-egg', 'SHAOXING_WINE', 'DRY_SHERRY', 1.000, 'TEASPOON', 'CLOSE',
     'Источник прямо предлагает сухой херес как замену шаосинскому вину.', 1),
    ('chinese-tomato-and-egg', 'SHAOXING_WINE', 'MIRIN', 1.000, 'TEASPOON', 'ADAPTED',
     'Мирин указан источником как допустимая замена, но он слаще, поэтому сахар можно немного уменьшить.', 2),
    ('chinese-egg-fried-rice', 'PEANUT_OIL', 'VEGETABLE_OIL', 2.000, 'TABLESPOON', 'CLOSE',
     'Источник разрешает использовать нейтральное растительное масло вместо арахисового.', 1)
) AS values_to_insert(
    recipe_slug, original_ingredient_code, substitute_ingredient_code,
    amount, unit, substitution_type, impact_note, sort_order
)
JOIN recipes recipe ON recipe.slug = values_to_insert.recipe_slug
JOIN ingredients original ON original.code = values_to_insert.original_ingredient_code
JOIN recipe_ingredients recipe_ingredient
    ON recipe_ingredient.recipe_id = recipe.id
    AND recipe_ingredient.ingredient_id = original.id
JOIN ingredients substitute ON substitute.code = values_to_insert.substitute_ingredient_code;
