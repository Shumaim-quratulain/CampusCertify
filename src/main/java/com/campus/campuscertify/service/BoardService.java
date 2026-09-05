package com.campus.campuscertify.service;

import com.campus.campuscertify.domain.Activity;
import com.campus.campuscertify.domain.EvaluationResponse;
import com.campus.campuscertify.domain.EvaluationSummary;
import com.campus.campuscertify.domain.Participant;
import com.campus.campuscertify.domain.ParticipantResult;
import com.campus.campuscertify.domain.ValidationError;
import com.campus.campuscertify.state.BoardState;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BoardService {

    /**
     * Boolean natural order is false &lt; true, so reverseOrder on the eligible key puts eligible first.
     * Do not use .reversed() here — it would reverse the id ordering too.
     */
    private static final Comparator<ParticipantResult> RESULT_ORDER =
            Comparator.comparing(ParticipantResult::eligible, Comparator.reverseOrder())
                    .thenComparing(ParticipantResult::participantId);

    private final BoardState state;
    private final ParticipantValidator validator;
    private final EligibilityEvaluator evaluator;

    public BoardService(BoardState state, ParticipantValidator validator, EligibilityEvaluator evaluator) {
        this.state = state;
        this.validator = validator;
        this.evaluator = evaluator;
    }

    public List<Activity> activities() {
        return state.activities();
    }

    public List<Participant> participants() {
        return state.participants();
    }

    public void addOrUpdate(Participant participant) {
        state.upsert(participant);
    }

    public boolean deleteParticipant(String id) {
        return state.remove(id);
    }

    public void reset() {
        state.reset();
    }

    public EvaluationResponse evaluate() {
        List<Participant> participants = state.participants();
        Map<String, Activity> activityIndex = state.activityIndex();

        List<ValidationError> errors = validator.validate(participants, activityIndex);
        if (!errors.isEmpty()) {
            return EvaluationResponse.ofErrors(errors);
        }

        List<ParticipantResult> results = participants.stream()
                .map(participant -> evaluator.evaluate(participant, activityIndex))
                .sorted(RESULT_ORDER)
                .toList();

        long eligibleCount = results.stream().filter(ParticipantResult::eligible).count();
        EvaluationSummary summary =
                new EvaluationSummary((int) eligibleCount, results.size() - (int) eligibleCount);

        return EvaluationResponse.ofResults(results, summary);
    }
}
