package com.frandm.studytracker.backend.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "is_favorite")
    private Boolean isFavorite = false;

    @Column(name = "weekly_goal_min")
    private Integer weeklyGoalMin = 0;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Session> sessions;

    public Long getId() { return id; }
    public Tag getTag() { return tag; }
    public String getName() { return name; }
    public Boolean getIsFavorite() { return isFavorite; }
    public Integer getWeeklyGoalMin() { return weeklyGoalMin; }

    public void setId(Long id) { this.id = id; }
    public void setTag(Tag tag) { this.tag = tag; }
    public void setName(String name) { this.name = name; }
    public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }
    public void setWeeklyGoalMin(Integer weeklyGoalMin) { this.weeklyGoalMin = weeklyGoalMin; }
}