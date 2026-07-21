
package com.example.pkveksamen.service;

import com.example.pkveksamen.model.Priority;
import com.example.pkveksamen.model.Status;
import com.example.pkveksamen.model.SubTask;
import com.example.pkveksamen.model.Task;
import com.example.pkveksamen.repository.EmployeeRepository;
import com.example.pkveksamen.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;

    public TaskService(TaskRepository taskRepository, EmployeeRepository employeeRepository) {
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
    }

    public Task createTask(Integer employeeId, long subProjectId, String taskName, String taskDescription
            , Status status, LocalDate startDate, LocalDate endDate, int taskDuration, Priority priority, String taskNote) {
        Task task = taskRepository.createTask( employeeId, subProjectId, taskName, taskDescription, status,
                startDate, endDate, taskDuration, priority, taskNote);
        if (task.getAssignedEmployee() != null) {
            task.getAssignedEmployee().setAlphaRoles(employeeRepository.findAlphaRolesByEmployeeId(task.getAssignedEmployee().getEmployeeId()));
        }
        return task;
    }

    public List<Task> showTaskByEmployeeId(int employeeId) {
        List<Task> tasks = taskRepository.showTaskByEmployeeId(employeeId);
        for (Task task : tasks) {
            if (task.getAssignedEmployee() != null) {
                task.getAssignedEmployee().setAlphaRoles(employeeRepository.findAlphaRolesByEmployeeId(task.getAssignedEmployee().getEmployeeId()));
            }
        }
        return tasks;
    }

    public List<Task> showTasksBySubProjectId(long subProjectId) {
        List<Task> tasks = taskRepository.showTasksBySubProjectId(subProjectId);
        for (Task task : tasks) {
            if (task.getAssignedEmployee() != null) {
                task.getAssignedEmployee().setAlphaRoles(employeeRepository.findAlphaRolesByEmployeeId(task.getAssignedEmployee().getEmployeeId()));
            }
        }
        return tasks;
    }

    public List<Task> showTasksBySubProjectIdAndEmployeeId(long subProjectId, int employeeId) {
        List<Task> tasks = taskRepository.showTasksBySubProjectIdAndEmployeeId(subProjectId, employeeId);
        for (Task task : tasks) {
            if (task.getAssignedEmployee() != null) {
                task.getAssignedEmployee().setAlphaRoles(employeeRepository.findAlphaRolesByEmployeeId(task.getAssignedEmployee().getEmployeeId()));
            }
        }
        return tasks;
    }

    public boolean taskBelongsToSubProject(long taskId, long subProjectId) {
        return taskRepository.taskBelongsToSubProject(taskId, subProjectId);
    }

    public boolean subTaskBelongsToTask(long subTaskId, long taskId) {
        return taskRepository.subTaskBelongsToTask(subTaskId, taskId);
    }

    public Task saveTask(Task task, int employeeId, long projectId, long subProjectId) {
        task.setTaskDuration(task.getTaskDuration()); // bare for sikkerhed
        task.recalculateDuration();
        return taskRepository.saveTask(task, employeeId, projectId, subProjectId);
    }

    public void deleteTask(long taskId) {
        taskRepository.deleteTask(taskId);
    }

    public void editTask(Task task) {
        taskRepository.editTask(task);
    }

    public Task getTaskById(long taskId) {
        return taskRepository.getTaskById(taskId);

    }

    public SubTask createSubTask(long taskId, String subTaskName, String subTaskDescription,
                              String subTaskStatus, LocalDate subTaskStartDate, LocalDate subTaskEndDate,
                              int subTaskDuration, String subTaskPriority, String subTaskNote) {
        return taskRepository.createSubTask(taskId, subTaskName, subTaskDescription, subTaskStatus,
                subTaskStartDate, subTaskEndDate, subTaskDuration, subTaskPriority, subTaskNote);
    }

    public SubTask saveSubTask(SubTask subTask, long taskId) {
        return taskRepository.saveSubTask(subTask, taskId);
    }

    public List<SubTask> showSubTasksByTaskId(long taskId) {
        return taskRepository.showSubTasksByTaskId(taskId);
    }

    public void deleteSubTask(long subTaskId) {
        taskRepository.deleteSubTask(subTaskId);
    }

    public void updateTaskNote(long taskId, String taskNote) {
        taskRepository.updateTaskNote(taskId, taskNote);
    }

    public void updateTaskStatus(long taskId, Status status) {
        taskRepository.updateTaskStatus(taskId, status.getDisplayName());
    }

    public void updateTaskPriority(long taskId, Priority priority) {
        taskRepository.updateTaskPriority(taskId, priority.getDisplayName());
    }

    public void updateSubTaskStatus(long subTaskId, Status status) {
        taskRepository.updateSubTaskStatus(subTaskId, status.getDisplayName());
    }

    public void updateSubTaskNote(long subTaskId, String subTaskNote) {
        taskRepository.updateSubTaskNote(subTaskId, subTaskNote);
    }

    public void updateSubTaskPriority(long subTaskId, Priority priority) {
        taskRepository.updateSubTaskPriority(subTaskId, priority.getDisplayName());
    }

    public void editSubTask(SubTask subTask) {
        taskRepository.editSubTask(subTask);
    }

    public SubTask getSubTaskById(long subTaskId) {
        return taskRepository.getSubTaskById(subTaskId);
    }
}
