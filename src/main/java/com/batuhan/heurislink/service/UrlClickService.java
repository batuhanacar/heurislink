package com.batuhan.heurislink.service;

import com.batuhan.heurislink.entity.ShortUrl;
import com.batuhan.heurislink.entity.UrlClick;
import com.batuhan.heurislink.repository.UrlClickRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UrlClickService {

    private final UrlClickRepository urlClickRepository;

    @Transactional
    public void recordClick(ShortUrl shortUrl) {
        UrlClick urlClick = new UrlClick(shortUrl);

        urlClickRepository.save(urlClick);
    }

    @Transactional(readOnly = true)
    public long getClickCount(ShortUrl shortUrl) {
        return urlClickRepository.countByShortUrlId(shortUrl.getId());
    }
}