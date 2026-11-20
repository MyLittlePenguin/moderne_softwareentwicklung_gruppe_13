(() => {
	const ISO_MUSTER = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/;
	const DE_MUSTER = /^(\d{2})\.(\d{2})\.(\d{4})\s+(\d{2}):(\d{2})$/;

	function isoZuDeutsch(isoWert) {
		const treffer = ISO_MUSTER.exec(isoWert);
		if (!treffer) {
			return null;
		}
		const [, jahr, monat, tag, stunde, minute] = treffer;
		return `${tag}.${monat}.${jahr} ${stunde}:${minute}`;
	}

	function deutschZuIso(deutschWert) {
		const treffer = DE_MUSTER.exec(deutschWert.trim());
		if (!treffer) {
			return "";
		}
		const [, tag, monat, jahr, stunde, minute] = treffer;
		const j = Number(jahr);
		const m = Number(monat);
		const t = Number(tag);
		const h = Number(stunde);
		const min = Number(minute);
		if (m < 1 || m > 12 || t < 1 || t > 31 || h > 23 || min > 59) {
			return "";
		}
		const datum = new Date(j, m - 1, t, h, min);
		if (
			datum.getFullYear() !== j ||
			datum.getMonth() !== m - 1 ||
			datum.getDate() !== t
		) {
			return "";
		}
		return `${jahr}-${monat}-${tag}T${stunde}:${minute}`;
	}

	function synchronisiereTextZuPicker(textFeld) {
		const picker = document.querySelector(`input[data-datum-ziel="${textFeld.id}"]`);
		if (!picker) {
			return;
		}
		picker.value = deutschZuIso(textFeld.value);
	}

	function synchronisiereAllePicker() {
		document.querySelectorAll("input[data-datum-ziel]").forEach((picker) => {
			const ziel = document.getElementById(picker.dataset.datumZiel);
			if (ziel) {
				picker.value = deutschZuIso(ziel.value);
			}
		});
	}

	document.addEventListener("input", (ereignis) => {
		const element = ereignis.target;
		if (!(element instanceof HTMLInputElement)) {
			return;
		}
		if (element.dataset.datumZiel) {
			const deutsch = isoZuDeutsch(element.value);
			const ziel = document.getElementById(element.dataset.datumZiel);
			if (ziel && deutsch) {
				ziel.value = deutsch;
			}
			return;
		}
		if (element.id) {
			synchronisiereTextZuPicker(element);
		}
	});

	document.addEventListener("change", (ereignis) => {
		const element = ereignis.target;
		if (!(element instanceof HTMLInputElement)) {
			return;
		}
		if (element.dataset.datumZiel) {
			const deutsch = isoZuDeutsch(element.value);
			const ziel = document.getElementById(element.dataset.datumZiel);
			if (ziel && deutsch) {
				ziel.value = deutsch;
			}
		}
	});

	document.addEventListener("DOMContentLoaded", synchronisiereAllePicker);
	document.addEventListener("htmx:after:swap", synchronisiereAllePicker);
	document.addEventListener("htmx:afterSwap", synchronisiereAllePicker);

	if (document.readyState !== "loading") {
		synchronisiereAllePicker();
	}
})();
