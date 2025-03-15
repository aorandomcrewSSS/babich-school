package com.vectoredu.backend.model.progress;

import com.vectoredu.backend.model.Block;
import com.vectoredu.backend.model.User;
import com.vectoredu.backend.model.enums.ProgressStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "block_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"block_id", "user_id"}))
public class BlockProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "block_id")
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressStatus status;

    public BlockProgress(Block block, User user, ProgressStatus status) {
        this.block = block;
        this.user = user;
        this.status = status;
    }
}


