INSERT INTO ingredients (code, name_ru, name_en) VALUES
    ('COTTAGE_CHEESE', 'Творог', 'Cottage cheese'),
    ('WHEAT_FLOUR', 'Пшеничная мука', 'Wheat flour'),
    ('SUGAR', 'Сахар', 'Sugar'),
    ('EGG', 'Куриное яйцо', 'Chicken egg'),
    ('SEMOLINA', 'Манная крупа', 'Semolina'),
    ('SPAGHETTI', 'Спагетти', 'Spaghetti'),
    ('OLIVE_OIL', 'Оливковое масло', 'Olive oil'),
    ('CHILI_FLAKES', 'Хлопья перца чили', 'Chili flakes'),
    ('FRESH_CHILI', 'Свежий перец чили', 'Fresh chili pepper'),
    ('PARSLEY', 'Петрушка', 'Parsley'),
    ('PARMESAN', 'Пармезан', 'Parmesan'),
    ('HARD_CHEESE', 'Твёрдый сыр', 'Hard cheese'),
    ('MILK', 'Молоко', 'Milk'),
    ('SPINACH', 'Шпинат', 'Spinach'),
    ('BLACK_PEPPER', 'Чёрный перец', 'Black pepper'),
    ('CANNED_TOMATOES', 'Консервированные томаты', 'Canned tomatoes'),
    ('TOMATO', 'Помидор', 'Tomato'),
    ('BASIL', 'Базилик', 'Basil'),
    ('EGGPLANT', 'Баклажан', 'Eggplant'),
    ('ZUCCHINI', 'Кабачок', 'Zucchini'),
    ('HERBS_DE_PROVENCE', 'Прованские травы', 'Herbes de Provence'),
    ('CREAM', 'Сливки', 'Cream'),
    ('BUTTER', 'Сливочное масло', 'Butter'),
    ('NUTMEG', 'Мускатный орех', 'Nutmeg'),
    ('RED_BEANS_DRY', 'Красная фасоль сухая', 'Dried red beans'),
    ('CANNED_RED_BEANS', 'Красная фасоль консервированная', 'Canned red beans'),
    ('CILANTRO', 'Кинза', 'Cilantro'),
    ('WALNUT', 'Грецкий орех', 'Walnut'),
    ('WINE_VINEGAR', 'Винный уксус', 'Wine vinegar'),
    ('KHMELI_SUNELI', 'Хмели-сунели', 'Khmeli suneli'),
    ('CHICKEN_THIGH', 'Куриное бедро', 'Chicken thigh'),
    ('GREEN_ONION', 'Зелёный лук', 'Green onion'),
    ('SOY_SAUCE', 'Соевый соус', 'Soy sauce'),
    ('CUMIN', 'Зира', 'Cumin')
ON CONFLICT (name_ru) DO UPDATE
SET code = EXCLUDED.code,
    name_en = COALESCE(ingredients.name_en, EXCLUDED.name_en);

INSERT INTO recipes (
    slug, name_ru, name_en, description, servings, preparation_minutes,
    cooking_minutes, difficulty, status, source_name, source_url,
    personal_notes, cuisine_id, origin
)
SELECT
    values_to_insert.slug,
    values_to_insert.name_ru,
    values_to_insert.name_en,
    values_to_insert.description,
    values_to_insert.servings,
    values_to_insert.preparation_minutes,
    values_to_insert.cooking_minutes,
    values_to_insert.difficulty,
    'UNTESTED',
    values_to_insert.source_name,
    values_to_insert.source_url,
    'Количество продуктов и время приготовления стоит уточнить после личной проверки.',
    cuisine.id,
    'CLASSIC'
