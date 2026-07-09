const users = {
  1: "Maria",
  2: "Jonas"
};

const statusLabels = {
  OPEN: "Offen",
  IN_PROGRESS: "In Arbeit",
  DONE: "Fertig"
};

const state = {
  userId: 1,
  projects: [],
  selectedProject: null,
  tasks: [],
  selectedTask: null,
  comments: []
};

const el = {
  projectList: document.querySelector("#projectList"),
  projectForm: document.querySelector("#projectForm"),
  memberForm: document.querySelector("#memberForm"),
  taskForm: document.querySelector("#taskForm"),
  taskBoard: document.querySelector("#taskBoard"),
  taskDetails: document.querySelector("#taskDetails"),
  commentList: document.querySelector("#commentList"),
  commentForm: document.querySelector("#commentForm"),
  deleteTaskButton: document.querySelector("#deleteTaskButton"),
  projectTitle: document.querySelector("#projectTitle"),
  projectDescription: document.querySelector("#projectDescription"),
  projectOwner: document.querySelector("#projectOwner"),
  refreshButton: document.querySelector("#refreshButton"),
  toast: document.querySelector("#toast")
};

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

async function loadProjects() {
  state.projects = await api(`/projects?userId=${state.userId}`);
  if (!state.selectedProject || !state.projects.some(project => project.id === state.selectedProject.id)) {
    state.selectedProject = state.projects[0] || null;
  }
  renderProjects();
  await loadTasks();
}

async function loadTasks() {
  if (!state.selectedProject) {
    state.tasks = [];
    state.selectedTask = null;
    renderProjectHeader();
    renderTasks();
    renderDetails();
    return;
  }

  state.tasks = await api(`/projects/${state.selectedProject.id}/tasks`);
  if (state.selectedTask) {
    state.selectedTask = state.tasks.find(task => task.id === state.selectedTask.id) || null;
  }
  renderProjectHeader();
  renderTasks();
  await loadComments();
}

async function loadComments() {
  if (!state.selectedTask) {
    state.comments = [];
    renderDetails();
    return;
  }
  state.comments = await api(`/tasks/${state.selectedTask.id}/comments`);
  renderDetails();
}

function renderProjects() {
  el.projectList.innerHTML = "";
  if (!state.projects.length) {
    el.projectList.innerHTML = `<div class="empty-state">Keine Projekte für ${users[state.userId]}.</div>`;
    return;
  }

  for (const project of state.projects) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `project-button ${state.selectedProject?.id === project.id ? "active" : ""}`;
    button.innerHTML = `
      <strong>${escapeHtml(project.name)}</strong>
      <span class="project-meta">${project.memberIds.length} Mitglieder · Owner ${users[project.ownerId] || project.ownerId}</span>
      <span class="project-meta">${escapeHtml(project.description || "Keine Beschreibung")}</span>
    `;
    button.addEventListener("click", async () => {
      state.selectedProject = project;
      state.selectedTask = null;
      renderProjects();
      await loadTasks();
    });
    el.projectList.append(button);
  }
}

function renderProjectHeader() {
  if (!state.selectedProject) {
    el.projectTitle.textContent = "Projekt auswählen";
    el.projectDescription.textContent = "Wählen Sie links ein Projekt aus.";
    el.projectOwner.textContent = "Owner";
    el.memberForm.hidden = true;
    el.taskForm.hidden = true;
    return;
  }

  el.projectTitle.textContent = state.selectedProject.name;
  el.projectDescription.textContent = state.selectedProject.description || "Keine Beschreibung";
  el.projectOwner.textContent = `Owner ${users[state.selectedProject.ownerId] || state.selectedProject.ownerId}`;
  el.memberForm.hidden = false;
  el.taskForm.hidden = false;
}

