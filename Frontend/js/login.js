import { AUTH_API_URL } from './api.js';

const byId = (id) => document.getElementById(id);

const nameInput = byId('name');
const passwordInput = byId('password');
const loginBtn = byId('loginBtn');
const alertBox = byId('alertBox');

const errorSpans = {
  name: byId('nameError'),
  password: byId('passwordError'),
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

function showAlert(type, message) {
  alertBox.className = 'alert show ' + type;
  alertBox.textContent = message;
}

document.getElementById('loginForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  alertBox.className = 'alert';

  const name = nameInput.value.trim();
  const password = passwordInput.value;

  let valid = true;
  if (!name) {
    setError('name', 'Username is required.');
    valid = false;
  } else {
    setError('name', '');
  }

  if (!password) {
    setError('password', 'Password is required.');
    valid = false;
  } else {
    setError('password', '');
  }

  if (!valid) {
    showAlert('error', 'Please fill in both fields.');
    return;
  }

  const payload = { name, password };

  loginBtn.disabled = true;
  loginBtn.textContent = 'Logging in...';

  try {
    const response = await fetch(AUTH_API_URL + '/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      const data = await response.json();
      window.location.href = 'home.html?user=' + encodeURIComponent(data.username || name);
    } else {
      let message = 'Login failed. Please try again.';
      try {
        const data = await response.json();
        if (data.message) message = data.message;
      } catch (_) { /* non-JSON error body */ }
      showAlert('error', message);
    }
  } catch (err) {
    showAlert('error', 'Cannot reach the authentication service at ' + AUTH_API_URL + '. Is it running?');
  } finally {
    loginBtn.disabled = false;
    loginBtn.textContent = 'Login';
  }
});