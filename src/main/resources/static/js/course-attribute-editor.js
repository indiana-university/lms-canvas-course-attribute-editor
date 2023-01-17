function buttonLoading(button) {
    if (button.dataset.action != null) {
        document.getElementById("caeSubmit").value = button.dataset.action;
    }

    button.setAttribute("aria-busy", true);
    var buttonsToDisable = document.getElementsByTagName('button');
    for(var i = 0; i < buttonsToDisable.length; i++) {
        buttonsToDisable[i].disabled = true;
    }
    button.classList.add("rvt-button--loading");
    button.getElementsByTagName('div')[0].classList.remove("rvt-display-none");

    // FF doesn't need this, but Chrome and Edge do
    button.form.submit();
}
