package com.example.pkveksamen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "task")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id")
    private EmployeeEntity assignedEmployee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sub_project_id", nullable = false)
    private SubProjectEntity subProject;

    @Column(name = "task_title", nullable = false)
    private String title;

    @Column(name = "task_description")
    private String description;

    @Column(name = "task_status", nullable = false)
    private String status;

    @Column(name = "task_start_date")
    private LocalDate startDate;

    @Column(name = "task_deadline")
    private LocalDate deadline;

    @Column(name = "task_duration")
    private Integer duration;

    @Column(name = "task_priority")
    private String priority;

    @Column(name = "task_note")
    private String note;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EmployeeEntity getAssignedEmployee() {
        return assignedEmployee;
    }

    public void setAssignedEmployee(EmployeeEntity assignedEmployee) {
        this.assignedEmployee = assignedEmployee;
    }

    public SubProjectEntity getSubProject() {
        return subProject;
    }

    public void setSubProject(SubProjectEntity subProject) {
        this.subProject = subProject;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
