// FocusRoom Front-End Application Logic
// Interacts with the backend REST endpoints at http://localhost:8080/api/...

let currentUser = null;
let activeRoomId = null;
let activeRoomData = null;
let currentSessionData = null;
let timerInterval = null;
let roomPollingInterval = null;

// ==========================================================
// Initialization & Authentication State
// ==========================================================
document.addEventListener('DOMContentLoaded', () => {
    initAuth();
    setupEventHandlers();
    loadBrowseRooms();
});

function initAuth() {
    const saved = localStorage.getItem('focusroom_user');
    if (saved) {
        try {
            currentUser = JSON.parse(saved);
            renderAuthState();
        } catch (e) {
            currentUser = null;
            localStorage.removeItem('focusroom_user');
        }
    } else {
        renderAuthState();
    }
}

function renderAuthState() {
    const guestSection = document.getElementById('authGuestSection');
    const profileSection = document.getElementById('authProfileSection');
    const guestHero = document.getElementById('guestHero');
    const dashboardSection = document.getElementById('dashboardSection');

    if (currentUser) {
        guestSection.style.display = 'none';
        guestHero.style.display = 'none';
        profileSection.style.display = 'flex';
        dashboardSection.style.display = 'block';

        document.getElementById('profileName').textContent = currentUser.name;
        document.getElementById('profileDept').textContent = `${currentUser.department} (Yr ${currentUser.year})`;
        document.getElementById('userAvatar').textContent = currentUser.name.charAt(0).toUpperCase();

        loadMyRooms();
    } else {
        guestSection.style.display = 'block';
        guestHero.style.display = 'flex';
        profileSection.style.display = 'none';
        dashboardSection.style.display = 'block';
    }
}

