document.addEventListener('DOMContentLoaded', function () {
    const tabButtons = document.querySelectorAll('[data-composer-tab]');
    const tabPanes = document.querySelectorAll('[data-composer-pane]');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', function () {
            const target = this.getAttribute('data-composer-tab');
            tabButtons.forEach(b => b.classList.remove('btn-primary', 'text-white'));
            tabButtons.forEach(b => b.classList.add('btn-outline-secondary'));
            this.classList.remove('btn-outline-secondary');
            this.classList.add('btn-primary', 'text-white');

            tabPanes.forEach(pane => {
                if (pane.getAttribute('data-composer-pane') === target) {
                    pane.classList.remove('d-none');
                } else {
                    pane.classList.add('d-none');
                }
            });
        });
    });

    const confirmForms = document.querySelectorAll('form[data-confirm]');
    confirmForms.forEach(form => {
        form.addEventListener('submit', function (e) {
            const message = this.getAttribute('data-confirm');
            if (!window.confirm(message)) {
                e.preventDefault();
            }
        });
    });
});
