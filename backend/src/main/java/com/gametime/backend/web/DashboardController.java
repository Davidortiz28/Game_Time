package com.gametime.backend.web;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gametime.backend.common.BusinessRuleException;
import com.gametime.backend.api.ExportService;
import com.gametime.backend.games.GameService;
import com.gametime.backend.sessions.PlaySession;
import com.gametime.backend.sessions.SessionService;

@Controller
public class DashboardController {

    private final SessionService sessions;
    private final GameService games;
    private final ExportService exports;

    public DashboardController(SessionService sessions, GameService games, ExportService exports) {
        this.sessions = sessions;
        this.games = games;
        this.exports = exports;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        sessions.advanceTimers();
        Optional<PlaySession> open = sessions.findOpen();
        List<PlaySession> recent = sessions.findRecent(5);

        model.addAttribute("openSession", open.orElse(null));
        model.addAttribute("openLaunches", open
                .map(s -> sessions.launchesBySession(List.of(s.id())).getOrDefault(s.id(), List.of()))
                .orElse(List.of()));
        model.addAttribute("pendingRequest", sessions.findPendingRequest().orElse(null));
        model.addAttribute("pendingExportRequest", exports.findPending().orElse(null));
        model.addAttribute("recentExportRequests", exports.findRecent(5));
        model.addAttribute("recentSessions", recent);
        model.addAttribute("launchableGames", games.countLaunchable());
        model.addAttribute("minMinutes", SessionService.MIN_MINUTES);
        model.addAttribute("maxMinutes", SessionService.MAX_MINUTES);
        return "dashboard";
    }

    @PostMapping("/sessions")
    public String create(@RequestParam int minutes, RedirectAttributes flash) {
        return run(flash, () -> sessions.create(minutes),
                "Sesión de " + minutes + " min creada. Pulsa «Iniciar temporizador» cuando el jugador esté listo.");
    }

    @PostMapping("/sessions/{id}/start")
    public String start(@PathVariable UUID id, RedirectAttributes flash) {
        return run(flash, () -> sessions.start(id), "Temporizador iniciado.");
    }

    @PostMapping("/sessions/{id}/end")
    public String end(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean confirm,
            RedirectAttributes flash) {
        if (!confirm) {
            flash.addFlashAttribute("error", "Marca la casilla de confirmación para terminar la sesión.");
            return "redirect:/";
        }
        return run(flash, () -> sessions.endByAdmin(id),
                "Sesión terminada. El jugador tiene 30 segundos para guardar antes de que se cierre el juego.");
    }

    @PostMapping("/requests/{id}/approve")
    public String approve(@PathVariable long id, @RequestParam int minutes, RedirectAttributes flash) {
        return run(flash, () -> sessions.approveRequest(id, minutes),
                "Solicitud aprobada: sesión de " + minutes + " min creada. Inicia el temporizador cuando corresponda.");
    }

    @PostMapping("/requests/{id}/reject")
    public String reject(
            @PathVariable long id,
            @RequestParam(required = false) String note,
            RedirectAttributes flash) {
        return run(flash, () -> sessions.rejectRequest(id, note), "Solicitud rechazada.");
    }

    @PostMapping("/exports/{id}/approve")
    public String approveExport(@PathVariable long id,
            @RequestParam(defaultValue = "false") boolean confirm,
            RedirectAttributes flash) {
        return run(flash, () -> exports.approve(id, confirm),
                "Exportación aprobada. El jugador ya puede descargar el CSV.");
    }

    @PostMapping("/exports/{id}/reject")
    public String rejectExport(@PathVariable long id,
            @RequestParam(defaultValue = "false") boolean confirm,
            @RequestParam(required = false) String note,
            RedirectAttributes flash) {
        return run(flash, () -> exports.reject(id, confirm, note), "Solicitud de exportación rechazada.");
    }

    @GetMapping("/history")
    public String history(Model model) {
        sessions.advanceTimers();
        List<PlaySession> recent = sessions.findRecent(50);
        model.addAttribute("sessions", recent);
        model.addAttribute("launches", sessions.launchesBySession(recent.stream().map(PlaySession::id).toList()));
        return "history";
    }

    private static String run(RedirectAttributes flash, Runnable action, String success) {
        try {
            action.run();
            flash.addFlashAttribute("notice", success);
        } catch (BusinessRuleException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/";
    }
}
