import {
    apiRequest,
    clearStoredToken,
    getApiBaseUrl,
    getStoredToken,
    setApiBaseUrl,
    setStoredToken
} from "./apiClient.js";

const apiBaseUrlInput = document.querySelector("#apiBaseUrl");
const usernameInput = document.querySelector("#username");
const passwordInput = document.querySelector("#password");
const loginButton = document.querySelector("#loginButton");
const logoutButton = document.querySelector("#logoutButton");
const refreshProjectsButton = document.querySelector("#refreshProjectsButton");
const createProjectButton = document.querySelector("#createProjectButton");
const addMemberButton = document.querySelector("#addMemberButton");
const createSubProjectButton = document.querySelector("#createSubProjectButton");
const createTaskButton = document.querySelector("#createTaskButton");
const createSubTaskButton = document.querySelector("#createSubTaskButton");
const loginMessage = document.querySelector("#loginMessage");
const workspaceMessage = document.querySelector("#workspaceMessage");
const loginPanel = document.querySelector("#loginPanel");
const workspace = document.querySelector("#workspace");
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
    selectedProject: null,
    selectedSubProject: null,
    selectedTask: null,
    pendingSubmit: null,
    isBusy: false
};

apiBaseUrlInput.value = getApiBaseUrl();

loginButton.addEventListener("click", login);
logoutButton.addEventListener("click", logout);
refreshProjectsButton.addEventListener("click", loadProjects);
createProjectButton.addEventListener("click", () => openProjectDialog());
addMemberButton.addEventListener("click", () => openAddMemberDialog());
createSubProjectButton.addEventListener("click", () => openSubProjectDialog());
createTaskButton.addEventListener("click", () => openTaskDialog());
createSubTaskButton.addEventListener("click", () => openSubTaskDialog());
closeDialogButton.addEventListener("click", () => crudDialog.close());
cancelDialogButton.addEventListener("click", () => crudDialog.close());
crudForm.addEventListener("submit", submitDialog);

if (getStoredToken()) {
    bootWorkspace();
}

async function login() {
    setApiBaseUrl(apiBaseUrlInput.value);
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

async function bootWorkspace() {
    showWorkspace();
    try {
        await loadCurrentEmployee();
        updateCreateButtons();
        await loadProjects();
    } catch (error) {
        showWorkspaceMessage(error.message);
    }
}

function logout() {
    clearStoredToken();
    loginPanel.hidden = false;
    workspace.hidden = true;
    logoutButton.hidden = true;
    workspaceMessage.textContent = "";
    resetState();
    renderAllLists();
}

function showWorkspace() {
    loginPanel.hidden = true;
    workspace.hidden = false;
    logoutButton.hidden = false;
}

async function loadCurrentEmployee() {
    state.currentEmployee = await apiRequest("/api/employees/me");
}

async function loadProjects() {
    await runWithLoading(projectsList, "Loading projects", async () => {
        clearSelections("project");
        state.projects = await apiRequest("/api/projects");
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

function renderAllLists() {
    renderProjects();
    renderMembers();
    renderSubProjects();
    renderTasks();
    renderSubTasks();
}

function renderProjects() {
    projectsList.innerHTML = "";
    selectedProjectTitle.textContent = state.selectedProject?.projectName || "Subprojects";

    if (state.projects.length === 0) {
        projectsList.appendChild(emptyItem("No projects yet"));
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

    if (!state.selectedProject) {
        membersList.appendChild(emptyItem("Select a project"));
        return;
    }
    if (state.projectMembers.length === 0) {
        membersList.appendChild(emptyItem("No members yet"));
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

    if (!state.selectedProject) {
        subProjectsList.appendChild(emptyItem("Select a project"));
        return;
    }
    if (state.subProjects.length === 0) {
        subProjectsList.appendChild(emptyItem("No subprojects yet"));
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

    if (!state.selectedSubProject) {
        tasksList.appendChild(emptyItem("Select a subproject"));
        return;
    }
    if (state.tasks.length === 0) {
        tasksList.appendChild(emptyItem("No tasks yet"));
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

    if (!state.selectedTask) {
        subTasksList.appendChild(emptyItem("Select a task"));
        return;
    }
    if (state.subTasks.length === 0) {
        subTasksList.appendChild(emptyItem("No subtasks yet"));
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

    openDialog({
        title: subProject ? "Edit subproject" : "Create subproject",
        fields: [
            textField("subProjectName", "Name", subProject?.subProjectName, true),
            dateField("subProjectStartDate", "Start date", subProject?.subProjectStartDate),
            dateField("subProjectDeadline", "Deadline", subProject?.subProjectDeadline),
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

    openDialog({
        title: task ? "Edit task" : "Create task",
        fields: [
            textField("taskName", "Name", task?.taskName, true),
            dateField("taskStartDate", "Start date", task?.taskStartDate),
            dateField("taskDeadline", "Deadline", task?.taskDeadline),
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

    openDialog({
        title: subTask ? "Edit subtask" : "Create subtask",
        fields: [
            textField("subTaskName", "Name", subTask?.subTaskName, true),
            dateField("subTaskStartDate", "Start date", subTask?.subTaskStartDate),
            dateField("subTaskDeadline", "Deadline", subTask?.subTaskDeadline),
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
    renderMembers();
    updateCreateButtons();
}

async function reloadSubProjects() {
    const path = subProjectsPath();
    clearSelections("subProject");
    state.subProjects = await apiRequest(path);
    renderAllLists();
    updateCreateButtons();
}

async function reloadTasks() {
    const path = tasksPath();
    clearSelections("task");
    state.tasks = await apiRequest(path);
    renderAllLists();
    updateCreateButtons();
}

async function reloadSubTasks() {
    state.subTasks = await apiRequest(subTasksPath());
    renderSubTasks();
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
    clearSelections("project");
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

function dateField(name, label, value = "") {
    return { name, label, value, type: "date" };
}

function textAreaField(name, label, value = "") {
    return { name, label, value, type: "textarea" };
}

function selectField(name, label, options, value = "", required = false) {
    return { name, label, options, value, required, type: "select" };
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