function setupEventHandlers() {
    // Navigation Tabs
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

            btn.classList.add('active');
            const target = btn.getAttribute('data-tab');
            document.getElementById(target).classList.add('active');

            if (target === 'tabBrowse') loadBrowseRooms();
            if (target === 'tabMyRooms') loadMyRooms();
            if (target === 'tabHistory') loadStudentHistory();
        });
    });

    // Auth Modal Toggles
    const authModal = document.getElementById('authModal');
    document.getElementById('btnOpenLogin')?.addEventListener('click', () => openAuthModal('login'));
    document.getElementById('btnHeroLogin')?.addEventListener('click', () => openAuthModal('login'));
    document.getElementById('btnHeroDemo')?.addEventListener('click', () => {
        openAuthModal('login');
        autofillLogin('alice@university.edu', 'Password@123');
    });
    document.getElementById('btnAuthClose')?.addEventListener('click', () => authModal.style.display = 'none');

    document.getElementById('tabLoginBtn')?.addEventListener('click', () => switchAuthTab('login'));
    document.getElementById('tabRegisterBtn')?.addEventListener('click', () => switchAuthTab('register'));

    document.getElementById('btnLogout')?.addEventListener('click', () => {
        if (confirm('Are you sure you want to log out?')) {
            currentUser = null;
            localStorage.removeItem('focusroom_user');
            renderAuthState();
        }
    });

    // Login Form Submit
    document.getElementById('loginForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('loginEmail').value.trim();
        const password = document.getElementById('loginPassword').value;

        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            const data = await res.json();
            if (data.success) {
                currentUser = data.student;
                localStorage.setItem('focusroom_user', JSON.stringify(currentUser));
                authModal.style.display = 'none';
                renderAuthState();
                loadBrowseRooms();
            } else {
                alert(data.error || 'Login failed. Please verify your credentials.');
            }
        } catch (err) {
            alert('Could not connect to backend server: ' + err.message);
        }
    });

    // Register Form Submit
    document.getElementById('registerForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = document.getElementById('regName').value.trim();
        const email = document.getElementById('regEmail').value.trim();
        const password = document.getElementById('regPassword').value;
        const department = document.getElementById('regDept').value;
        const year = parseInt(document.getElementById('regYear').value, 10);

        try {
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, email, password, department, year })
            });
            const data = await res.json();
            if (data.success) {
                alert('Account created successfully! You are now logged in.');
                currentUser = data.student;
                localStorage.setItem('focusroom_user', JSON.stringify(currentUser));
                authModal.style.display = 'none';
                renderAuthState();
                loadBrowseRooms();
            } else {
                alert(data.error || 'Registration failed.');
            }
        } catch (err) {
            alert('Registration error: ' + err.message);
        }
    });

    // Filter Search
    document.getElementById('btnSearchRooms')?.addEventListener('click', loadBrowseRooms);
    document.getElementById('btnResetFilter')?.addEventListener('click', () => {
        document.getElementById('filterSubject').value = '';
        document.getElementById('filterExam').value = '';
        loadBrowseRooms();
    });
    document.getElementById('btnRefreshBrowse')?.addEventListener('click', loadBrowseRooms);
    document.getElementById('btnRefreshMyRooms')?.addEventListener('click', loadMyRooms);

    // Create Room Form
    document.getElementById('createRoomForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!requireAuth()) return;

        const roomName = document.getElementById('createRoomName').value.trim();
        const subject = document.getElementById('createSubject').value.trim();
        const examName = document.getElementById('createExam').value.trim();
        const description = document.getElementById('createDesc').value.trim();
        const maxParticipants = parseInt(document.getElementById('createCapacity').value, 10);

        try {
            const res = await fetch('/api/rooms', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    roomName, subject, examName, description, maxParticipants,
                    creatorId: currentUser.studentId
                })
            });
            const data = await res.json();
            if (data.success) {
                alert(`Study Room "${data.room.roomName}" created successfully!`);
                document.getElementById('createRoomForm').reset();
                openRoomModal(data.room.roomId);
            } else {
                alert(data.error || 'Failed to create room.');
            }
        } catch (err) {
            alert('Error creating room: ' + err.message);
        }
    });

    // Room Modal Controls
    document.getElementById('btnModalExit')?.addEventListener('click', closeRoomModal);
    document.getElementById('btnModalRefresh')?.addEventListener('click', refreshRoomData);
    document.getElementById('btnModalLeave')?.addEventListener('click', leaveCurrentRoom);
    document.getElementById('btnModalCloseRoom')?.addEventListener('click', closeCurrentRoom);

    // Timer Controls
    document.getElementById('btnStartSession')?.addEventListener('click', startFocusSession);
    document.getElementById('btnStopSession')?.addEventListener('click', stopFocusSession);

    // Share Resource Form
    document.getElementById('shareResourceForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!requireAuth()) return;

        const title = document.getElementById('resTitle').value.trim();
        const link = document.getElementById('resLink').value.trim();

        try {
            const res = await fetch(`/api/rooms/${activeRoomId}/resources`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    studentId: currentUser.studentId,
                    title,
                    link
                })
            });
            const data = await res.json();
            if (data.success) {
                document.getElementById('shareResourceForm').reset();
                refreshResources();
            } else {
                alert(data.error || 'Failed to share resource.');
            }
        } catch (err) {
            alert('Error sharing resource: ' + err.message);
        }
    });

    // History Tab & Profile History Button
    document.getElementById('btnHistory')?.addEventListener('click', () => {
        document.querySelector('[data-tab="tabHistory"]')?.click();
    });
    document.getElementById('btnRefreshHistory')?.addEventListener('click', loadStudentHistory);
}

function requireAuth() {
    if (!currentUser) {
        openAuthModal('login');
        return false;
    }
    return true;
}

function openAuthModal(tab) {
    document.getElementById('authModal').style.display = 'flex';
    switchAuthTab(tab);
}

