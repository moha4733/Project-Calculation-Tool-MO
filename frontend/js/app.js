import {
    apiRequest,
    clearStoredToken,
    getStoredToken,
    setStoredToken
} from "./apiClient.js";

const showLoginButton = document.querySelector("#showLoginButton");
const showRegisterButton = document.querySelector("#showRegisterButton");
const loginForm = document.querySelector("#loginForm");
const registerForm = document.querySelector("#registerForm");
const usernameInput = document.querySelector("#username");
const passwordInput = document.querySelector("#password");
const registerUsernameInput = document.querySelector("#registerUsername");
const registerEmailInput = document.querySelector("#registerEmail");
const registerPasswordInput = document.querySelector("#registerPassword");
const registerRoleInput = document.querySelector("#registerRole");
const registerAlphaRoleInput = document.querySelector("#registerAlphaRole");
const loginButton = document.querySelector("#loginButton");
const registerButton = document.querySelector("#registerButton");
const logoutButton = document.querySelector("#logoutButton");
const refreshDashboardButton = document.querySelector("#refreshDashboardButton");
const refreshProjectsButton = document.querySelector("#refreshProjectsButton");
const createProjectButton = document.querySelector("#createProjectButton");
const addMemberButton = document.querySelector("#addMemberButton");
const createSubProjectButton = document.querySelector("#createSubProjectButton");
const createTaskButton = document.querySelector("#createTaskButton");
const createSubTaskButton = document.querySelector("#createSubTaskButton");
const loginMessage = document.querySelector("#loginMessage");
const registerMessage = document.querySelector("#registerMessage");
const workspaceMessage = document.querySelector("#workspaceMessage");
const loginPanel = document.querySelector("#loginPanel");
const workspace = document.querySelector("#workspace");
const workspaceTitle = document.querySelector("#workspaceTitle");
const userKicker = document.querySelector("#userKicker");
const navLinks = document.querySelectorAll(".nav-link");
const viewPanels = document.querySelectorAll("[data-view-panel]");
const dashboardWelcome = document.querySelector("#dashboardWelcome");
const dashboardStats = document.querySelector("#dashboardStats");
const dashboardBreakdown = document.querySelector("#dashboardBreakdown");
const dashboardActivity = document.querySelector("#dashboardActivity");
const projectsList = document.querySelector("#projectsList");
const membersList = document.querySelector("#membersList");
const subProjectsList = document.querySelector("#subProjectsList");
const tasksList = document.querySelector("#tasksList");
const subTasksList = document.querySelector("#subTasksList");
const selectedMembersTitle = document.querySelector("#selectedMembersTitle");
const selectedProjectTitle = document.querySelector("#selectedProjectTitle");
const selectedSubProjectTitle = document.querySelector("#selectedSubProjectTitle");
const selectedTaskTitle = document.querySelector("#selectedTaskTitle");
const crudDialog = document.querySelector("#crudDialog");
const crudForm = document.querySelector("#crudForm");
const dialogTitle = document.querySelector("#dialogTitle");
const dialogFields = document.querySelector("#dialogFields");
const dialogMessage = document.querySelector("#dialogMessage");
const closeDialogButton = document.querySelector("#closeDialogButton");
const cancelDialogButton = document.querySelector("#cancelDialogButton");

const state = {
    currentEmployee: null,
    availableEmployees: [],
    projectMembers: [],
    projects: [],
    subProjects: [],
    tasks: [],
    subTasks: [],
    dashboard: {
        isLoading: false,
        members: [],
        subProjects: [],
        tasks: [],
        subTasks: []
    },
    selectedProject: null,
    selectedSubProject: null,
    selectedTask: null,
    activeView: "dashboard",
    pendingSubmit: null,
    isBusy: false
};

showLoginButton.addEventListener("click", () => setAuthMode("login"));
showRegisterButton.addEventListener("click", () => setAuthMode("register"));
loginForm.addEventListener("submit", login);
registerForm.addEventListener("submit", register);
logoutButton.addEventListener("click", logout);
refreshDashboardButton.addEventListener("click", refreshDashboard);
refreshProjectsButton.addEventListener("click", loadProjects);
createProjectButton.addEventListener("click", () => openProjectDialog());
addMemberButton.addEventListener("click", () => openAddMemberDialog());
createSubProjectButton.addEventListener("click", () => openSubProjectDialog());
createTaskButton.addEventListener("click", () => openTaskDialog());
createSubTaskButton.addEventListener("click", () => openSubTaskDialog());
navLinks.forEach((link) => {
    link.addEventListener("click", (event) => {
        event.preventDefault();
        setActiveView(link.dataset.view);
    });
});
closeDialogButton.addEventListener("click", () => crudDialog.close());
cancelDialogButton.addEventListener("click", () => crudDialog.close());
crudForm.addEventListener("submit", submitDialog);

renderAllLists();
setActiveView(initialViewFromHash(), false);

if (getStoredToken()) {
    bootWorkspace();
}

function setAuthMode(mode) {
    const isLogin = mode === "login";
    loginForm.hidden = !isLogin;
    registerForm.hidden = isLogin;
    showLoginButton.classList.toggle("active", isLogin);
    showRegisterButton.classList.toggle("active", !isLogin);
    loginMessage.textContent = "";
    registerMessage.textContent = "";
}

