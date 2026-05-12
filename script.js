(function () {
  'use strict';

  // Mobile nav toggle
  const toggle = document.querySelector('.nav-toggle');
  const body = document.body;
  if (toggle) {
    toggle.addEventListener('click', () => {
      body.classList.toggle('nav-open');
      const expanded = body.classList.contains('nav-open');
      toggle.setAttribute('aria-expanded', expanded);
    });
    document.querySelectorAll('.nav-links a').forEach((link) => {
      link.addEventListener('click', () => body.classList.remove('nav-open'));
    });
  }

  // Scroll reveal animations
  const reveals = document.querySelectorAll('.reveal');
  if ('IntersectionObserver' in window && reveals.length) {
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('in');
            io.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' }
    );
    reveals.forEach((el) => io.observe(el));
  } else {
    reveals.forEach((el) => el.classList.add('in'));
  }

  // Form: demo submit
  const form = document.querySelector('form[data-demo-form]');
  if (form) {
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      const submit = form.querySelector('button[type="submit"]');
      const original = submit.textContent;
      submit.textContent = 'Se trimite...';
      submit.disabled = true;
      setTimeout(() => {
        submit.textContent = 'Trimis cu succes!';
        form.reset();
        setTimeout(() => {
          submit.textContent = original;
          submit.disabled = false;
        }, 2400);
      }, 800);
    });
  }

  // Year in footer
  document.querySelectorAll('[data-year]').forEach((el) => {
    el.textContent = new Date().getFullYear();
  });
})();