function switchAuthTab(tab) {
    const loginContainer = document.getElementById('loginFormContainer');
    const registerContainer = document.getElementById('registerFormContainer');
    const loginTabBtn = document.getElementById('tabLoginBtn');
    const regTabBtn = document.getElementById('tabRegisterBtn');

    if (tab === 'login') {
        loginContainer.style.display = 'block';
        registerContainer.style.display = 'none';
        loginTabBtn.classList.add('active');
        regTabBtn.classList.remove('active');
    } else {
        loginContainer.style.display = 'none';
        registerContainer.style.display = 'block';
        regTabBtn.classList.add('active');
        loginTabBtn.classList.remove('active');
    }
}

function autofillLogin(email, pass) {
    document.getElementById('loginEmail').value = email;
    document.getElementById('loginPassword').value = pass;
}

// ==========================================================
// Browse & My Rooms
// ==========================================================
async function loadBrowseRooms() {
    const subject = encodeURIComponent(document.getElementById('filterSubject')?.value.trim() || '');
    const exam = encodeURIComponent(document.getElementById('filterExam')?.value.trim() || '');

    const grid = document.getElementById('browseRoomsGrid');
    grid.innerHTML = '<div class="text-muted">Loading open study rooms from MySQL...</div>';

    try {
        const res = await fetch(`/api/rooms?subject=${subject}&exam=${exam}`);
        const data = await res.json();

        if (!data.success || !data.rooms || data.rooms.length === 0) {
            grid.innerHTML = '<div class="card"><p class="text-muted">No open rooms found matching your search. Create one!</p></div>';
            return;
        }

        grid.innerHTML = '';
        data.rooms.forEach(room => {
            const card = createRoomCard(room, 'browse');
            grid.appendChild(card);
        });
    } catch (e) {
        grid.innerHTML = `<div class="card"><p class="text-danger">Failed to load rooms: ${e.message}</p></div>`;
    }
}

async function loadMyRooms() {
    if (!currentUser) return;

    const grid = document.getElementById('myRoomsGrid');
    grid.innerHTML = '<div class="text-muted">Loading your joined study rooms...</div>';

    try {
        const res = await fetch(`/api/rooms/my?studentId=${currentUser.studentId}`);
        const data = await res.json();

        const badge = document.getElementById('myRoomsBadge');
        if (badge) badge.textContent = data.rooms ? data.rooms.length : 0;

        if (!data.success || !data.rooms || data.rooms.length === 0) {
            grid.innerHTML = '<div class="card"><p class="text-muted">You have not joined any study rooms yet. Go to "Browse Rooms" to join!</p></div>';
            return;
        }

        grid.innerHTML = '';
        data.rooms.forEach(room => {
            const card = createRoomCard(room, 'my');
            grid.appendChild(card);
        });
    } catch (e) {
        grid.innerHTML = `<div class="card"><p class="text-danger">Failed to load your rooms: ${e.message}</p></div>`;
    }
}

function createRoomCard(room, context) {
    const card = document.createElement('div');
    card.className = 'room-card';

    const pct = Math.min(100, Math.round((room.activeCount / room.maxParticipants) * 100));

    card.innerHTML = `
        <div>
            <div class="room-card-header">
                <div class="room-badges">
                    <span class="badge badge-primary">${escapeHtml(room.subject)}</span>
                    <span class="badge badge-accent">${escapeHtml(room.examName)}</span>
                    <span class="badge badge-status ${room.roomStatus.toLowerCase()}">${room.roomStatus}</span>
                </div>
                ${room.hasActiveSession ? '<span class="session-live-pill">● Session Live</span>' : ''}
            </div>

            <h3>${escapeHtml(room.roomName)}</h3>
            <p class="room-desc">${escapeHtml(room.description || 'Focused peer study group.')}</p>

            <div class="capacity-container">
                <div class="capacity-labels">
                    <span>Capacity</span>
                    <strong>${room.activeCount} / ${room.maxParticipants} Members</strong>
                </div>
                <div class="progress-bar-bg">
                    <div class="progress-fill" style="width: ${pct}%"></div>
                </div>
            </div>
        </div>

        <div class="room-footer">
            <span class="creator-info">Host: ${escapeHtml(room.creatorName || 'Student #' + room.createdBy)}</span>
            <div class="room-card-actions" id="actions-${room.roomId}">
                <!-- Actions rendered below -->
            </div>
        </div>
    `;

    const actionsContainer = card.querySelector(`#actions-${room.roomId}`);
    if (context === 'my') {
        const enterBtn = document.createElement('button');
        enterBtn.className = 'btn btn-primary btn-sm';
        enterBtn.innerHTML = '🚪 Enter Room';
        enterBtn.onclick = () => openRoomModal(room.roomId);
        actionsContainer.appendChild(enterBtn);
    } else {
        const joinBtn = document.createElement('button');
        joinBtn.className = 'btn btn-success btn-sm';
        joinBtn.innerHTML = '🤝 Join & Enter';
        joinBtn.onclick = () => joinRoomAction(room.roomId);
        actionsContainer.appendChild(joinBtn);
    }

    return card;
}

