package com.example.pkveksamen.repository;

import com.example.pkveksamen.entity.EmployeeEntity;
import com.example.pkveksamen.entity.SubProjectEntity;
import com.example.pkveksamen.entity.SubTaskEntity;
import com.example.pkveksamen.entity.TaskEntity;
import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.model.EmployeeRole;
import com.example.pkveksamen.model.SubTask;
import com.example.pkveksamen.model.Task;
import com.example.pkveksamen.model.Priority;
import com.example.pkveksamen.model.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Repository
public class TaskRepository {
    private final JdbcTemplate jdbcTemplate;
    private final TaskJpaRepository taskJpaRepository;
    private final SubTaskJpaRepository subTaskJpaRepository;
    private final SubProjectJpaRepository subProjectJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public TaskRepository(JdbcTemplate jdbcTemplate,
                          TaskJpaRepository taskJpaRepository,
                          SubTaskJpaRepository subTaskJpaRepository,
                          SubProjectJpaRepository subProjectJpaRepository,
                          EmployeeJpaRepository employeeJpaRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.taskJpaRepository = taskJpaRepository;
        this.subTaskJpaRepository = subTaskJpaRepository;
        this.subProjectJpaRepository = subProjectJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    public Task createTask(Integer employeeId, long subProjectId, String taskName, String taskDescription,
                           Status status, LocalDate taskStartDate, LocalDate taskDeadline, int taskDuration,
                           Priority priority, String taskNote) {

        Task task = new Task();
        task.setTaskName(taskName);
        task.setTaskDescription(taskDescription);
        task.setTaskStatus(status);
        task.setTaskStartDate(taskStartDate);
        task.setTaskDeadline(taskDeadline);
        task.setTaskDuration(taskDuration);
        task.setTaskPriority(priority);
        task.setTaskNote(taskNote);
        return saveTask(task, employeeId, 0, subProjectId);
    }

    public List<Task> showTaskByEmployeeId(int employeeId) {
        return taskJpaRepository.findByAssignedEmployeeIdOrderById(employeeId)
                .stream()
                .map(this::toTaskModel)
                .toList();
    }

    public List<Task> showTasksBySubProjectId(long subProjectId) {
        return taskJpaRepository.findBySubProjectIdOrderById(subProjectId)
                .stream()
                .map(this::toTaskModel)
                .toList();
    }

    public List<Task> showTasksBySubProjectIdAndEmployeeId(long subProjectId, int employeeId) {
        return taskJpaRepository.findBySubProjectIdAndAssignedEmployeeIdOrderById(subProjectId, employeeId)
                .stream()
                .map(this::toTaskModel)
                .toList();
    }

    public boolean taskBelongsToSubProject(long taskId, long subProjectId) {
        return taskJpaRepository.existsByIdAndSubProjectId(taskId, subProjectId);
    }

    public boolean subTaskBelongsToTask(long subTaskId, long taskId) {
        return subTaskJpaRepository.existsByIdAndTaskId(subTaskId, taskId);
    }

    public Task saveTask(Task task, Integer employeeId, long projectId, long subProjectId) {
        SubProjectEntity subProject = subProjectJpaRepository.findById(subProjectId)
                .orElseThrow(() -> new NoSuchElementException("Subproject not found: " + subProjectId));
        EmployeeEntity assignedEmployee = employeeId != null ? employeeJpaRepository.findById((long) employeeId).orElse(null) : null;
        task.recalculateDuration();

        TaskEntity entity = new TaskEntity();
        entity.setAssignedEmployee(assignedEmployee);
        entity.setSubProject(subProject);
        entity.setTitle(task.getTaskName());
        entity.setDescription(task.getTaskDescription());
        entity.setStatus((task.getTaskStatus() != null ? task.getTaskStatus() : Status.NOT_STARTED).name());
        entity.setStartDate(task.getTaskStartDate());
        entity.setDeadline(task.getTaskDeadline());
        entity.setDuration(task.getTaskDuration());
        entity.setPriority((task.getTaskPriority() != null ? task.getTaskPriority() : Priority.MEDIUM).name());
        entity.setNote(task.getTaskNote());

        TaskEntity saved = taskJpaRepository.save(entity);
        task.setTaskID(saved.getId().intValue());
        return toTaskModel(saved);
    }

    public void deleteTask(long taskId) {
        taskJpaRepository.deleteById(taskId);
    }

    public void editTask(Task task) {
        TaskEntity entity = taskJpaRepository.findById((long) task.getTaskID())
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + task.getTaskID()));
        EmployeeEntity assignedEmployee = task.getAssignedEmployee() != null
                ? employeeJpaRepository.findById((long) task.getAssignedEmployee().getEmployeeId()).orElse(null)
                : null;
        task.recalculateDuration();

