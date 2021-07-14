jQuery(document).ready(function($) {

    var buttons = $(':button');

    $(buttons).each(function() {
        $(this).click(function() {
            var action = $('#action');

            action.val($(this).val());
        });
    });

     // this will prevent forms from submitting twice
     $('form').preventDoubleSubmission();
});