async function joinRoomAction(roomId) {
    if (!requireAuth()) return;

    try {
        const res = await fetch(`/api/rooms/${roomId}/join`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ studentId: currentUser.studentId })
        });
        const data = await res.json();
        if (data.success) {
            openRoomModal(roomId);
            loadBrowseRooms();
            loadMyRooms();
        } else {
            alert(data.error || 'Failed to join room.');
        }
    } catch (e) {
        alert('Error joining room: ' + e.message);
    }
}

// ==========================================================
// Interactive Room View & Timer Logic
// ==========================================================
async function openRoomModal(roomId) {
    activeRoomId = roomId;
    const modal = document.getElementById('roomModal');
    modal.style.display = 'flex';

    await refreshRoomData();

    // Start background live polling every 6 seconds
    if (roomPollingInterval) clearInterval(roomPollingInterval);
    roomPollingInterval = setInterval(refreshRoomData, 6000);

    // Start local timer clock tick every 1 second
    if (timerInterval) clearInterval(timerInterval);
    timerInterval = setInterval(updateTimerDisplay, 1000);
}

function closeRoomModal() {
    activeRoomId = null;
    activeRoomData = null;
    currentSessionData = null;
    if (roomPollingInterval) clearInterval(roomPollingInterval);
    if (timerInterval) clearInterval(timerInterval);

    document.getElementById('roomModal').style.display = 'none';
    loadBrowseRooms();
    loadMyRooms();
}

async function refreshRoomData() {
    if (!activeRoomId) return;

    try {
        // 1. Room info
        const roomRes = await fetch(`/api/rooms/${activeRoomId}`);
        const roomData = await roomRes.json();
        if (roomData.success) {
            activeRoomData = roomData.room;
            renderRoomHeader(activeRoomData);
        }

        // 2. Active Session
        const sessionRes = await fetch(`/api/rooms/${activeRoomId}/session`);
        const sessionData = await sessionRes.json();
        if (sessionData.success && sessionData.active) {
            currentSessionData = sessionData.session;
            renderSessionActive(currentSessionData);
        } else {
            currentSessionData = null;
            renderSessionIdle();
        }

        // 3. Members
        refreshMembers();

        // 4. Resources
        refreshResources();

    } catch (e) {
        console.warn('Sync error: ', e);
    }
}

function renderRoomHeader(room) {
    document.getElementById('modalRoomTitle').textContent = room.roomName;
    document.getElementById('modalSubject').textContent = room.subject;
    document.getElementById('modalExam').textContent = room.examName;
    document.getElementById('modalStatus').textContent = room.roomStatus;
    document.getElementById('modalRoomMeta').textContent = 
        `Host: ${room.creatorName || room.createdBy} • Capacity: ${room.activeCount} / ${room.maxParticipants} Active Members`;

    const closeBtn = document.getElementById('btnModalCloseRoom');
    if (currentUser && currentUser.studentId === room.createdBy && room.roomStatus === 'OPEN') {
        closeBtn.style.display = 'inline-block';
    } else {
        closeBtn.style.display = 'none';
    }
}