FROM (VALUES
    (
        'classic-syrniki',
        'Классические сырники',
        'Classic syrniki',
        'Нежные творожные сырники с тонкой румяной корочкой.',
        4, 15, 20, 'EASY',
        'Московское долголетие — классические сырники',
        'https://msp.dszn.ru/recipes/syrniki-klassicheskie',
        'EAST_SLAVIC'
    ),
    (
        'belarusian-draniki',
        'Белорусские драники',
        'Belarusian draniki',
        'Картофельные оладьи с хрустящими краями и мягкой серединой.',
        4, 20, 25, 'EASY',
        'JDC — белорусские драники',
        'https://www.jdc.org/learn/recipe/darias-draniki-belarusian-latkes/',
        'BELARUSIAN'
    ),
    (
        'spaghetti-aglio-e-olio',
        'Спагетти aglio e olio',
        'Spaghetti aglio e olio',
        'Простая итальянская паста с чесноком, оливковым маслом и лёгкой остротой.',
        2, 10, 15, 'EASY',
        'La Cucina Italiana — spaghetti aglio, olio e peperoncino',
        'https://www.lacucinaitaliana.it/ricetta/primi/spaghetti-aglio-olio-e-peperoncino-2/',
        'ITALIAN'
    ),
    (
        'spinach-frittata',
        'Фриттата со шпинатом',
        'Spinach frittata',
        'Итальянский омлет со шпинатом и сыром, доведённый до готовности в духовке.',
        4, 10, 20, 'EASY',
        'La Cucina Italiana — фриттата',
        'https://www.lacucinaitaliana.it/ricetta/secondi/frittata-2/',
        'ITALIAN'
    ),
    (
        'spaghetti-al-pomodoro',
        'Спагетти с томатным соусом',
        'Spaghetti al pomodoro',
        'Классическая паста с насыщенным томатным соусом и свежим базиликом.',
        4, 10, 30, 'EASY',
        'La Cucina Italiana — spaghetti al pomodoro',
        'https://www.lacucinaitaliana.it/ricetta/primi/spaghetti-al-pomodoro/',
        'ITALIAN'
    ),
    (
        'classic-ratatouille',
        'Рататуй',
        'Ratatouille',
        'Овощное рагу из баклажана, кабачка, перца и помидоров с прованскими травами.',
        4, 20, 45, 'MEDIUM',
        'France.fr — рататуй',
        'https://www.france.fr/en/article/taste-provence-this-weekend-and-make-a-ratatouille/',
        'FRENCH'
    ),
    (
        'gratin-dauphinois',
        'Гратен дофинуа',
        'Gratin dauphinois',
        'Тонкие ломтики картофеля, запечённые в молоке и сливках без сырной корочки.',
        6, 20, 100, 'MEDIUM',
        'Taste France — gratin dauphinois',
        'https://www.tastefrance.com/recipes/french-classics/gratin-dauphinois',
        'FRENCH'
    ),
    (
        'georgian-lobio',
        'Лобио',
        'Georgian lobio',
        'Густое грузинское блюдо из красной фасоли с орехами, зеленью и специями.',
        4, 20, 110, 'MEDIUM',
        'Georgia Travel — лобио',
        'https://georgia.travel/lobio-bean-stew',
        'GEORGIAN'
    ),
    (
        'georgian-chakhokhbili',
        'Чахохбили',
        'Georgian chakhokhbili',
        'Курица, тушённая с большим количеством лука, томатами, зеленью и специями.',
        4, 20, 60, 'MEDIUM',
        'Georgia Travel — чахохбили',
        'https://georgia.travel/chakhokhbili',
        'GEORGIAN'
    ),
    (
        'chinese-tomato-and-egg',
        'Помидоры с яйцом по-китайски',
        'Chinese tomato and egg',
        'Популярное домашнее китайское блюдо из мягких яиц и сочных помидоров.',
        2, 10, 10, 'EASY',
        'The Woks of Life — stir-fried tomato and egg',
        'https://thewoksoflife.com/stir-fried-tomato-and-egg/',
        'CHINESE_HOME'
    ),
    (
        'chinese-egg-fried-rice',
        'Жареный рис с яйцом',
        'Egg fried rice',
        'Быстрое домашнее блюдо из охлаждённого риса, яйца и зелёного лука.',
        2, 10, 10, 'EASY',
        'Omnivore''s Cookbook — egg fried rice',
        'https://omnivorescookbook.com/egg-fried-rice/',
        'CHINESE_HOME'
    ),
    (
        'classic-shakshuka',
        'Шакшука',
        'Classic shakshuka',
        'Яйца, приготовленные в пряном томатном соусе со сладким перцем.',
        4, 15, 30, 'EASY',
        'Foodish — классическая шакшука',
        'https://foodish.anumuseum.org.il/en/recipe/classic-shakshouka/',
        'NORTH_AFRICAN_MIDDLE_EASTERN'
    )
) AS values_to_insert(
    slug, name_ru, name_en, description, servings, preparation_minutes,
    cooking_minutes, difficulty, source_name, source_url, cuisine_code
)
JOIN cuisines cuisine ON cuisine.code = values_to_insert.cuisine_code;

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, amount, unit, optional, sort_order)
SELECT recipe.id, ingredient.id, values_to_insert.amount, values_to_insert.unit,
       values_to_insert.optional, values_to_insert.sort_order
