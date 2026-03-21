package com.frandm.studytracker.backend.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 20)
    private String color = "#ffffff";

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @Column(name = "weekly_goal_min")
    private Integer weeklyGoalMin = 0;

    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public Boolean getIsArchived() { return isArchived; }
    public Integer getWeeklyGoalMin() { return weeklyGoalMin; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setColor(String color) { this.color = color; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }
    public void setWeeklyGoalMin(Integer weeklyGoalMin) { this.weeklyGoalMin = weeklyGoalMin; }
}