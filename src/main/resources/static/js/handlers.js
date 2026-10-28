document.addEventListener('DOMContentLoaded', function () {
    var logoutLink = document.getElementById('logout-link');
    if (logoutLink) {
        logoutLink.addEventListener('click', function (e) {
            e.preventDefault();
            document.getElementById('logout').submit();
        });
    }

    var submitOnchangeInputs = document.querySelectorAll('input[data-submit-on-change]');
    submitOnchangeInputs.forEach(function (input) {
        input.addEventListener('change', function () {
            input.form.submit();
        });
    });
});