async function login(event) {
    event.preventDefault();
    loginMessage.textContent = "";
    setBusy(true);

    try {
        const result = await apiRequest("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({
                username: usernameInput.value,
                password: passwordInput.value
            })
        });
        setStoredToken(result.token);
        await bootWorkspace();
    } catch (error) {
        loginMessage.textContent = error.message;
    } finally {
        setBusy(false);
    }
}

async function register(event) {
    event.preventDefault();
    registerMessage.textContent = "";
    setBusy(true);

    try {
        const result = await apiRequest("/api/auth/register", {
            method: "POST",
            body: JSON.stringify({
                username: registerUsernameInput.value,
                password: registerPasswordInput.value,
                email: registerEmailInput.value,
                role: registerRoleInput.value,
                alphaRole: registerAlphaRoleInput.value
            })
        });
        setStoredToken(result.token);
        await bootWorkspace();
    } catch (error) {
        registerMessage.textContent = error.message;
    } finally {
        setBusy(false);
    }
}

async function bootWorkspace() {
    showWorkspace();
    try {
        await loadCurrentEmployee();
        userKicker.textContent = `${state.currentEmployee.username} - ${roleLabel(state.currentEmployee.role)}`;
        updateCreateButtons();
        await loadProjects();
    } catch (error) {
        returnToLogin(error.message);
    }
}

function logout() {
    clearStoredToken();
    loginPanel.hidden = false;
    workspace.hidden = true;
    logoutButton.hidden = true;
    userKicker.textContent = "Workspace";
    workspaceMessage.textContent = "";
    resetState();
    renderAllLists();
}

function showWorkspace() {
    loginPanel.hidden = true;
    workspace.hidden = false;
    logoutButton.hidden = false;
}

function returnToLogin(message) {
    clearStoredToken();
    loginPanel.hidden = false;
    workspace.hidden = true;
    logoutButton.hidden = true;
    userKicker.textContent = "Workspace";
    resetState();
    renderAllLists();
    setAuthMode("login");
    loginMessage.textContent = message;
}

async function loadCurrentEmployee() {
    state.currentEmployee = await apiRequest("/api/employees/me");
}

async function loadProjects() {
    await runWithLoading(projectsList, "Loading projects", async () => {
        clearSelections("project");
        state.projects = await apiRequest("/api/projects");
        await loadDashboardData();
        renderAllLists();
        updateCreateButtons();
    });
}

async function selectProject(project) {
    await runWithLoading(subProjectsList, "Loading subprojects", async () => {
        state.selectedProject = project;
        clearSelections("subProject");
        showLoading(membersList, "Loading members");
        const [members, subProjects] = await Promise.all([
            apiRequest(`/api/projects/${project.projectId}/members`),
            apiRequest(`/api/projects/${project.projectId}/subprojects`)
        ]);
        state.projectMembers = members;
        state.subProjects = subProjects;
        renderProjects();
        renderMembers();
        renderSubProjects();
        updateCreateButtons();
    });
}

async function selectSubProject(subProject) {
    await runWithLoading(tasksList, "Loading tasks", async () => {
        state.selectedSubProject = subProject;
        clearSelections("task");
        state.tasks = await apiRequest(tasksPath());
        renderSubProjects();
        renderTasks();
        updateCreateButtons();
    });
}

async function selectTask(task) {
    await runWithLoading(subTasksList, "Loading subtasks", async () => {
        state.selectedTask = task;
        state.subTasks = await apiRequest(subTasksPath());
        renderTasks();
        renderSubTasks();
        updateCreateButtons();
    });
}

async function refreshDashboard() {
    await runMutation(async () => {
        await loadDashboardData();
        renderDashboard();
        showWorkspaceMessage("Dashboard refreshed");
    });
}

async function loadDashboardData() {
    if (!state.currentEmployee) {
        return;
    }

    state.dashboard.isLoading = true;
    renderDashboard();
    const membersById = new Map();
    const subProjects = [];
    const tasks = [];
    const subTasks = [];

    try {
        await Promise.all(state.projects.map(async (project) => {
            const [projectMembers, projectSubProjects] = await Promise.all([
                apiRequest(`/api/projects/${project.projectId}/members`),
                apiRequest(`/api/projects/${project.projectId}/subprojects`)
            ]);

            projectMembers.forEach((member) => membersById.set(member.employeeId, member));
            subProjects.push(...projectSubProjects);

            await Promise.all(projectSubProjects.map(async (subProject) => {
                const subProjectTasks = await apiRequest(`/api/projects/${project.projectId}/subprojects/${subProject.subProjectId}/tasks`);
                tasks.push(...subProjectTasks);

                await Promise.all(subProjectTasks.map(async (task) => {
                    const taskSubTasks = await apiRequest(
                        `/api/projects/${project.projectId}/subprojects/${subProject.subProjectId}/tasks/${task.taskId}/subtasks`
                    );
                    subTasks.push(...taskSubTasks);
                }));
            }));
        }));

        state.dashboard.members = [...membersById.values()];
        state.dashboard.subProjects = subProjects;
        state.dashboard.tasks = tasks;
        state.dashboard.subTasks = subTasks;
    } catch (error) {
        showWorkspaceMessage(error.message);
    } finally {
        state.dashboard.isLoading = false;
    }
}

