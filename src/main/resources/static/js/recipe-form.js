document.addEventListener("DOMContentLoaded", () => {
	activateValidationFeedback();
	syncIngredientNameOptions();

	const configurations = {
		ingredient: {
			list: document.querySelector('[data-row-list="ingredient"]'),
			template: document.querySelector("#ingredient-row-template")
		},
		step: {
			list: document.querySelector('[data-row-list="step"]'),
			template: document.querySelector("#step-row-template")
		},
		substitution: {
			list: document.querySelector('[data-row-list="substitution"]'),
			template: document.querySelector("#substitution-row-template")
		}
	};

	document.querySelectorAll("[data-add-row]").forEach((button) => {
		button.addEventListener("click", () => {
			const type = button.dataset.addRow;
			if (type === "substitution") {
				syncIngredientNameOptions();
			}
			const configuration = configurations[type];
			const index = configuration.list.querySelectorAll("[data-row]").length;
			const html = configuration.template.innerHTML
				.replaceAll("__INDEX__", index)
				.replaceAll("__NUMBER__", index + 1);
			configuration.list.insertAdjacentHTML("beforeend", html);
		});
	});

	document.addEventListener("input", (event) => {
		if (event.target.matches('[name^="ingredients"][name$=".name"]')) {
			syncIngredientNameOptions();
		}
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
			const titles = {
				ingredient: "Ингредиент",
				step: "Шаг",
				substitution: "Замена"
			};
			row.querySelector(".row-title").textContent = `${titles[type]} ${index + 1}`;
			row.querySelectorAll("[name]").forEach((field) => {
				field.name = field.name.replace(/(ingredients|steps|substitutions)\[\d+\]/, `$1[${index}]`);
			});
		});
	}

	function activateValidationFeedback() {
		const summary = document.querySelector("[data-error-summary]");
		if (!summary) {
			return;
		}

		const errors = Array.from(document.querySelectorAll(".form-error"))
				.filter((error) => error.textContent.trim() !== "");

		errors.forEach((error) => {
			const container = error.closest(".field, .check-group")
					?? error.closest("[data-row]")
					?? error.closest(".form-section");
			container?.classList.add("validation-error");

			const row = error.closest("[data-row]");
			if (row && row !== container) {
				row.classList.add("validation-error");
			}

			const control = findFirstControl(container);
			control?.setAttribute("aria-invalid", "true");
		});

		const firstError = errors[0];
		if (!firstError) {
			summary.focus();
			return;
		}

		const firstContainer = firstError.closest(".field, .check-group, [data-row], .form-section");
		const firstControl = findFirstControl(firstContainer);
		requestAnimationFrame(() => {
			firstError.scrollIntoView({ behavior: "smooth", block: "center" });
			firstControl?.focus({ preventScroll: true });
		});
	}

	function syncIngredientNameOptions() {
		const datalist = document.querySelector("#recipe-ingredient-names");
		if (!datalist) {
			return;
		}

		const names = new Set();
		document.querySelectorAll('[name^="ingredients"][name$=".name"]').forEach((field) => {
			const name = field.value.trim();
			if (name !== "") {
				names.add(name);
			}
		});

		datalist.replaceChildren(...Array.from(names).map((name) => {
			const option = document.createElement("option");
			option.value = name;
			return option;
		}));
	}

	function findFirstControl(container) {
		return container?.querySelector("input:not([type=hidden]):not([disabled]), textarea:not([disabled]), select:not([disabled])")
				?? null;
	}
});
