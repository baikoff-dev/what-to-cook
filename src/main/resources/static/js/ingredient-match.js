const filterInput = document.querySelector("[data-ingredient-filter]");
const ingredientList = document.querySelector("[data-ingredient-list]");
const selectedCount = document.querySelector("[data-selected-count]");

if (filterInput && ingredientList) {
	const choices = [...ingredientList.querySelectorAll(".ingredient-choice")];

	const updateSelectedCount = () => {
		if (!selectedCount) {
			return;
		}
		const count = choices.filter((choice) => choice.querySelector("input").checked).length;
		selectedCount.textContent = `Выбрано продуктов: ${count}`;
	};

	filterInput.addEventListener("input", () => {
		const query = filterInput.value.trim().toLocaleLowerCase("ru-RU");
		choices.forEach((choice) => {
			choice.hidden = !choice.textContent.toLocaleLowerCase("ru-RU").includes(query);
		});
	});

	ingredientList.addEventListener("change", updateSelectedCount);
	updateSelectedCount();
}
