package com.bookmuse.app.service;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.mapper.Views;
import com.bookmuse.app.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class RecommendationService {
    private final RecommendationRepository repo;
    private final AccessService access;
    public Responses.PageView<Responses.BookView> recommendations(Pageable page) { return Responses.PageView.of(repo.recommend(access.id(),page).map(Views::book)); }
}