function renderAllLists() {
    renderDashboard();
    renderProjects();
    renderMembers();
    renderSubProjects();
    renderTasks();
    renderSubTasks();
    renderActiveView();
}

function setActiveView(view, updateHash = true) {
    state.activeView = isValidView(view) ? view : "dashboard";
    if (updateHash) {
        history.replaceState(null, "", `#${state.activeView}`);
    }
    renderActiveView();
}

function renderActiveView() {
    viewPanels.forEach((panel) => {
        panel.hidden = panel.dataset.viewPanel !== state.activeView;
        panel.classList.toggle("is-active-view", panel.dataset.viewPanel === state.activeView);
    });
    navLinks.forEach((link) => {
        link.classList.toggle("active", link.dataset.view === state.activeView);
    });
    workspaceTitle.textContent = viewTitle(state.activeView);
}

function renderDashboard() {
    dashboardStats.innerHTML = "";
    dashboardBreakdown.innerHTML = "";
    dashboardActivity.innerHTML = "";
    dashboardWelcome.textContent = state.currentEmployee
        ? `Welcome back, ${state.currentEmployee.username}`
        : "Welcome back";

    if (!state.currentEmployee) {
        dashboardStats.appendChild(emptyItem("Log in to see dashboard metrics"));
        return;
    }
    if (state.dashboard.isLoading) {
        dashboardStats.appendChild(emptyItem("Loading dashboard"));
        return;
    }

    const metrics = dashboardMetrics();
    [
        { label: "Active Projects", value: metrics.activeProjects },
        { label: "Tasks", value: metrics.totalTasks },
        { label: "Developers", value: metrics.developers }
    ].forEach((metric) => dashboardStats.appendChild(metricCard(metric.label, metric.value)));

    [
        { label: "Active", value: metrics.activeTasks },
        { label: "Completed", value: metrics.completedTasks },
        { label: "Late Tasks", value: metrics.lateTasks }
    ].forEach((metric) => dashboardBreakdown.appendChild(metricCard(metric.label, metric.value, "compact")));

    if (state.projects.length === 0) {
        dashboardActivity.appendChild(emptyItem(isProjectManager()
            ? "No projects yet. Create a project to start building the workspace."
            : "No projects assigned yet."));
        return;
    }

    state.projects.slice(0, 4).forEach((project) => {
        dashboardActivity.appendChild(createListItem({
            isActive: state.selectedProject?.projectId === project.projectId,
            title: project.projectName,
            meta: [
                project.projectCustomer || "No customer",
                projectPeriod(project)
            ],
            onSelect: async () => {
                await selectProject(project);
                setActiveView("subprojects");
            }
        }));
    });
}

function renderProjects() {
    projectsList.innerHTML = "";
    selectedProjectTitle.textContent = state.selectedProject?.projectName || "Subprojects";

    if (!state.currentEmployee) {
        projectsList.appendChild(emptyItem("Log in to see projects"));
        return;
    }
    if (state.projects.length === 0) {
        projectsList.appendChild(emptyItem(isProjectManager()
            ? "No projects yet. Create the first project to start planning work."
            : "No projects assigned yet. Ask a project manager to add you as a member."));
        return;
    }

    state.projects.forEach((project) => {
        projectsList.appendChild(createListItem({
            isActive: state.selectedProject?.projectId === project.projectId,
            title: project.projectName,
            meta: [project.projectCustomer || "No customer"],
            onSelect: () => selectProject(project),
            onEdit: isProjectManager() ? () => openProjectDialog(project) : null,
            onDelete: isProjectManager() ? () => deleteProject(project) : null
        }));
    });
}

function renderMembers() {
    membersList.innerHTML = "";
    selectedMembersTitle.textContent = state.selectedProject ? "Members" : "Members";

    if (!state.currentEmployee) {
        membersList.appendChild(emptyItem("Log in to see project members"));
        return;
    }
    if (!state.selectedProject) {
        membersList.appendChild(emptyItem("Select a project to see the delivery team"));
        return;
    }
    if (state.projectMembers.length === 0) {
        membersList.appendChild(emptyItem("No members have been added to this project yet"));
        return;
    }

    state.projectMembers.forEach((member) => {
        const isOwner = isProjectOwner(member);
        membersList.appendChild(createListItem({
            title: member.username,
            meta: [
                member.email || "No email",
                isOwner ? "Project owner" : roleLabel(member.role)
            ],
            onDelete: isProjectManager() && !isOwner ? () => removeProjectMember(member) : null
        }));
    });
}

