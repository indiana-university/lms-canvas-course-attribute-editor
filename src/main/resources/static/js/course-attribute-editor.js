/*-
 * #%L
 * course-attribute-editor
 * %%
 * Copyright (C) 2023 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
    button.getElementsByClassName('rvt-loader')[0].classList.remove("rvt-display-none");
    button.getElementsByClassName('loader-sr-text')[0].classList.remove("rvt-display-none");

    // FF doesn't need this, but Chrome and Edge do
    button.form.submit();
}

jQuery(document).ready(function($) {

    // handle focus
    // first, look for an alert at the top. If there isn't an alert, look for an invalid input.
    // Lastly, check to see if we need to focus on the course details or edit regions. These last
    // options are passed in as a hidden input
    let alertMsg = $(".rvt-alert");
    let invalidInputs = $("input[aria-invalid='true']");

    if (alertMsg.length > 0) {
        alertMsg.first().focus();
    } else if (invalidInputs.length > 0) {
        invalidInputs.first().focus();
    } else {
        let caeFocus = $("#cae-focus").val();
        if (caeFocus) {
            let focusTarget = document.getElementById(caeFocus);
            if (focusTarget) {
                focusTarget.focus();
            }
        }
    }
});