FROM (VALUES
    ('classic-syrniki', 'COTTAGE_CHEESE', 500.000, 'GRAM', FALSE, 1),
    ('classic-syrniki', 'EGG', 1.000, 'PIECE', FALSE, 2),
    ('classic-syrniki', 'WHEAT_FLOUR', 80.000, 'GRAM', FALSE, 3),
    ('classic-syrniki', 'SUGAR', 2.000, 'TABLESPOON', FALSE, 4),
    ('classic-syrniki', 'SALT', 1.000, 'PINCH', FALSE, 5),
    ('classic-syrniki', 'VEGETABLE_OIL', 2.000, 'TABLESPOON', FALSE, 6),

    ('belarusian-draniki', 'POTATO', 800.000, 'GRAM', FALSE, 1),
    ('belarusian-draniki', 'ONION', 1.000, 'PIECE', FALSE, 2),
    ('belarusian-draniki', 'EGG', 1.000, 'PIECE', FALSE, 3),
    ('belarusian-draniki', 'WHEAT_FLOUR', 2.000, 'TABLESPOON', FALSE, 4),
    ('belarusian-draniki', 'SALT', 1.000, 'TEASPOON', FALSE, 5),
    ('belarusian-draniki', 'VEGETABLE_OIL', 3.000, 'TABLESPOON', FALSE, 6),

    ('spaghetti-aglio-e-olio', 'SPAGHETTI', 200.000, 'GRAM', FALSE, 1),
    ('spaghetti-aglio-e-olio', 'GARLIC', 4.000, 'CLOVE', FALSE, 2),
    ('spaghetti-aglio-e-olio', 'OLIVE_OIL', 4.000, 'TABLESPOON', FALSE, 3),
    ('spaghetti-aglio-e-olio', 'CHILI_FLAKES', 0.500, 'TEASPOON', FALSE, 4),
    ('spaghetti-aglio-e-olio', 'PARSLEY', 0.500, 'BUNCH', TRUE, 5),
    ('spaghetti-aglio-e-olio', 'SALT', 1.000, 'TEASPOON', FALSE, 6),

    ('spinach-frittata', 'EGG', 6.000, 'PIECE', FALSE, 1),
    ('spinach-frittata', 'SPINACH', 150.000, 'GRAM', FALSE, 2),
    ('spinach-frittata', 'PARMESAN', 60.000, 'GRAM', FALSE, 3),
    ('spinach-frittata', 'MILK', 60.000, 'MILLILITER', FALSE, 4),
    ('spinach-frittata', 'OLIVE_OIL', 1.000, 'TABLESPOON', FALSE, 5),
    ('spinach-frittata', 'SALT', 0.500, 'TEASPOON', FALSE, 6),
    ('spinach-frittata', 'BLACK_PEPPER', 2.000, 'PINCH', TRUE, 7),

    ('spaghetti-al-pomodoro', 'SPAGHETTI', 320.000, 'GRAM', FALSE, 1),
    ('spaghetti-al-pomodoro', 'CANNED_TOMATOES', 800.000, 'GRAM', FALSE, 2),
    ('spaghetti-al-pomodoro', 'GARLIC', 2.000, 'CLOVE', FALSE, 3),
    ('spaghetti-al-pomodoro', 'OLIVE_OIL', 3.000, 'TABLESPOON', FALSE, 4),
    ('spaghetti-al-pomodoro', 'BASIL', 0.500, 'BUNCH', FALSE, 5),
    ('spaghetti-al-pomodoro', 'SALT', 1.000, 'TEASPOON', FALSE, 6),

    ('classic-ratatouille', 'EGGPLANT', 1.000, 'PIECE', FALSE, 1),
    ('classic-ratatouille', 'ZUCCHINI', 2.000, 'PIECE', FALSE, 2),
    ('classic-ratatouille', 'BELL_PEPPER', 2.000, 'PIECE', FALSE, 3),
    ('classic-ratatouille', 'TOMATO', 4.000, 'PIECE', FALSE, 4),
    ('classic-ratatouille', 'ONION', 1.000, 'PIECE', FALSE, 5),
    ('classic-ratatouille', 'GARLIC', 3.000, 'CLOVE', FALSE, 6),
    ('classic-ratatouille', 'OLIVE_OIL', 3.000, 'TABLESPOON', FALSE, 7),
    ('classic-ratatouille', 'HERBS_DE_PROVENCE', 1.000, 'TEASPOON', FALSE, 8),
    ('classic-ratatouille', 'SALT', 1.000, 'TEASPOON', FALSE, 9),

    ('gratin-dauphinois', 'POTATO', 1.000, 'KILOGRAM', FALSE, 1),
    ('gratin-dauphinois', 'MILK', 500.000, 'MILLILITER', FALSE, 2),
    ('gratin-dauphinois', 'CREAM', 250.000, 'MILLILITER', FALSE, 3),
    ('gratin-dauphinois', 'GARLIC', 1.000, 'CLOVE', FALSE, 4),
    ('gratin-dauphinois', 'BUTTER', 20.000, 'GRAM', FALSE, 5),
    ('gratin-dauphinois', 'NUTMEG', 2.000, 'PINCH', FALSE, 6),
    ('gratin-dauphinois', 'SALT', 1.000, 'TEASPOON', FALSE, 7),
    ('gratin-dauphinois', 'BLACK_PEPPER', 2.000, 'PINCH', TRUE, 8),

    ('georgian-lobio', 'RED_BEANS_DRY', 400.000, 'GRAM', FALSE, 1),
    ('georgian-lobio', 'ONION', 2.000, 'PIECE', FALSE, 2),
    ('georgian-lobio', 'GARLIC', 3.000, 'CLOVE', FALSE, 3),
    ('georgian-lobio', 'CILANTRO', 1.000, 'BUNCH', FALSE, 4),
    ('georgian-lobio', 'WALNUT', 50.000, 'GRAM', FALSE, 5),
    ('georgian-lobio', 'WINE_VINEGAR', 1.000, 'TABLESPOON', FALSE, 6),
    ('georgian-lobio', 'KHMELI_SUNELI', 1.000, 'TEASPOON', FALSE, 7),
    ('georgian-lobio', 'SALT', 1.000, 'TEASPOON', FALSE, 8),

    ('georgian-chakhokhbili', 'CHICKEN_THIGH', 1.000, 'KILOGRAM', FALSE, 1),
    ('georgian-chakhokhbili', 'ONION', 3.000, 'PIECE', FALSE, 2),
    ('georgian-chakhokhbili', 'TOMATO', 600.000, 'GRAM', FALSE, 3),
    ('georgian-chakhokhbili', 'GARLIC', 3.000, 'CLOVE', FALSE, 4),
    ('georgian-chakhokhbili', 'CILANTRO', 1.000, 'BUNCH', FALSE, 5),
    ('georgian-chakhokhbili', 'BASIL', 0.500, 'BUNCH', FALSE, 6),
    ('georgian-chakhokhbili', 'KHMELI_SUNELI', 1.000, 'TEASPOON', FALSE, 7),
    ('georgian-chakhokhbili', 'CHILI_FLAKES', 0.500, 'TEASPOON', TRUE, 8),
    ('georgian-chakhokhbili', 'SALT', 1.000, 'TEASPOON', FALSE, 9),

    ('chinese-tomato-and-egg', 'EGG', 4.000, 'PIECE', FALSE, 1),
    ('chinese-tomato-and-egg', 'TOMATO', 3.000, 'PIECE', FALSE, 2),
    ('chinese-tomato-and-egg', 'GREEN_ONION', 0.500, 'BUNCH', FALSE, 3),
    ('chinese-tomato-and-egg', 'VEGETABLE_OIL', 1.000, 'TABLESPOON', FALSE, 4),
    ('chinese-tomato-and-egg', 'SUGAR', 1.000, 'TEASPOON', FALSE, 5),
    ('chinese-tomato-and-egg', 'SALT', 0.500, 'TEASPOON', FALSE, 6),

    ('chinese-egg-fried-rice', 'RICE', 400.000, 'GRAM', FALSE, 1),
    ('chinese-egg-fried-rice', 'EGG', 3.000, 'PIECE', FALSE, 2),
    ('chinese-egg-fried-rice', 'GREEN_ONION', 0.500, 'BUNCH', FALSE, 3),
    ('chinese-egg-fried-rice', 'VEGETABLE_OIL', 2.000, 'TABLESPOON', FALSE, 4),
    ('chinese-egg-fried-rice', 'SOY_SAUCE', 1.000, 'TABLESPOON', TRUE, 5),
    ('chinese-egg-fried-rice', 'SALT', 0.500, 'TEASPOON', FALSE, 6),

    ('classic-shakshuka', 'EGG', 6.000, 'PIECE', FALSE, 1),
    ('classic-shakshuka', 'CANNED_TOMATOES', 800.000, 'GRAM', FALSE, 2),
    ('classic-shakshuka', 'BELL_PEPPER', 1.000, 'PIECE', FALSE, 3),
    ('classic-shakshuka', 'ONION', 1.000, 'PIECE', FALSE, 4),
    ('classic-shakshuka', 'GARLIC', 3.000, 'CLOVE', FALSE, 5),
    ('classic-shakshuka', 'OLIVE_OIL', 2.000, 'TABLESPOON', FALSE, 6),
    ('classic-shakshuka', 'PAPRIKA', 1.000, 'TEASPOON', FALSE, 7),
    ('classic-shakshuka', 'CUMIN', 1.000, 'TEASPOON', FALSE, 8),
    ('classic-shakshuka', 'CHILI_FLAKES', 0.500, 'TEASPOON', TRUE, 9),
    ('classic-shakshuka', 'SALT', 1.000, 'TEASPOON', FALSE, 10)
) AS values_to_insert(recipe_slug, ingredient_code, amount, unit, optional, sort_order)
JOIN recipes recipe ON recipe.slug = values_to_insert.recipe_slug
JOIN ingredients ingredient ON ingredient.code = values_to_insert.ingredient_code;