function renderSubProjects() {
    subProjectsList.innerHTML = "";
    selectedSubProjectTitle.textContent = state.selectedSubProject?.subProjectName || "Tasks";

    if (!state.currentEmployee) {
        subProjectsList.appendChild(emptyItem("Log in to see subprojects"));
        return;
    }
    if (!state.selectedProject) {
        subProjectsList.appendChild(emptyItem("Select a project to see its subprojects"));
        return;
    }
    if (state.subProjects.length === 0) {
        subProjectsList.appendChild(emptyItem(isProjectManager()
            ? "No subprojects yet. Break the project into smaller delivery phases."
            : "No subprojects are available for this project yet."));
        return;
    }

    state.subProjects.forEach((subProject) => {
        subProjectsList.appendChild(createListItem({
            isActive: state.selectedSubProject?.subProjectId === subProject.subProjectId,
            title: subProject.subProjectName,
            meta: [subProject.subProjectDescription || "No description", `${subProject.subProjectDuration} days`],
            onSelect: () => selectSubProject(subProject),
            onEdit: isProjectManager() ? () => openSubProjectDialog(subProject) : null,
            onDelete: isProjectManager() ? () => deleteSubProject(subProject) : null
        }));
    });
}

function renderTasks() {
    tasksList.innerHTML = "";
    selectedTaskTitle.textContent = state.selectedTask?.taskName || "Subtasks";

    if (!state.currentEmployee) {
        tasksList.appendChild(emptyItem("Log in to see tasks"));
        return;
    }
    if (!state.selectedSubProject) {
        tasksList.appendChild(emptyItem("Select a subproject to see tasks"));
        return;
    }
    if (state.tasks.length === 0) {
        tasksList.appendChild(emptyItem(isProjectManager()
            ? "No tasks yet. Create tasks and assign them to project members."
            : "No tasks assigned to you in this subproject."));
        return;
    }

    state.tasks.forEach((task) => {
        tasksList.appendChild(createListItem({
            isActive: state.selectedTask?.taskId === task.taskId,
            title: task.taskName,
            meta: [task.taskDescription || "No description", formatTaskMeta(task), noteMeta(task.taskNote)],
            onSelect: () => selectTask(task),
            onEdit: isProjectManager() ? () => openTaskDialog(task) : null,
            onDelete: isProjectManager() ? () => deleteTask(task) : null,
            extraActions: canUpdateTaskProgress(task)
                ? [
                    { label: "Status", onClick: () => openTaskStatusDialog(task) },
                    { label: "Note", onClick: () => openTaskNoteDialog(task) }
                ]
                : []
        }));
    });
}

function renderSubTasks() {
    subTasksList.innerHTML = "";

    if (!state.currentEmployee) {
        subTasksList.appendChild(emptyItem("Log in to see subtasks"));
        return;
    }
    if (!state.selectedTask) {
        subTasksList.appendChild(emptyItem("Select a task to see subtasks"));
        return;
    }
    if (state.subTasks.length === 0) {
        subTasksList.appendChild(emptyItem(isProjectManager()
            ? "No subtasks yet. Add subtasks when the task needs a detailed breakdown."
            : "No subtasks have been created for this task."));
        return;
    }

    state.subTasks.forEach((subTask) => {
        subTasksList.appendChild(createListItem({
            title: subTask.subTaskName,
            meta: [subTask.subTaskDescription || "No description", formatSubTaskMeta(subTask), noteMeta(subTask.subTaskNote)],
            onEdit: isProjectManager() ? () => openSubTaskDialog(subTask) : null,
            onDelete: isProjectManager() ? () => deleteSubTask(subTask) : null,
            extraActions: canUpdateSelectedTaskProgress()
                ? [
                    { label: "Status", onClick: () => openSubTaskStatusDialog(subTask) },
                    { label: "Note", onClick: () => openSubTaskNoteDialog(subTask) }
                ]
                : []
        }));
    });
}

function createListItem({ isActive = false, title, meta, onSelect, onEdit, onDelete, extraActions = [] }) {
    const item = document.createElement("div");
    item.className = `item${isActive ? " active" : ""}`;

    const content = document.createElement("div");
    content.className = `item-content${onSelect ? " selectable" : ""}`;
    content.innerHTML = `
        <div class="item-title">${escapeHtml(title)}</div>
        ${meta.map((line) => `<div class="meta">${escapeHtml(line)}</div>`).join("")}
    `;
    if (onSelect) {
        content.tabIndex = 0;
        content.setAttribute("role", "button");
        content.addEventListener("click", onSelect);
        content.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                onSelect();
            }
        });
    }
    item.appendChild(content);

    if (onEdit || onDelete || extraActions.length > 0) {
        const actions = document.createElement("div");
        actions.className = "item-actions";
        extraActions.forEach((extraAction) => {
            actions.appendChild(actionButton(extraAction.label, extraAction.onClick));
        });
        if (onEdit) {
            actions.appendChild(actionButton("Edit", onEdit));
        }
        if (onDelete) {
            actions.appendChild(actionButton("Delete", onDelete, "danger"));
        }
        item.appendChild(actions);
    }

    return item;
}

function actionButton(label, onClick, variant = "") {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `small-button ${variant}`.trim();
    button.textContent = label;
    button.addEventListener("click", (event) => {
        event.stopPropagation();
        onClick();
    });
    return button;
}

