(function () {
    var password = document.getElementById("password");
    var toggle = document.getElementById("togglePassword");
    var form = document.getElementById("loginForm");
    if (password && toggle) {
        toggle.addEventListener("click", function () {
            var hidden = password.type === "password";
            password.type = hidden ? "text" : "password";
            toggle.setAttribute("aria-label", hidden ? "Hide password" : "Show password");
            toggle.innerHTML = hidden
                ? '<i class="fa-regular fa-eye-slash"></i>'
                : '<i class="fa-regular fa-eye"></i>';
        });
    }

    var fills = document.querySelectorAll(".demo-fill");
    Array.prototype.forEach.call(fills, function (button) {
        button.addEventListener("click", function () {
            var user = document.getElementById("username");
            var pass = document.getElementById("password");
            if (user) {
                user.value = button.getAttribute("data-user");
            }
            if (pass) {
                pass.value = button.getAttribute("data-pass");
            }
            if (form) {
                form.submit();
            }
        });
    });

    if (form) {
        form.addEventListener("submit", function () {
            var loader = document.getElementById("pageLoader");
            if (!loader) {
                return;
            }
            loader.hidden = false;
            loader.setAttribute("aria-hidden", "false");
            document.body.classList.add("is-page-loading");
        });
    }
})();
