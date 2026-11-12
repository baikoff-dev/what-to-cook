document.addEventListener("DOMContentLoaded", () => {
	const configurations = {
		ingredient: {
			list: document.querySelector('[data-row-list="ingredient"]'),
			template: document.querySelector("#ingredient-row-template")
		},
		step: {
			list: document.querySelector('[data-row-list="step"]'),
			template: document.querySelector("#step-row-template")
		}
	};

	document.querySelectorAll("[data-add-row]").forEach((button) => {
		button.addEventListener("click", () => {
			const type = button.dataset.addRow;
			const configuration = configurations[type];
			const index = configuration.list.querySelectorAll("[data-row]").length;
			const html = configuration.template.innerHTML
				.replaceAll("__INDEX__", index)
				.replaceAll("__NUMBER__", index + 1);
			configuration.list.insertAdjacentHTML("beforeend", html);
		});
	});

	document.addEventListener("click", (event) => {
		const button = event.target.closest("[data-remove-row]");
		if (!button) {
			return;
		}

		const list = button.closest("[data-row-list]");
		const rows = list.querySelectorAll("[data-row]");
		if (rows.length === 1) {
			rows[0].querySelectorAll("input:not([type=hidden]), textarea, select").forEach((field) => {
				if (field.type === "checkbox") {
					field.checked = false;
				} else {
					field.value = "";
				}
			});
			return;
		}

		button.closest("[data-row]").remove();
		reindex(list);
	});

	function reindex(list) {
		const type = list.dataset.rowList;
		list.querySelectorAll("[data-row]").forEach((row, index) => {
			row.querySelector(".row-title").textContent = `${type === "step" ? "Шаг" : "Ингредиент"} ${index + 1}`;
			row.querySelectorAll("[name]").forEach((field) => {
				field.name = field.name.replace(/(ingredients|steps)\[\d+\]/, `$1[${index}]`);
			});
		});
	}
});