function openProjectDialog(project = null) {
    openDialog({
        title: project ? "Edit project" : "Create project",
        fields: [
            textField("projectName", "Name", project?.projectName, true),
            textField("projectCustomer", "Customer", project?.projectCustomer),
            dateField("projectStartDate", "Start date", project?.projectStartDate),
            dateField("projectDeadline", "Deadline", project?.projectDeadline),
            textAreaField("projectDescription", "Description", project?.projectDescription)
        ],
        onSubmit: async (values) => {
            if (project) {
                await apiRequest(`/api/projects/${project.projectId}`, {
                    method: "PUT",
                    body: JSON.stringify(values)
                });
            } else {
                await apiRequest("/api/projects", {
                    method: "POST",
                    body: JSON.stringify(values)
                });
            }
            await loadProjects();
            showWorkspaceMessage(project ? "Project updated" : "Project created");
        }
    });
}

function openSubProjectDialog(subProject = null) {
    if (!state.selectedProject) {
        return;
    }

    const period = dateBounds(state.selectedProject.projectStartDate, state.selectedProject.projectDeadline);
    openDialog({
        title: subProject ? "Edit subproject" : "Create subproject",
        fields: [
            hintField(`Allowed period: ${formatAllowedPeriod(period)}`),
            textField("subProjectName", "Name", subProject?.subProjectName, true),
            dateField("subProjectStartDate", "Start date", subProject?.subProjectStartDate || period.min, period),
            dateField("subProjectDeadline", "Deadline", subProject?.subProjectDeadline || period.max, period),
            textAreaField("subProjectDescription", "Description", subProject?.subProjectDescription)
        ],
        onSubmit: async (values) => {
            if (subProject) {
                await apiRequest(`${subProjectsPath()}/${subProject.subProjectId}`, {
                    method: "PUT",
                    body: JSON.stringify(values)
                });
            } else {
                await apiRequest(subProjectsPath(), {
                    method: "POST",
                    body: JSON.stringify(values)
                });
            }
            await reloadSubProjects();
            showWorkspaceMessage(subProject ? "Subproject updated" : "Subproject created");
        }
    });
}

async function openAddMemberDialog() {
    if (!state.selectedProject) {
        return;
    }

    setBusy(true);
    try {
        state.availableEmployees = await apiRequest(`/api/projects/${state.selectedProject.projectId}/available-employees`);
        if (state.availableEmployees.length === 0) {
            showWorkspaceMessage("No available employees for this project");
            return;
        }

        openDialog({
            title: "Add member",
            fields: [
                selectField("employeeId", "Employee", availableEmployeeOptions(), state.availableEmployees[0].employeeId, true)
            ],
            onSubmit: async (values) => {
                await apiRequest(`/api/projects/${state.selectedProject.projectId}/members/${values.employeeId}`, {
                    method: "POST"
                });
                await reloadProjectMembers();
                showWorkspaceMessage("Member added");
            }
        });
    } catch (error) {
        showWorkspaceMessage(error.message);
    } finally {
        setBusy(false);
    }
}

function openTaskDialog(task = null) {
    if (!state.selectedSubProject) {
        return;
    }

    const period = dateBounds(state.selectedSubProject.subProjectStartDate, state.selectedSubProject.subProjectDeadline);
    openDialog({
        title: task ? "Edit task" : "Create task",
        fields: [
            hintField(`Allowed period: ${formatAllowedPeriod(period)}`),
            textField("taskName", "Name", task?.taskName, true),
            dateField("taskStartDate", "Start date", task?.taskStartDate || period.min, period),
            dateField("taskDeadline", "Deadline", task?.taskDeadline || period.max, period),
            selectField("taskStatus", "Status", statusOptions(), task?.taskStatus || "NOT_STARTED"),
            selectField("taskPriority", "Priority", priorityOptions(), task?.taskPriority || "MEDIUM"),
            selectField("assignedToEmployeeId", "Assigned to", employeeOptions(), task?.assignedEmployee?.employeeId || ""),
            textAreaField("taskDescription", "Description", task?.taskDescription),
            textAreaField("taskNote", "Note", task?.taskNote)
        ],
        onSubmit: async (values) => {
            values.assignedToEmployeeId = values.assignedToEmployeeId ? Number(values.assignedToEmployeeId) : null;
            if (task) {
                await apiRequest(`${tasksPath()}/${task.taskId}`, {
                    method: "PUT",
                    body: JSON.stringify(values)
                });
            } else {
                await apiRequest(tasksPath(), {
                    method: "POST",
                    body: JSON.stringify(values)
                });
            }
            await reloadTasks();
            showWorkspaceMessage(task ? "Task updated" : "Task created");
        }
    });
}

function openTaskStatusDialog(task) {
    openDialog({
        title: "Update task status",
        fields: [
            selectField("status", "Status", statusOptions(), task.taskStatus || "NOT_STARTED", true)
        ],
        onSubmit: async (values) => {
            await apiRequest(`${tasksPath()}/${task.taskId}/status`, {
                method: "PATCH",
                body: JSON.stringify({ status: values.status })
            });
            await reloadTasks();
            showWorkspaceMessage("Task status updated");
        }
    });
}

