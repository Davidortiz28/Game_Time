package com.gametime.backend.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gametime.backend.common.BusinessRuleException;
import com.gametime.backend.games.Game;
import com.gametime.backend.games.GameForm;
import com.gametime.backend.games.GameService;
import com.gametime.backend.games.Launcher;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/games")
public class GamesController {

    private final GameService games;

    public GamesController(GameService games) {
        this.games = games;
    }

    @ModelAttribute("launchers")
    Launcher[] launchers() {
        return Launcher.values();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("games", games.findAll());
        return "games/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("game", new GameForm());
        model.addAttribute("gameId", null);
        return "games/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("game") GameForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (errors.hasErrors()) {
            model.addAttribute("gameId", null);
            return "games/form";
        }
        try {
            games.create(form);
        } catch (BusinessRuleException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("gameId", null);
            return "games/form";
        }
        flash.addFlashAttribute("notice", "Juego «" + form.getName() + "» agregado.");
        return "redirect:/games";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        Game game = games.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("game", GameForm.from(game));
        model.addAttribute("gameId", id);
        return "games/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("game") GameForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        model.addAttribute("gameId", id);
        if (errors.hasErrors()) {
            return "games/form";
        }
        try {
            games.update(id, form);
        } catch (BusinessRuleException exception) {
            model.addAttribute("error", exception.getMessage());
            return "games/form";
        }
        flash.addFlashAttribute("notice", "Cambios guardados en «" + form.getName() + "».");
        return "redirect:/games";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable long id,
            @RequestParam(defaultValue = "false") boolean confirm,
            RedirectAttributes flash) {
        if (!confirm) {
            flash.addFlashAttribute("error", "Marca la casilla de confirmación para eliminar el juego.");
            return "redirect:/games/" + id + "/edit";
        }
        games.delete(id);
        flash.addFlashAttribute("notice", "Juego eliminado. El historial de sesiones conserva su nombre.");
        return "redirect:/games";
    }
}