INSERT INTO recipe_steps (recipe_id, sort_order, instruction)
SELECT recipe.id, values_to_insert.sort_order, values_to_insert.instruction
FROM (VALUES
    ('classic-syrniki', 1, 'Размять творог вилкой. Добавить яйцо, сахар и соль, затем перемешать до однородности.'),
    ('classic-syrniki', 2, 'Всыпать муку и ещё раз перемешать. Масса должна держать форму, но оставаться мягкой.'),
    ('classic-syrniki', 3, 'Сформировать влажными руками небольшие сырники и слегка обвалять их в муке.'),
    ('classic-syrniki', 4, 'Разогреть масло на среднем огне. Жарить сырники по 3–4 минуты с каждой стороны до румяной корочки.'),
    ('classic-syrniki', 5, 'Убавить нагрев, накрыть крышкой и готовить ещё 3–5 минут, чтобы середина прогрелась.'),

    ('belarusian-draniki', 1, 'Очистить картофель и лук. Натереть их на мелкой тёрке и перемешать.'),
    ('belarusian-draniki', 2, 'Если масса слишком жидкая, слить часть сока. Добавить яйцо, муку и соль.'),
    ('belarusian-draniki', 3, 'Разогреть растительное масло и выкладывать картофельную массу тонкими лепёшками.'),
    ('belarusian-draniki', 4, 'Жарить на среднем огне по 3–4 минуты с каждой стороны до золотистой корочки.'),

    ('spaghetti-aglio-e-olio', 1, 'Отварить спагетти в подсоленной воде до состояния al dente. Сохранить около 100 мл воды от пасты.'),
    ('spaghetti-aglio-e-olio', 2, 'Тонко нарезать чеснок. На слабом огне прогреть его в оливковом масле, не допуская потемнения.'),
    ('spaghetti-aglio-e-olio', 3, 'Добавить хлопья чили, спагетти и несколько ложек воды от варки.'),
    ('spaghetti-aglio-e-olio', 4, 'Энергично перемешать до образования лёгкой эмульсии. По желанию добавить петрушку.'),

    ('spinach-frittata', 1, 'Разогреть духовку до 180 °C. Яйца взбить с молоком, солью и перцем.'),
    ('spinach-frittata', 2, 'На подходящей для духовки сковороде прогреть масло и припустить шпинат 2–3 минуты.'),
    ('spinach-frittata', 3, 'Залить шпинат яичной смесью, посыпать тёртым пармезаном и готовить на слабом огне 3 минуты.'),
    ('spinach-frittata', 4, 'Переставить сковороду в духовку и запекать 10–12 минут, пока середина не схватится.'),

    ('spaghetti-al-pomodoro', 1, 'Слегка раздавить чеснок и прогреть его в оливковом масле до появления аромата.'),
    ('spaghetti-al-pomodoro', 2, 'Добавить томаты, соль и половину базилика. Томить соус на слабом огне около 20 минут.'),
    ('spaghetti-al-pomodoro', 3, 'Отварить спагетти до состояния al dente, сохранив немного воды от варки.'),
    ('spaghetti-al-pomodoro', 4, 'Переложить пасту в соус, перемешать и прогреть 1–2 минуты. Добавить оставшийся базилик.'),

    ('classic-ratatouille', 1, 'Нарезать баклажан, кабачки, перец и помидоры одинаковыми кубиками. Лук и чеснок измельчить.'),
    ('classic-ratatouille', 2, 'На части масла отдельно обжарить баклажан и кабачки до лёгкой румяности, затем отложить.'),
    ('classic-ratatouille', 3, 'В той же посуде размягчить лук и перец, добавить чеснок, помидоры и прованские травы.'),
    ('classic-ratatouille', 4, 'Вернуть баклажан и кабачки, посолить и тушить под крышкой 25–30 минут на слабом огне.'),
    ('classic-ratatouille', 5, 'Снять крышку и готовить ещё 5 минут, если нужно выпарить лишнюю жидкость.'),

    ('gratin-dauphinois', 1, 'Разогреть духовку до 160 °C. Очистить картофель и нарезать очень тонкими кружками, не промывая их.'),
    ('gratin-dauphinois', 2, 'Натереть форму разрезанным зубчиком чеснока и смазать сливочным маслом.'),
    ('gratin-dauphinois', 3, 'Прогреть молоко со сливками, солью, перцем и мускатным орехом почти до кипения.'),
    ('gratin-dauphinois', 4, 'Выложить картофель слоями, залить горячей молочно-сливочной смесью.'),
    ('gratin-dauphinois', 5, 'Запекать около 90 минут до полной мягкости картофеля и золотистой поверхности.'),

    ('georgian-lobio', 1, 'Замочить фасоль в холодной воде на 8–12 часов, затем промыть.'),
    ('georgian-lobio', 2, 'Залить свежей водой и варить до мягкости 70–90 минут. Часть фасоли слегка размять.'),
    ('georgian-lobio', 3, 'Отдельно обжарить лук до мягкости. Добавить чеснок, хмели-сунели и измельчённые орехи.'),
    ('georgian-lobio', 4, 'Соединить заправку с фасолью и небольшим количеством отвара. Тушить 10–15 минут.'),
    ('georgian-lobio', 5, 'Добавить уксус, соль и нарезанную кинзу. Дать блюду настояться 10 минут.'),

    ('georgian-chakhokhbili', 1, 'Куриные бёдра обсушить, разделить на удобные куски и слегка посолить.'),
    ('georgian-chakhokhbili', 2, 'Обжарить курицу в сухой глубокой сковороде до лёгкой корочки, затем временно переложить.'),
    ('georgian-chakhokhbili', 3, 'В вытопившемся жире размягчить нарезанный лук. Добавить помидоры и тушить 10 минут.'),
    ('georgian-chakhokhbili', 4, 'Вернуть курицу, добавить хмели-сунели и чили. Тушить под крышкой 35–40 минут.'),
    ('georgian-chakhokhbili', 5, 'Добавить чеснок, кинзу и базилик. Прогреть 2 минуты и дать настояться под крышкой.'),

    ('chinese-tomato-and-egg', 1, 'Яйца взбить с щепоткой соли. Помидоры нарезать крупными дольками, зелёный лук измельчить.'),
    ('chinese-tomato-and-egg', 2, 'На хорошо разогретом масле быстро приготовить мягкие крупные кусочки омлета и переложить их на тарелку.'),
    ('chinese-tomato-and-egg', 3, 'В той же сковороде готовить помидоры с сахаром и солью 3–4 минуты, пока они не дадут сок.'),
    ('chinese-tomato-and-egg', 4, 'Вернуть яйца, аккуратно перемешать с соусом и прогреть около минуты. Посыпать зелёным луком.'),

    ('chinese-egg-fried-rice', 1, 'Использовать полностью остывший рис и разбить комочки. Яйца взбить, зелёный лук нарезать.'),
    ('chinese-egg-fried-rice', 2, 'Разогреть половину масла, быстро приготовить яйца и разбить их лопаткой на небольшие кусочки.'),
    ('chinese-egg-fried-rice', 3, 'Добавить оставшееся масло и рис. Жарить 3–4 минуты, постоянно перемешивая.'),
    ('chinese-egg-fried-rice', 4, 'Добавить зелёный лук, соль и по желанию соевый соус. Перемешать и прогреть ещё минуту.'),

    ('classic-shakshuka', 1, 'Лук и сладкий перец нарезать кубиками. Обжаривать в оливковом масле 6–8 минут до мягкости.'),
    ('classic-shakshuka', 2, 'Добавить чеснок, паприку, зиру и чили. Перемешивать около 30 секунд.'),
    ('classic-shakshuka', 3, 'Добавить томаты и соль. Тушить без крышки 15–20 минут до загустения соуса.'),
    ('classic-shakshuka', 4, 'Сделать в соусе углубления, разбить в них яйца и накрыть сковороду крышкой.'),
    ('classic-shakshuka', 5, 'Готовить 5–8 минут: белок должен схватиться, а желток остаться мягким.' )
) AS values_to_insert(recipe_slug, sort_order, instruction)
JOIN recipes recipe ON recipe.slug = values_to_insert.recipe_slug;