function openTaskNoteDialog(task) {
    openDialog({
        title: "Update task note",
        fields: [
            textAreaField("note", "Note", task.taskNote)
        ],
        onSubmit: async (values) => {
            await apiRequest(`${tasksPath()}/${task.taskId}/note`, {
                method: "PATCH",
                body: JSON.stringify({ note: values.note })
            });
            await reloadTasks();
            showWorkspaceMessage("Task note updated");
        }
    });
}

function openSubTaskDialog(subTask = null) {
    if (!state.selectedTask) {
        return;
    }

    const period = dateBounds(state.selectedTask.taskStartDate, state.selectedTask.taskDeadline);
    openDialog({
        title: subTask ? "Edit subtask" : "Create subtask",
        fields: [
            hintField(`Allowed period: ${formatAllowedPeriod(period)}`),
            textField("subTaskName", "Name", subTask?.subTaskName, true),
            dateField("subTaskStartDate", "Start date", subTask?.subTaskStartDate || period.min, period),
            dateField("subTaskDeadline", "Deadline", subTask?.subTaskDeadline || period.max, period),
            selectField("subTaskStatus", "Status", statusOptions(), subTask?.subTaskStatus || "NOT_STARTED"),
            selectField("subTaskPriority", "Priority", priorityOptions(), subTask?.subTaskPriority || "MEDIUM"),
            textAreaField("subTaskDescription", "Description", subTask?.subTaskDescription),
            textAreaField("subTaskNote", "Note", subTask?.subTaskNote)
        ],
        onSubmit: async (values) => {
            if (subTask) {
                await apiRequest(`${subTasksPath()}/${subTask.subTaskId}`, {
                    method: "PUT",
                    body: JSON.stringify(values)
                });
            } else {
                await apiRequest(subTasksPath(), {
                    method: "POST",
                    body: JSON.stringify(values)
                });
            }
            await reloadSubTasks();
            showWorkspaceMessage(subTask ? "Subtask updated" : "Subtask created");
        }
    });
}

function openSubTaskStatusDialog(subTask) {
    openDialog({
        title: "Update subtask status",
        fields: [
            selectField("status", "Status", statusOptions(), subTask.subTaskStatus || "NOT_STARTED", true)
        ],
        onSubmit: async (values) => {
            await apiRequest(`${subTasksPath()}/${subTask.subTaskId}/status`, {
                method: "PATCH",
                body: JSON.stringify({ status: values.status })
            });
            await reloadSubTasks();
            showWorkspaceMessage("Subtask status updated");
        }
    });
}

function openSubTaskNoteDialog(subTask) {
    openDialog({
        title: "Update subtask note",
        fields: [
            textAreaField("note", "Note", subTask.subTaskNote)
        ],
        onSubmit: async (values) => {
            await apiRequest(`${subTasksPath()}/${subTask.subTaskId}/note`, {
                method: "PATCH",
                body: JSON.stringify({ note: values.note })
            });
            await reloadSubTasks();
            showWorkspaceMessage("Subtask note updated");
        }
    });
}

function openDialog({ title, fields, onSubmit }) {
    dialogTitle.textContent = title;
    dialogMessage.textContent = "";
    dialogFields.innerHTML = "";
    state.pendingSubmit = onSubmit;

    fields.forEach((field) => {
        dialogFields.appendChild(renderField(field));
    });

    crudDialog.showModal();
}

async function submitDialog(event) {
    event.preventDefault();
    dialogMessage.textContent = "";

    const values = Object.fromEntries(new FormData(crudForm).entries());
    try {
        setDialogBusy(true);
        await state.pendingSubmit(values);
        crudDialog.close();
    } catch (error) {
        dialogMessage.textContent = error.message;
    } finally {
        setDialogBusy(false);
    }
}

function renderField(field) {
    if (field.type === "hint") {
        const hint = document.createElement("p");
        hint.className = "field-hint";
        hint.textContent = field.text;
        return hint;
    }

    const label = document.createElement("label");
    label.textContent = field.label;

    let control;
    if (field.type === "textarea") {
        control = document.createElement("textarea");
        control.rows = 3;
    } else if (field.type === "select") {
        control = document.createElement("select");
        field.options.forEach((option) => {
            const element = document.createElement("option");
            element.value = option.value;
            element.textContent = option.label;
            control.appendChild(element);
        });
    } else {
        control = document.createElement("input");
        control.type = field.type;
    }

    control.name = field.name;
    control.value = field.value || "";
    control.required = Boolean(field.required);
    if (field.min) {
        control.min = field.min;
    }
    if (field.max) {
        control.max = field.max;
    }
    label.appendChild(control);
    return label;
}

async function deleteProject(project) {
    if (!confirm(`Delete project "${project.projectName}"?`)) {
        return;
    }
    await runMutation(async () => {
        await apiRequest(`/api/projects/${project.projectId}`, { method: "DELETE" });
        await loadProjects();
        showWorkspaceMessage("Project deleted");
    });
}

async function deleteSubProject(subProject) {
    if (!confirm(`Delete subproject "${subProject.subProjectName}"?`)) {
        return;
    }
    await runMutation(async () => {
        await apiRequest(`${subProjectsPath()}/${subProject.subProjectId}`, { method: "DELETE" });
        await reloadSubProjects();
        showWorkspaceMessage("Subproject deleted");
    });
}

