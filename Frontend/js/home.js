import { AUTH_API_URL } from './api.js';

const byId = (id) => document.getElementById(id);
const welcomeUser = byId('welcomeUser');
const avatarText = byId('avatarText');
const alertBox = byId('alertBox');
const logoutBtn = byId('logoutBtn');

function showAlert(type, message) {
  alertBox.className = 'alert show ' + type;
  alertBox.textContent = message;
}

function renderUser(username) {
  welcomeUser.textContent = username;
  avatarText.textContent = username.charAt(0).toUpperCase();
}

async function loadCurrentUser() {
  try {
    const response = await fetch(AUTH_API_URL + '/api/me', {
      method: 'GET',
      credentials: 'include',
    });

    if (response.status === 401) {
      window.location.href = 'login.html';
      return;
    }

    if (response.ok) {
      const data = await response.json();
      if (data.username) {
        renderUser(data.username);
      } else {
        window.location.href = 'login.html';
      }
      return;
    }

    showAlert('error', 'Could not verify your session.');
  } catch (err) {
    showAlert('error', 'Cannot reach the authentication service at ' + AUTH_API_URL + '. Is it running?');
    window.location.href = 'login.html';
  }
}

logoutBtn.addEventListener('click', async () => {
  logoutBtn.disabled = true;
  logoutBtn.textContent = 'Logging out...';

  try {
    const response = await fetch(AUTH_API_URL + '/api/logout', {
      method: 'POST',
      credentials: 'include',
    });
    if (response.ok || response.status === 401) {
      window.location.href = 'login.html';
    } else {
      showAlert('error', 'Logout failed. Please try again.');
      logoutBtn.disabled = false;
      logoutBtn.textContent = 'Logout';
    }
  } catch (err) {
    showAlert('error', 'Cannot reach the authentication service at ' + AUTH_API_URL + '. Is it running?');
    logoutBtn.disabled = false;
    logoutBtn.textContent = 'Logout';
  }
});

loadCurrentUser();