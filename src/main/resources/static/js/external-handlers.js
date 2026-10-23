// External event listeners - no inline handlers (CSP compliant)
document.addEventListener('DOMContentLoaded', function () {
    // Replace onclick on logout link
    var logoutLink = document.getElementById('logout-link');
    if (logoutLink) {
        logoutLink.addEventListener('click', function (e) {
            e.preventDefault();
            document.getElementById('logout-form').submit();
        });
    }

    // Replace onChange on perfumer checkboxes
    var perfumerCheckboxes = document.querySelectorAll('.perfumer-checkbox');
    perfumerCheckboxes.forEach(function (checkbox) {
        checkbox.addEventListener('change', function () {
            checkbox.form.submit();
        });
    });

    // Replace onChange on gender checkboxes
    var genderCheckboxes = document.querySelectorAll('.gender-checkbox');
    genderCheckboxes.forEach(function (checkbox) {
        checkbox.addEventListener('change', function () {
            checkbox.form.submit();
        });
    });

    // Replace onChange on price radio buttons
    var priceCheckboxes = document.querySelectorAll('.price-checkbox');
    priceCheckboxes.forEach(function (radio) {
        radio.addEventListener('change', function () {
            radio.form.submit();
        });
    });
});
