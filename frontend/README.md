# Frontend

This folder is the vanilla HTML, CSS and JavaScript frontend source.

The Spring Boot app serves the production entrypoint from `src/main/resources/static`.
Keep changes here and sync them into static assets before packaging the backend.

Current scope:
- JWT login
- Project, subproject, task and subtask lists
- Create, update and delete actions for project managers
- Project members panel with add and remove actions for project managers
- Status and note updates for assigned team members
- Loading states and API error feedback
- Task assignment dropdown based on `/api/projects/{projectId}/members`

Run the backend and open `http://localhost:8080` for the official app entrypoint.
