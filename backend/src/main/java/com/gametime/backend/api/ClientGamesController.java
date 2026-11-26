package com.gametime.backend.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gametime.backend.common.BusinessRuleException;
import com.gametime.backend.games.Game;
import com.gametime.backend.games.GameForm;
import com.gametime.backend.games.GameService;

import jakarta.validation.Valid;

/** Player token may manage game entries, but cannot delete them. */
@RestController
@RequestMapping("/api/client/games")
public class ClientGamesController {

    private final GameService games;

    public ClientGamesController(GameService games) {
        this.games = games;
    }

    @GetMapping
    public List<Game> list() {
        return games.findAll();
    }

    @PostMapping
    public ResponseEntity<Game> create(@Valid @RequestBody GameForm form) {
        form.setProcessSupervisionApproved(false);
        long id = games.create(form);
        Game created = games.findById(id).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Game update(@PathVariable long id, @Valid @RequestBody GameForm form) {
        form.setProcessSupervisionApproved(false);
        games.update(id, form);
        return games.findById(id).orElseThrow();
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<Map<String, String>> rejected(BusinessRuleException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }
}