INSERT INTO recipe_equipment (recipe_id, equipment_id)
SELECT recipe.id, equipment.id
FROM (VALUES
    ('classic-syrniki', 'ELECTRIC_STOVE'),
    ('belarusian-draniki', 'ELECTRIC_STOVE'),
    ('spaghetti-aglio-e-olio', 'ELECTRIC_STOVE'),
    ('spinach-frittata', 'ELECTRIC_STOVE'),
    ('spinach-frittata', 'ELECTRIC_OVEN'),
    ('spaghetti-al-pomodoro', 'ELECTRIC_STOVE'),
    ('classic-ratatouille', 'ELECTRIC_STOVE'),
    ('gratin-dauphinois', 'ELECTRIC_STOVE'),
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
    ('spinach-frittata', 'FRYING'),
    ('spinach-frittata', 'BAKING'),
    ('spaghetti-al-pomodoro', 'BOILING'),
    ('spaghetti-al-pomodoro', 'STEWING'),
    ('classic-ratatouille', 'FRYING'),
    ('classic-ratatouille', 'STEWING'),
    ('gratin-dauphinois', 'BOILING'),
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
    ('classic-syrniki', 'WHEAT_FLOUR', 'SEMOLINA', 60.000, 'GRAM', 'ADAPTED',
     'Сырники станут плотнее. После добавления манки оставь массу на 15 минут для набухания.', 1),
    ('spaghetti-aglio-e-olio', 'CHILI_FLAKES', 'FRESH_CHILI', 0.500, 'PIECE', 'CLOSE',
     'Острота свежего перца различается: начни с меньшего количества и попробуй соус.', 1),
    ('spinach-frittata', 'PARMESAN', 'HARD_CHEESE', 60.000, 'GRAM', 'CLOSE',
     'Подойдёт выдержанный твёрдый сыр, но вкус получится менее характерным.', 1),
    ('spaghetti-al-pomodoro', 'CANNED_TOMATOES', 'TOMATO', 1.000, 'KILOGRAM', 'ADAPTED',
     'Свежие помидоры лучше очистить от кожицы, а соус готовить немного дольше.', 1),
    ('georgian-lobio', 'RED_BEANS_DRY', 'CANNED_RED_BEANS', 800.000, 'GRAM', 'ADAPTED',
     'Промой консервированную фасоль и сократи тушение: текстура будет мягче, а вкус — менее насыщенным.', 1),
    ('georgian-chakhokhbili', 'CILANTRO', 'PARSLEY', 1.000, 'BUNCH', 'ADAPTED',
     'Петрушка смягчит характерный аромат блюда, но сохранит свежесть зелени.', 1),
    ('chinese-egg-fried-rice', 'GREEN_ONION', 'ONION', 0.500, 'PIECE', 'ADAPTED',
     'Репчатый лук обжарь до добавления риса; вкус будет слаще и менее свежим.', 1),
    ('classic-shakshuka', 'CANNED_TOMATOES', 'TOMATO', 6.000, 'PIECE', 'ADAPTED',
     'Спелые свежие помидоры дадут больше жидкости, поэтому соус придётся выпаривать дольше.', 1)
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