async function deleteTask(task) {
    if (!confirm(`Delete task "${task.taskName}"?`)) {
        return;
    }
    await runMutation(async () => {
        await apiRequest(`${tasksPath()}/${task.taskId}`, { method: "DELETE" });
        await reloadTasks();
        showWorkspaceMessage("Task deleted");
    });
}

async function deleteSubTask(subTask) {
    if (!confirm(`Delete subtask "${subTask.subTaskName}"?`)) {
        return;
    }
    await runMutation(async () => {
        await apiRequest(`${subTasksPath()}/${subTask.subTaskId}`, { method: "DELETE" });
        await reloadSubTasks();
        showWorkspaceMessage("Subtask deleted");
    });
}

async function removeProjectMember(member) {
    if (!confirm(`Remove ${member.username} from "${state.selectedProject.projectName}"?`)) {
        return;
    }
    await runMutation(async () => {
        await apiRequest(`/api/projects/${state.selectedProject.projectId}/members/${member.employeeId}`, {
            method: "DELETE"
        });
        await reloadProjectMembers();
        showWorkspaceMessage("Member removed");
    });
}

async function reloadProjectMembers() {
    state.projectMembers = await apiRequest(`/api/projects/${state.selectedProject.projectId}/members`);
    await loadDashboardData();
    renderMembers();
    renderDashboard();
    updateCreateButtons();
}

async function reloadSubProjects() {
    const path = subProjectsPath();
    clearSelections("subProject");
    state.subProjects = await apiRequest(path);
    await loadDashboardData();
    renderAllLists();
    updateCreateButtons();
}

async function reloadTasks() {
    const path = tasksPath();
    clearSelections("task");
    state.tasks = await apiRequest(path);
    await loadDashboardData();
    renderAllLists();
    updateCreateButtons();
}

async function reloadSubTasks() {
    state.subTasks = await apiRequest(subTasksPath());
    await loadDashboardData();
    renderAllLists();
}

function clearSelections(level) {
    if (level === "project") {
        state.selectedProject = null;
        state.projects = [];
        state.projectMembers = [];
        state.availableEmployees = [];
    }
    if (level === "project" || level === "subProject") {
        state.selectedSubProject = null;
        state.subProjects = [];
    }
    if (level === "project" || level === "subProject" || level === "task") {
        state.selectedTask = null;
        state.tasks = [];
        state.subTasks = [];
    }
}

function resetState() {
    state.currentEmployee = null;
    state.projectMembers = [];
    state.dashboard = {
        isLoading: false,
        members: [],
        subProjects: [],
        tasks: [],
        subTasks: []
    };
    clearSelections("project");
}

function initialViewFromHash() {
    return window.location.hash.replace("#", "");
}

function isValidView(view) {
    return ["dashboard", "projects", "members", "subprojects", "tasks", "subtasks"].includes(view);
}

function viewTitle(view) {
    return {
        dashboard: "Dashboard",
        projects: "Projects",
        members: "Members",
        subprojects: "Subprojects",
        tasks: "Tasks",
        subtasks: "Subtasks"
    }[view] || "Projects";
}

function updateCreateButtons() {
    const canManage = isProjectManager();
    createProjectButton.hidden = !canManage;
    addMemberButton.hidden = !canManage || !state.selectedProject;
    createSubProjectButton.hidden = !canManage || !state.selectedProject;
    createTaskButton.hidden = !canManage || !state.selectedSubProject;
    createSubTaskButton.hidden = !canManage || !state.selectedTask;
}

function isProjectManager() {
    return state.currentEmployee?.role === "PROJECT_MANAGER";
}

function canUpdateTaskProgress(task) {
    if (isProjectManager()) {
        return true;
    }
    return task.assignedEmployee?.employeeId === state.currentEmployee?.employeeId;
}

function canUpdateSelectedTaskProgress() {
    return state.selectedTask && canUpdateTaskProgress(state.selectedTask);
}

function subProjectsPath() {
    return `/api/projects/${state.selectedProject.projectId}/subprojects`;
}

function tasksPath() {
    return `${subProjectsPath()}/${state.selectedSubProject.subProjectId}/tasks`;
}

function subTasksPath() {
    return `${tasksPath()}/${state.selectedTask.taskId}/subtasks`;
}

function textField(name, label, value = "", required = false) {
    return { name, label, value, required, type: "text" };
}

function dateField(name, label, value = "", bounds = {}) {
    return { name, label, value, min: bounds.min, max: bounds.max, type: "date" };
}

function textAreaField(name, label, value = "") {
    return { name, label, value, type: "textarea" };
}

function hintField(text) {
    return { text, type: "hint" };
}

function selectField(name, label, options, value = "", required = false) {
    return { name, label, options, value, required, type: "select" };
}

function dateBounds(min, max) {
    return { min: min || "", max: max || "" };
}