function renderTasks() {
  el.taskBoard.innerHTML = "";
  const statuses = ["OPEN", "IN_PROGRESS", "DONE"];

  for (const status of statuses) {
    const column = document.createElement("section");
    column.className = "column";
    const tasks = state.tasks.filter(task => task.status === status);
    column.innerHTML = `
      <div class="column-header">
        <span>${statusLabels[status]}</span>
        <span>${tasks.length}</span>
      </div>
      <div class="task-list"></div>
    `;
    const list = column.querySelector(".task-list");

    if (!tasks.length) {
      list.innerHTML = `<div class="empty-state">Keine Aufgaben.</div>`;
    }

    for (const task of tasks) {
      list.append(createTaskCard(task));
    }
    el.taskBoard.append(column);
  }
}

function createTaskCard(task) {
  const card = document.createElement("article");
  card.className = `task-card ${state.selectedTask?.id === task.id ? "active" : ""}`;
  card.innerHTML = `
    <div>
      <h3>${escapeHtml(task.title)}</h3>
      <p>${escapeHtml(task.description || "Keine Beschreibung")}</p>
    </div>
    <div class="task-footer">
      <span class="status-pill ${statusClass(task.status)}">${statusLabels[task.status] || task.status}</span>
      <span class="project-meta">${task.dueDate || "Kein Datum"} · ${users[task.assignedUserId] || "Ohne Person"}</span>
    </div>
    <div class="task-actions" aria-label="Status ändern">
      <button type="button" title="Offen" aria-label="Offen">○</button>
      <button type="button" title="In Arbeit" aria-label="In Arbeit">◐</button>
      <button type="button" title="Fertig" aria-label="Fertig">✓</button>
    </div>
  `;

  card.addEventListener("click", async event => {
    if (event.target.tagName === "BUTTON") {
      return;
    }
    state.selectedTask = task;
    renderTasks();
    await loadComments();
  });

  const buttons = card.querySelectorAll("button");
  buttons[0].addEventListener("click", () => updateTaskStatus(task, "OPEN"));
  buttons[1].addEventListener("click", () => updateTaskStatus(task, "IN_PROGRESS"));
  buttons[2].addEventListener("click", () => updateTaskStatus(task, "DONE"));
  return card;
}

function renderDetails() {
  if (!state.selectedTask) {
    el.taskDetails.className = "task-details empty-state";
    el.taskDetails.textContent = "Keine Aufgabe ausgewählt.";
    el.commentList.innerHTML = "";
    el.commentForm.hidden = true;
    el.deleteTaskButton.disabled = true;
    return;
  }

  const task = state.selectedTask;
  el.deleteTaskButton.disabled = false;
  el.commentForm.hidden = false;
  el.taskDetails.className = "task-details";
  el.taskDetails.innerHTML = `
    <h3>${escapeHtml(task.title)}</h3>
    <div class="detail-row"><span>Status</span><span>${statusLabels[task.status] || task.status}</span></div>
    <div class="detail-row"><span>Fällig</span><span>${task.dueDate || "Kein Datum"}</span></div>
    <div class="detail-row"><span>Person</span><span>${users[task.assignedUserId] || "Ohne Person"}</span></div>
    <div class="detail-row"><span>Text</span><span>${escapeHtml(task.description || "Keine Beschreibung")}</span></div>
  `;

  el.commentList.innerHTML = "";
  if (!state.comments.length) {
    el.commentList.innerHTML = `<div class="empty-state">Keine Kommentare.</div>`;
    return;
  }

  for (const comment of state.comments) {
    const item = document.createElement("div");
    item.className = "comment";
    item.innerHTML = `
      <p>${escapeHtml(comment.text)}</p>
      <small>${users[comment.userId] || comment.userId} · ${formatDate(comment.createdAt)}</small>
    `;
    el.commentList.append(item);
  }
}

async function updateTaskStatus(task, status) {
  try {
    state.selectedTask = await api(`/tasks/${task.id}`, {
      method: "PUT",
      body: JSON.stringify({ status })
    });
    await loadTasks();
    showToast("Status aktualisiert");
  } catch (error) {
    showToast("Status konnte nicht geändert werden");
  }
}