function renderSessionActive(session) {
    document.getElementById('timerPulse').style.display = 'inline-block';
    document.getElementById('timerStatusText').textContent = 
        `🟢 Focus Session in progress! Initiated by: ${session.startedByName || 'Peer'}`;
    document.getElementById('timerStatusText').style.color = '#34D399';

    document.getElementById('btnStartSession').disabled = true;
    document.getElementById('btnStopSession').disabled = false;
    updateTimerDisplay();
}

function renderSessionIdle() {
    document.getElementById('timerPulse').style.display = 'none';
    document.getElementById('timerClock').textContent = '00:00:00';
    document.getElementById('timerStatusText').textContent = '⚪ No active study session. Click "Start Focus Session" to begin.';
    document.getElementById('timerStatusText').style.color = 'var(--text-muted)';

    document.getElementById('btnStartSession').disabled = false;
    document.getElementById('btnStopSession').disabled = true;
}

function updateTimerDisplay() {
    if (currentSessionData && currentSessionData.startTime) {
        const now = Date.now();
        const start = currentSessionData.startTime;
        const diffSeconds = Math.max(0, Math.floor((now - start) / 1000));

        const hrs = String(Math.floor(diffSeconds / 3600)).padStart(2, '0');
        const mins = String(Math.floor((diffSeconds % 3600) / 60)).padStart(2, '0');
        const secs = String(diffSeconds % 60).padStart(2, '0');

        document.getElementById('timerClock').textContent = `${hrs}:${mins}:${secs}`;
    }
}

