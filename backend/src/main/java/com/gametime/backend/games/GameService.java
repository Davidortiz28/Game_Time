package com.gametime.backend.games;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gametime.backend.common.BusinessRuleException;

@Service
public class GameService {

    private static final String COLUMNS =
            "id, name, launcher, executable_path, launch_arguments, process_name, category, description, enabled";

    private final JdbcClient jdbc;

    public GameService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Game> findAll() {
        return jdbc.sql("SELECT " + COLUMNS + " FROM games ORDER BY lower(name)")
                .query(GameService::map)
                .list();
    }

    public List<Game> findEnabled() {
        return jdbc.sql("SELECT " + COLUMNS + " FROM games WHERE enabled ORDER BY lower(name)")
                .query(GameService::map)
                .list();
    }

    public Optional<Game> findById(long id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM games WHERE id = :id")
                .param("id", id)
                .query(GameService::map)
                .optional();
    }

    @Transactional
    public long create(GameForm form) {
        normalizeAndValidate(form);
        try {
            return jdbc.sql("""
                    INSERT INTO games (name, launcher, executable_path, launch_arguments, process_name,
                                       category, description, enabled)
                    VALUES (:name, :launcher, :target, :args, :process, :category, :description, :enabled)
                    RETURNING id
                    """)
                    .paramSource(params(form))
                    .query(Long.class)
                    .single();
        } catch (DuplicateKeyException exception) {
            throw duplicate();
        }
    }

    @Transactional
    public void update(long id, GameForm form) {
        normalizeAndValidate(form);
        MapSqlParameterSource params = params(form).addValue("id", id);
        try {
            int updated = jdbc.sql("""
                    UPDATE games
                    SET name = :name, launcher = :launcher, executable_path = :target,
                        launch_arguments = :args, process_name = :process, category = :category,
                        description = :description, enabled = :enabled, updated_at = CURRENT_TIMESTAMP
                    WHERE id = :id
                    """)
                    .paramSource(params)
                    .update();
            if (updated == 0) {
                throw new BusinessRuleException("El juego ya no existe.");
            }
        } catch (DuplicateKeyException exception) {
            throw duplicate();
        }
    }

    @Transactional
    public void delete(long id) {
        jdbc.sql("DELETE FROM games WHERE id = :id").param("id", id).update();
    }

    static void normalizeAndValidate(GameForm form) {
        String target = form.getLaunchTarget().trim();
        form.setLaunchTarget(target);
        form.setName(form.getName().trim());
        form.setProcessName(form.getProcessName().trim());

        String prefix = form.getLauncher().getUriPrefix();
        if (prefix != null) {
            if (!target.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                throw new BusinessRuleException("Para " + form.getLauncher().getLabel()
                        + " la dirección de inicio debe empezar por " + prefix);
            }
            if (form.getLaunchArguments() != null && !form.getLaunchArguments().isBlank()) {
                throw new BusinessRuleException(
                        "Los argumentos solo se usan con ejecutables; inclúyelos en la dirección del launcher.");
            }
            return;
        }

        String lower = target.toLowerCase(Locale.ROOT);
        boolean absolute;
        try {
            absolute = Path.of(target).isAbsolute();
        } catch (RuntimeException exception) {
            absolute = false;
        }
        if (!absolute || !(lower.endsWith(".exe") || lower.endsWith(".lnk"))) {
            throw new BusinessRuleException(
                    "Para un ejecutable indica la ruta completa a un archivo .exe o a un acceso directo .lnk.");
        }
    }

    private static BusinessRuleException duplicate() {
        return new BusinessRuleException("Ya existe un juego con ese nombre para el mismo launcher.");
    }

    private static MapSqlParameterSource params(GameForm form) {
        return new MapSqlParameterSource()
                .addValue("name", form.getName())
                .addValue("launcher", form.getLauncher().name())
                .addValue("target", form.getLaunchTarget())
                .addValue("args", blankToNull(form.getLaunchArguments()))
                .addValue("process", form.getProcessName())
                .addValue("category", blankToNull(form.getCategory()))
                .addValue("description", blankToNull(form.getDescription()))
                .addValue("enabled", form.isEnabled());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Game map(ResultSet rs, int row) throws SQLException {
        return new Game(
                rs.getLong("id"),
                rs.getString("name"),
                Launcher.valueOf(rs.getString("launcher")),
                rs.getString("executable_path"),
                rs.getString("launch_arguments"),
                rs.getString("process_name"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getBoolean("enabled"));
    }
}