        entity.setTitle(task.getTaskName());
        entity.setDescription(task.getTaskDescription());
        entity.setStatus(task.getTaskStatus().name());
        entity.setStartDate(task.getTaskStartDate());
        entity.setDeadline(task.getTaskDeadline());
        entity.setDuration(task.getTaskDuration());
        entity.setPriority(task.getTaskPriority().name());
        entity.setNote(task.getTaskNote());
        entity.setAssignedEmployee(assignedEmployee);

        taskJpaRepository.save(entity);
    }

    public Task getTaskById(long taskId) {
        return taskJpaRepository.findById(taskId)
                .map(this::toTaskModel)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
    }

    public SubTask saveSubTask(SubTask subTask, long subTaskId) {
        subTask.recalculateDuration();
        TaskEntity task = taskJpaRepository.findById(subTaskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + subTaskId));
        SubTaskEntity entity = new SubTaskEntity();
        applySubTaskModel(entity, subTask);
        entity.setTask(task);
        SubTaskEntity saved = subTaskJpaRepository.save(entity);
        subTask.setSubTaskId(saved.getId());
        return toSubTaskModel(saved);
    }

    public SubTask createSubTask(long taskId, String subTaskName, String subTaskDescription,
                              String subTaskStatus, LocalDate subTaskStartDate, LocalDate subTaskDeadline,
                              int subTaskDuration, String subTaskPriority, String subTaskNote) {

        TaskEntity task = taskJpaRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));

        SubTask subTask = new SubTask();
        subTask.setSubTaskName(subTaskName);
        subTask.setSubTaskDescription(subTaskDescription);
        subTask.setSubTaskStatus(Status.fromDisplayName(subTaskStatus));
        subTask.setSubTaskStartDate(subTaskStartDate);
        subTask.setSubTaskDeadline(subTaskDeadline);
        subTask.setSubTaskDuration(subTaskDuration);
        subTask.setSubTaskPriority(Priority.fromDisplayName(subTaskPriority));
        subTask.setSubTaskNote(subTaskNote);
        subTask.recalculateDuration();

        SubTaskEntity entity = new SubTaskEntity();
        applySubTaskModel(entity, subTask);
        entity.setTask(task);
        SubTaskEntity saved = subTaskJpaRepository.save(entity);
        subTask.setSubTaskId(saved.getId());
        return toSubTaskModel(saved);
    }

    public List<SubTask> showSubTasksByTaskId(long taskId) {
        return subTaskJpaRepository.findByTaskIdOrderById(taskId)
                .stream()
                .map(this::toSubTaskModel)
                .toList();
    }

    public void deleteSubTask(long subTaskId) {
        subTaskJpaRepository.deleteById(subTaskId);
    }

    public void updateTaskNote(long taskId, String taskNote) {
        TaskEntity entity = taskJpaRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
        entity.setNote(taskNote);
        taskJpaRepository.save(entity);
    }

    public void updateTaskStatus(long taskId, String taskStatus) {
        TaskEntity entity = taskJpaRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
        entity.setStatus(taskStatus);
        taskJpaRepository.save(entity);
    }

    public void updateTaskPriority(long taskId, String taskPriority) {
        TaskEntity entity = taskJpaRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
        entity.setPriority(taskPriority);
        taskJpaRepository.save(entity);
    }

    public void updateSubTaskStatus(long subTaskId, String subTaskStatus) {
        SubTaskEntity entity = subTaskJpaRepository.findById(subTaskId)
                .orElseThrow(() -> new NoSuchElementException("Subtask not found: " + subTaskId));
        entity.setStatus(subTaskStatus);
        subTaskJpaRepository.save(entity);
    }

    public void updateSubTaskPriority(long subTaskId, String subTaskPriority) {
        SubTaskEntity entity = subTaskJpaRepository.findById(subTaskId)
                .orElseThrow(() -> new NoSuchElementException("Subtask not found: " + subTaskId));
        entity.setPriority(subTaskPriority);
        subTaskJpaRepository.save(entity);
    }

    public void updateSubTaskNote(long subTaskId, String subTaskNote) {
        SubTaskEntity entity = subTaskJpaRepository.findById(subTaskId)
                .orElseThrow(() -> new NoSuchElementException("Subtask not found: " + subTaskId));
        entity.setNote(subTaskNote);
        subTaskJpaRepository.save(entity);
    }



    public void editSubTask(SubTask subTask) {
        subTask.recalculateDuration();
        SubTaskEntity entity = subTaskJpaRepository.findById(subTask.getSubTaskId())
                .orElseThrow(() -> new NoSuchElementException("Subtask not found: " + subTask.getSubTaskId()));
        applySubTaskModel(entity, subTask);
        subTaskJpaRepository.save(entity);
    }

    public SubTask getSubTaskById(long subTaskId) {
        return subTaskJpaRepository.findById(subTaskId)
                .map(this::toSubTaskModel)
                .orElseThrow(() -> new NoSuchElementException("Subtask not found: " + subTaskId));
    }

    private Task toTaskModel(TaskEntity entity) {
        Task task = new Task();
        task.setTaskID(entity.getId().intValue());
        task.setTaskName(entity.getTitle());
        task.setTaskDescription(entity.getDescription());
        task.setTaskStatus(Status.fromDisplayName(entity.getStatus()));
        task.setTaskNote(entity.getNote());
        task.setTaskStartDate(entity.getStartDate());
        task.setTaskDeadline(entity.getDeadline());
        task.setTaskDuration(entity.getDuration() != null ? entity.getDuration() : 0);
        if (entity.getPriority() != null) {
            task.setTaskPriority(Priority.fromDisplayName(entity.getPriority()));
        }
        task.recalculateDuration();

        if (entity.getAssignedEmployee() != null) {
            task.setAssignedEmployee(toEmployeeModel(entity.getAssignedEmployee()));
        }

        return task;
    }

    private Employee toEmployeeModel(EmployeeEntity entity) {
        Employee employee = new Employee();
        employee.setEmployeeId(entity.getId().intValue());
        employee.setUsername(entity.getUsername());
        employee.setEmail(entity.getEmail());
        employee.setRole(EmployeeRole.fromDisplayName(entity.getRole()));
        return employee;
    }

    private void applySubTaskModel(SubTaskEntity entity, SubTask subTask) {
        entity.setTitle(subTask.getSubTaskName());
        entity.setDescription(subTask.getSubTaskDescription());
        entity.setStatus((subTask.getSubTaskStatus() != null ? subTask.getSubTaskStatus() : Status.NOT_STARTED).name());
        entity.setStartDate(subTask.getSubTaskStartDate());
        entity.setDeadline(subTask.getSubTaskDeadline());
        entity.setDuration(subTask.getSubTaskDuration());
        entity.setPriority((subTask.getSubTaskPriority() != null ? subTask.getSubTaskPriority() : Priority.MEDIUM).name());
        entity.setNote(subTask.getSubTaskNote());
    }

    private SubTask toSubTaskModel(SubTaskEntity entity) {
        SubTask subTask = new SubTask();
        subTask.setSubTaskId(entity.getId());
        subTask.setSubTaskName(entity.getTitle());
        subTask.setSubTaskDescription(entity.getDescription());
        subTask.setSubTaskStatus(Status.fromDisplayName(entity.getStatus()));
        subTask.setSubTaskStartDate(entity.getStartDate());
        subTask.setSubTaskDeadline(entity.getDeadline());
        subTask.setSubTaskDuration(entity.getDuration() != null ? entity.getDuration() : 0);
        if (entity.getPriority() != null) {
            subTask.setSubTaskPriority(Priority.fromDisplayName(entity.getPriority()));
        }
        subTask.setSubTaskNote(entity.getNote());
        subTask.recalculateDuration();
        return subTask;
    }
}