el.projectForm.addEventListener("submit", async event => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  try {
    state.selectedProject = await api("/projects", {
      method: "POST",
      body: JSON.stringify({
        name: form.get("name"),
        description: form.get("description"),
        ownerId: state.userId,
        memberIds: [state.userId]
      })
    });
    event.currentTarget.reset();
    await loadProjects();
    showToast("Projekt angelegt");
  } catch (error) {
    showToast("Projekt konnte nicht angelegt werden");
  }
});

el.memberForm.addEventListener("submit", async event => {
  event.preventDefault();
  if (!state.selectedProject) {
    return;
  }
  const form = new FormData(event.currentTarget);
  try {
    state.selectedProject = await api(`/projects/${state.selectedProject.id}/members`, {
      method: "POST",
      body: JSON.stringify({ userId: Number(form.get("userId")) })
    });
    await loadProjects();
    showToast("Mitglied ergänzt");
  } catch (error) {
    showToast("Mitglied konnte nicht ergänzt werden");
  }
});

el.taskForm.addEventListener("submit", async event => {
  event.preventDefault();
  if (!state.selectedProject) {
    return;
  }
  const form = new FormData(event.currentTarget);
  const assignedUserId = form.get("assignedUserId");
  try {
    state.selectedTask = await api("/tasks", {
      method: "POST",
      body: JSON.stringify({
        projectId: state.selectedProject.id,
        title: form.get("title"),
        description: form.get("description"),
        status: form.get("status"),
        dueDate: form.get("dueDate") || null,
        assignedUserId: assignedUserId ? Number(assignedUserId) : null
      })
    });
    event.currentTarget.reset();
    await loadTasks();
    showToast("Aufgabe angelegt");
  } catch (error) {
    showToast("Aufgabe konnte nicht angelegt werden");
  }
});

el.commentForm.addEventListener("submit", async event => {
  event.preventDefault();
  if (!state.selectedTask) {
    return;
  }
  const form = new FormData(event.currentTarget);
  try {
    await api(`/tasks/${state.selectedTask.id}/comments`, {
      method: "POST",
      body: JSON.stringify({
        userId: state.userId,
        text: form.get("text")
      })
    });
    event.currentTarget.reset();
    await loadComments();
    showToast("Kommentar gespeichert");
  } catch (error) {
    showToast("Kommentar konnte nicht gespeichert werden");
  }
});

el.deleteTaskButton.addEventListener("click", async () => {
  if (!state.selectedTask) {
    return;
  }
  try {
    await api(`/tasks/${state.selectedTask.id}`, { method: "DELETE" });
    state.selectedTask = null;
    await loadTasks();
    showToast("Aufgabe gelöscht");
  } catch (error) {
    showToast("Aufgabe konnte nicht gelöscht werden");
  }
});

el.refreshButton.addEventListener("click", () => loadProjects().catch(() => showToast("Aktualisierung fehlgeschlagen")));

document.querySelectorAll(".segmented-button").forEach(button => {
  button.addEventListener("click", async () => {
    document.querySelectorAll(".segmented-button").forEach(item => item.classList.remove("active"));
    button.classList.add("active");
    state.userId = Number(button.dataset.userId);
    state.selectedProject = null;
    state.selectedTask = null;
    await loadProjects();
  });
});

function statusClass(status) {
  if (status === "OPEN") {
    return "open";
  }
  if (status === "IN_PROGRESS") {
    return "progress";
  }
  if (status === "DONE") {
    return "done";
  }
  return "";
}

function formatDate(value) {
  if (!value) {
    return "";
  }
  return new Intl.DateTimeFormat("de-CH", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(value));
}

function showToast(message) {
  el.toast.textContent = message;
  el.toast.classList.add("visible");
  clearTimeout(showToast.timeout);
  showToast.timeout = setTimeout(() => el.toast.classList.remove("visible"), 2200);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

loadProjects().catch(() => showToast("Daten konnten nicht geladen werden"));