async function startFocusSession() {
    if (!requireAuth()) return;

    try {
        const res = await fetch(`/api/rooms/${activeRoomId}/session-start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ studentId: currentUser.studentId })
        });
        const data = await res.json();
        if (data.success) {
            currentSessionData = data.session;
            renderSessionActive(currentSessionData);
        } else {
            alert(data.error || 'Failed to start session.');
        }
    } catch (e) {
        alert('Error starting session: ' + e.message);
    }
}

async function stopFocusSession() {
    if (!requireAuth() || !currentSessionData) return;

    if (!confirm('Conclude the current focus study session?')) return;

    try {
        const res = await fetch(`/api/rooms/${activeRoomId}/session-stop`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sessionId: currentSessionData.sessionId,
                studentId: currentUser.studentId
            })
        });
        const data = await res.json();
        if (data.success) {
            alert(`Study session ended! Recorded duration: ${data.durationMinutes} minute(s).`);
            currentSessionData = null;
            renderSessionIdle();
            refreshRoomData();
        } else {
            alert(data.error || 'Failed to stop session.');
        }
    } catch (e) {
        alert('Error stopping session: ' + e.message);
    }
}

async function refreshMembers() {
    try {
        const res = await fetch(`/api/rooms/${activeRoomId}/members`);
        const data = await res.json();
        const list = document.getElementById('membersList');
        const countSpan = document.getElementById('modalActiveCount');

        if (data.success && data.members) {
            countSpan.textContent = data.members.length;
            list.innerHTML = '';
            data.members.forEach(m => {
                const isYou = currentUser && currentUser.studentId === m.studentId;
                const chip = document.createElement('div');
                chip.className = 'member-chip';
                chip.innerHTML = `
                    <div class="avatar">${m.studentName.charAt(0)}</div>
                    <div class="member-meta">
                        <span class="member-name">${escapeHtml(m.studentName)} ${isYou ? '<strong style="color:#60A5FA">(You)</strong>' : ''}</span>
                        <span class="member-dept">${escapeHtml(m.department)} • Year ${m.year}</span>
                    </div>
                `;
                list.appendChild(chip);
            });
        }
    } catch (e) {
        console.warn('Members refresh error', e);
    }
}

async function refreshResources() {
    try {
        const res = await fetch(`/api/rooms/${activeRoomId}/resources`);
        const data = await res.json();
        const list = document.getElementById('resourcesList');

        if (data.success && data.resources) {
            if (data.resources.length === 0) {
                list.innerHTML = '<p class="text-muted" style="padding: 10px;">No resources shared yet. Be the first to share notes or references!</p>';
                return;
            }
            list.innerHTML = '';
            data.resources.forEach(r => {
                const item = document.createElement('div');
                item.className = 'resource-item';
                item.innerHTML = `
                    <div class="resource-info">
                        <h5>📄 ${escapeHtml(r.title)}</h5>
                        <span>Shared by ${escapeHtml(r.studentName || 'Peer')}</span>
                    </div>
                    <a href="${escapeHtml(r.link)}" target="_blank" rel="noopener noreferrer" class="btn btn-secondary btn-sm">
                        🌐 Open Link ↗
                    </a>
                `;
                list.appendChild(item);
            });
        }
    } catch (e) {
        console.warn('Resources refresh error', e);
    }
}

async function leaveCurrentRoom() {
    if (!requireAuth()) return;
    if (!confirm('Leave this study room? You can rejoin later.')) return;

    try {
        const res = await fetch(`/api/rooms/${activeRoomId}/leave`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ studentId: currentUser.studentId })
        });
        const data = await res.json();
        if (data.success) {
            closeRoomModal();
        } else {
            alert(data.error || 'Failed to leave room.');
        }
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

async function closeCurrentRoom() {
    if (!requireAuth()) return;
    if (!confirm('Permanently close this study room? No further sessions or joins will be allowed.')) return;

    try {
        const res = await fetch(`/api/rooms/${activeRoomId}/close`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ studentId: currentUser.studentId })
        });
        const data = await res.json();
        if (data.success) {
            alert('Room closed.');
            refreshRoomData();
        } else {
            alert(data.error || 'Failed to close room.');
        }
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

// ==========================================================
// Study History & Analytics
// ==========================================================
async function loadStudentHistory() {
    if (!currentUser) return;

    try {
        // 1. Stats
        const statsRes = await fetch(`/api/student/stats?studentId=${currentUser.studentId}`);
        const statsData = await statsRes.json();
        if (statsData.success) {
            document.getElementById('statsTotalTime').textContent = 
                `${statsData.hours} hr ${statsData.minutes} min`;
            document.getElementById('statsTotalMins').textContent = 
                `${statsData.totalMinutes} total focus minutes recorded`;
        }

        // 2. History table
        const historyRes = await fetch(`/api/student/history?studentId=${currentUser.studentId}`);
        const historyData = await historyRes.json();
        const tbody = document.getElementById('historyTableBody');
        tbody.innerHTML = '';

        if (historyData.success && historyData.history) {
            let completedCount = 0;
            historyData.history.forEach(item => {
                if (item.status === 'COMPLETED') completedCount++;

                const tr = document.createElement('tr');
                const startStr = item.startTime ? new Date(item.startTime).toLocaleString() : '-';
                const isHost = item.startedBy === currentUser.studentId;

                tr.innerHTML = `
                    <td><strong>#${item.sessionId}</strong></td>
                    <td>${escapeHtml(item.roomName || 'Room #' + item.roomId)}</td>
                    <td><span class="badge badge-primary">${escapeHtml(item.subject || '-')}</span></td>
                    <td>${isHost ? '<strong>You</strong>' : escapeHtml(item.startedByName || 'Peer')}</td>
                    <td>${startStr}</td>
                    <td><strong>${item.durationMinutes} mins</strong></td>
                    <td><span class="badge ${item.status === 'COMPLETED' ? 'badge-accent' : 'badge-status'}">${item.status}</span></td>
                `;
                tbody.appendChild(tr);
            });

            document.getElementById('statsTotalSessions').textContent = `${completedCount} Completed Sessions`;
        }
    } catch (e) {
        console.error('History load error', e);
    }
}

// Utility
function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
