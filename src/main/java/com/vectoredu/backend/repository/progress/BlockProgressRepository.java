package com.vectoredu.backend.repository.progress;

import com.vectoredu.backend.model.Block;
import com.vectoredu.backend.model.User;
import com.vectoredu.backend.model.progress.BlockProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockProgressRepository extends JpaRepository<BlockProgress, Long> {
    Optional<BlockProgress> findByBlockAndUser(Block block, User user);

    @Query("SELECT bp FROM BlockProgress bp WHERE bp.user = :user AND bp.block IN :blocks")
    List<BlockProgress> findProgressByUserAndBlocks(@Param("user") User user, @Param("blocks") List<Block> blocks);

    @Modifying
    @Transactional
    void deleteByBlockId(Long id);
}
