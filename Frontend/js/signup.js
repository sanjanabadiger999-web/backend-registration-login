import { AUTH_API_URL, REGISTER_API_URL } from './api.js';

const byId = (id) => document.getElementById(id);

const nameInput = byId('name');
const passwordInput = byId('password');
const confirmInput = byId('confirmPassword');
const emailInput = byId('email');
const phoneInput = byId('phone');
const signupBtn = byId('signupBtn');
const alertBox = byId('alertBox');

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const phoneRegex = /^[+]?[0-9\s-]{10,15}$/;

const errorSpans = {
  name: byId('nameError'),
  password: byId('passwordError'),
  confirmPassword: byId('confirmPasswordError'),
  email: byId('emailError'),
  phone: byId('phoneError'),
};

function setError(field, message) {
  const input = byId(field);
  const span = errorSpans[field];
  if (message) {
    input.classList.add('error');
    span.textContent = message;
    span.classList.add('show');
  } else {
    input.classList.remove('error');
    span.textContent = '';
    span.classList.remove('show');
  }
}

function validateForm() {
  let valid = true;
  const name = nameInput.value.trim();
  const password = passwordInput.value;
  const confirm = confirmInput.value;
  const email = emailInput.value.trim();
  const phone = phoneInput.value.trim();

  if (!name || name.length < 3) {
    setError('name', 'Username must be at least 3 characters.');
    valid = false;
  } else {
    setError('name', '');
  }

  if (!password || password.length < 6) {
    setError('password', 'Password must be at least 6 characters.');
    valid = false;
  } else {
    setError('password', '');
  }

  if (!confirm) {
    setError('confirmPassword', 'Please confirm your password.');
    valid = false;
  } else if (password !== confirm) {
    setError('confirmPassword', 'Password and Confirm Password do not match.');
    valid = false;
  } else {
    setError('confirmPassword', '');
  }

  if (!emailRegex.test(email)) {
    setError('email', 'Enter a valid email address.');
    valid = false;
  } else {
    setError('email', '');
  }

  if (!phoneRegex.test(phone)) {
    setError('phone', 'Enter a valid phone number.');
    valid = false;
  } else {
    setError('phone', '');
  }

  return valid;
}

function showAlert(type, message) {
  alertBox.className = 'alert show ' + type;
  alertBox.textContent = message;
}

document.getElementById('signupForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  alertBox.className = 'alert';

  if (!validateForm()) {
    showAlert('error', 'Please fix the highlighted fields.');
    return;
  }

  const payload = {
    name: nameInput.value.trim(),
    password: passwordInput.value,
    confirmPassword: confirmInput.value,
    email: emailInput.value.trim(),
    phone: phoneInput.value.trim(),
  };

  signupBtn.disabled = true;
  signupBtn.textContent = 'Creating account...';

  try {
    const response = await fetch(REGISTER_API_URL + '/api/reg', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(payload),
    });

    if (response.status === 201) {
      const data = await response.json();
      showAlert('success', data.message || 'Registration successful! Redirecting to login...');
      document.getElementById('signupForm').reset();
      setTimeout(() => { window.location.href = 'login.html'; }, 1500);
    } else {
      let message = 'Registration failed. Please try again.';
      try {
        const data = await response.json();
        if (data.message) message = data.message;
      } catch (_) { /* non-JSON error body */ }
      showAlert('error', message);
    }
  } catch (err) {
    showAlert('error', 'Cannot reach the registration service at ' + REGISTER_API_URL + '. Is it running?');
  } finally {
    signupBtn.disabled = false;
    signupBtn.textContent = 'Sign Up';
  }
});