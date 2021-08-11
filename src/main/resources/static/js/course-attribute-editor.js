function buttonLoading(button) {
    button.setAttribute("aria-busy", true);
    button.disabled = true;
    button.classList.add("rvt-button--loading");
    button.getElementsByTagName('div')[0].classList.remove("hideMe");
}
