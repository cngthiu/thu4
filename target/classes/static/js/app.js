// src/main/resources/static/js/app.js

(function () {
  'use strict';

  const body = document.body;
  const loadingEl = document.getElementById('appLoading');
  const sidebar = document.querySelector('.app-sidebar');

  function toggleSidebar(force) {
    if (!sidebar) {
      return;
    }
    if (window.innerWidth < 992) {
      sidebar.classList.toggle('open', force ?? !sidebar.classList.contains('open'));
      body.classList.toggle('sidebar-collapsed', false);
    } else {
      body.classList.toggle('sidebar-collapsed', force ?? !body.classList.contains('sidebar-collapsed'));
      sidebar.classList.remove('open');
    }
  }

  function setupSidebar() {
    document.querySelectorAll('[data-action="toggle-sidebar"]').forEach(btn => {
      btn.addEventListener('click', event => {
        event.preventDefault();
        toggleSidebar();
      });
    });

    if (sidebar) {
      sidebar.querySelectorAll('.app-nav-link').forEach(link => {
        link.addEventListener('click', () => {
          if (window.innerWidth < 992) {
            toggleSidebar(false);
          }
        });
      });
    }

    window.addEventListener('resize', () => {
      if (window.innerWidth >= 992) {
        sidebar?.classList.remove('open');
      }
    });
  }

  function showLoading() {
    loadingEl?.classList.remove('d-none');
  }

  function hideLoading() {
    loadingEl?.classList.add('d-none');
  }

  function setupLoadingListeners() {
    document.querySelectorAll('form[data-loading]').forEach(form => {
      form.addEventListener('submit', () => {
        showLoading();
        setTimeout(hideLoading, 4000); // fallback auto-hide
      });
    });

    document.querySelectorAll('[data-trigger-loading]').forEach(el => {
      el.addEventListener('click', () => {
        showLoading();
        setTimeout(hideLoading, 4000);
      });
    });
  }

  function setupConfirmations() {
    const modalEl = document.getElementById('confirmModal');
    if (!modalEl) {
      return;
    }
    const modal = new bootstrap.Modal(modalEl);
    const messageEl = document.getElementById('confirmMessage');
    const acceptBtn = document.getElementById('confirmAcceptButton');

    document.querySelectorAll('form[data-confirm]').forEach(form => {
      form.addEventListener('submit', event => {
        if (form.dataset.confirmed === 'true') {
          return;
        }
        event.preventDefault();
        const message = form.dataset.confirm || 'Are you sure?';
        if (messageEl) {
          messageEl.textContent = message;
        }
        acceptBtn.onclick = () => {
          form.dataset.confirmed = 'true';
          modal.hide();
          showLoading();
          form.submit();
        };
        modal.show();
      });
    });
  }

  function toast(message, variant = 'primary', delay = 4000) {
    const container = document.getElementById('toastContainer');
    if (!container || !message) {
      return;
    }
    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center text-bg-${variant} border-0`;
    toastEl.role = 'alert';
    toastEl.ariaLive = 'assertive';
    toastEl.ariaAtomic = 'true';
    toastEl.innerHTML = `
      <div class="d-flex">
        <div class="toast-body fw-semibold">${message}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
      </div>
    `;
    container.appendChild(toastEl);
    const toastObj = new bootstrap.Toast(toastEl, { delay });
    toastObj.show();
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
  }

  function setupFlashToast() {
    const flashMessage = body.dataset.toast;
    if (flashMessage) {
      toast(flashMessage, 'success');
    }
  }

  function resetPage(target) {
    const selector = target?.dataset?.resetPage;
    if (!selector) {
      return;
    }
    const hidden = document.querySelector(selector);
    if (hidden) {
      hidden.value = '0';
    }
  }

  function setupDebouncedSearch() {
    if (typeof _.debounce !== 'function') {
      return;
    }
    document.querySelectorAll('input[data-debounce]').forEach(input => {
      const wait = Number(input.dataset.debounce) || 400;
      const submitOnDebounce = _.debounce(() => {
        const formId = input.dataset.targetForm;
        const targetForm = formId ? document.getElementById(formId) : input.form;
        resetPage(input);
        targetForm?.requestSubmit();
      }, wait);
      input.addEventListener('input', submitOnDebounce);
    });

    document.querySelectorAll('[data-auto-submit]').forEach(select => {
      select.addEventListener('change', () => {
        const formId = select.dataset.targetForm;
        const targetForm = formId ? document.getElementById(formId) : select.form;
        resetPage(select);
        targetForm?.requestSubmit();
      });
    });
  }

  function setupLazyTables() {
    document.querySelectorAll('[data-animate-row]').forEach((row, index) => {
      row.style.setProperty('--stagger', `${index * 25}ms`);
    });
  }

  document.addEventListener('DOMContentLoaded', () => {
    setupSidebar();
    setupLoadingListeners();
    setupConfirmations();
    setupFlashToast();
    setupDebouncedSearch();
    setupLazyTables();
  });

  window.App = {
    toast,
    showLoading,
    hideLoading,
  };
})();
