package com.example.workflow.engine;

import com.example.workflow.dsl.ApprovalType;
import com.example.workflow.persistence.entity.UserTaskApprovalEntity;
import com.example.workflow.persistence.entity.UserTaskCandidateEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApprovalPolicyService {
    public boolean isCompleted(ApprovalType type, Integer minApprovals, List<UserTaskApprovalEntity> approvals, List<UserTaskCandidateEntity> candidates) {
        long approved = approvals.stream().filter(a -> "APPROVED".equalsIgnoreCase(a.getDecision())).count();
        return switch (type) {
            case ANY -> approved >= 1;
            case ALL -> approved >= Math.max(1, candidates.size());
            case MIN_APPROVAL -> approved >= Math.max(1, minApprovals == null ? 1 : minApprovals);
        };
    }
}