function dashboardMetrics() {
    const tasks = state.dashboard.tasks;
    const subTasks = state.dashboard.subTasks;
    const workItems = [...tasks, ...subTasks];
    const completedTasks = workItems.filter((item) => itemStatus(item) === "COMPLETED").length;

    return {
        activeProjects: state.projects.length,
        totalTasks: tasks.length,
        developers: state.dashboard.members.filter((member) => member.role === "TEAM_MEMBER").length,
        activeTasks: workItems.filter((item) => itemStatus(item) !== "COMPLETED").length,
        completedTasks,
        lateTasks: workItems.filter(isLateWorkItem).length
    };
}

function metricCard(label, value, variant = "") {
    const card = document.createElement("article");
    card.className = `metric-card ${variant}`.trim();
    card.innerHTML = `
        <span>${escapeHtml(label)}</span>
        <strong>${escapeHtml(value)}</strong>
    `;
    return card;
}

function itemStatus(item) {
    return item.taskStatus || item.subTaskStatus || "";
}

function itemDeadline(item) {
    return item.taskDeadline || item.subTaskDeadline || "";
}

function isLateWorkItem(item) {
    const deadline = itemDeadline(item);
    if (!deadline || itemStatus(item) === "COMPLETED") {
        return false;
    }
    return new Date(`${deadline}T23:59:59`) < new Date();
}

function projectPeriod(project) {
    if (project.projectStartDate && project.projectDeadline) {
        return `${formatDate(project.projectStartDate)} to ${formatDate(project.projectDeadline)}`;
    }
    return "No period set";
}

function formatAllowedPeriod({ min, max }) {
    if (min && max) {
        return `${formatDate(min)} to ${formatDate(max)}`;
    }
    if (min) {
        return `from ${formatDate(min)}`;
    }
    if (max) {
        return `until ${formatDate(max)}`;
    }
    return "not set on parent";
}

function formatDate(value) {
    return new Intl.DateTimeFormat(undefined, {
        day: "numeric",
        month: "short",
        year: "numeric"
    }).format(new Date(`${value}T00:00:00`));
}

function statusOptions() {
    return [
        { value: "NOT_STARTED", label: "Not started" },
        { value: "IN_PROGRESS", label: "In progress" },
        { value: "COMPLETED", label: "Completed" }
    ];
}

function priorityOptions() {
    return [
        { value: "LOW", label: "Low" },
        { value: "MEDIUM", label: "Medium" },
        { value: "HIGH", label: "High" }
    ];
}

function availableEmployeeOptions() {
    return state.availableEmployees.map((employee) => ({
        value: String(employee.employeeId),
        label: `${employee.username} (${roleLabel(employee.role)})`
    }));
}

function employeeOptions() {
    return [
        { value: "", label: "Unassigned" },
        ...state.projectMembers.map((employee) => ({
            value: String(employee.employeeId),
            label: `${employee.username} (${roleLabel(employee.role)})`
        }))
    ];
}

function isProjectOwner(employee) {
    return state.selectedProject?.owner?.employeeId === employee.employeeId;
}

function roleLabel(role) {
    return String(role || "")
        .replace("_", " ")
        .toLowerCase();
}

function formatTaskMeta(task) {
    const assignedTo = task.assignedEmployee?.username ? `Assigned to ${task.assignedEmployee.username}` : "Unassigned";
    return `${task.taskStatus} - ${task.taskPriority} - ${assignedTo}`;
}

function formatSubTaskMeta(subTask) {
    return `${subTask.subTaskStatus} - ${subTask.subTaskPriority} - ${subTask.subTaskDuration} days`;
}

function noteMeta(note) {
    return note ? `Note: ${note}` : "No note";
}

function emptyItem(text) {
    const item = document.createElement("div");
    item.className = "item muted";
    item.textContent = text;
    return item;
}

async function runWithLoading(container, text, action) {
    showLoading(container, text);
    setBusy(true);
    try {
        await action();
    } catch (error) {
        container.innerHTML = "";
        container.appendChild(emptyItem(error.message));
        showWorkspaceMessage(error.message);
    } finally {
        setBusy(false);
    }
}

async function runMutation(action) {
    setBusy(true);
    try {
        await action();
    } catch (error) {
        showWorkspaceMessage(error.message);
    } finally {
        setBusy(false);
    }
}

function showLoading(container, text) {
    container.innerHTML = "";
    const item = emptyItem(text);
    item.classList.add("loading");
    item.setAttribute("aria-busy", "true");
    container.appendChild(item);
}

function setBusy(isBusy) {
    state.isBusy = isBusy;
    document.querySelectorAll("button").forEach((button) => {
        if (!button.closest("#crudDialog")) {
            button.disabled = isBusy;
        }
    });
    loginButton.disabled = isBusy;
    registerButton.disabled = isBusy;
}

function setDialogBusy(isBusy) {
    crudDialog.querySelectorAll("button, input, select, textarea").forEach((control) => {
        control.disabled = isBusy;
    });
}

function showWorkspaceMessage(message) {
    workspaceMessage.textContent = message;
    window.clearTimeout(showWorkspaceMessage.timeout);
    showWorkspaceMessage.timeout = window.setTimeout(() => {
        workspaceMessage.textContent = "";
    }, 3500);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
