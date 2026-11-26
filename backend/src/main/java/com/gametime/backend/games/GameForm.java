package com.gametime.backend.games;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GameForm {

    @NotBlank(message = "Escribe el nombre del juego.")
    @Size(max = 200, message = "Máximo 200 caracteres.")
    private String name;

    @NotNull(message = "Elige el launcher.")
    private Launcher launcher = Launcher.STEAM;

    @NotBlank(message = "Indica cómo se inicia el juego.")
    @Size(max = 1000, message = "Máximo 1000 caracteres.")
    private String launchTarget;

    @Size(max = 1000, message = "Máximo 1000 caracteres.")
    private String launchArguments;

    @NotBlank(message = "Indica el proceso del juego, por ejemplo left4dead2.exe.")
    @Pattern(regexp = "^[\\w .()\\-]{1,250}\\.exe$", flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Debe ser solo el nombre del archivo .exe, sin carpetas.")
    private String processName;

    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String category;

    @Size(max = 2000, message = "Máximo 2000 caracteres.")
    private String description;

    private boolean enabled = true;

    private boolean processSupervisionApproved;

    public static GameForm from(Game game) {
        GameForm form = new GameForm();
        form.name = game.name();
        form.launcher = game.launcher();
        form.launchTarget = game.launchTarget();
        form.launchArguments = game.launchArguments();
        form.processName = game.processName();
        form.category = game.category();
        form.description = game.description();
        form.enabled = game.enabled();
        form.processSupervisionApproved = game.processSupervisionApproved();
        return form;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Launcher getLauncher() { return launcher; }
    public void setLauncher(Launcher launcher) { this.launcher = launcher; }
    public String getLaunchTarget() { return launchTarget; }
    public void setLaunchTarget(String launchTarget) { this.launchTarget = launchTarget; }
    public String getLaunchArguments() { return launchArguments; }
    public void setLaunchArguments(String launchArguments) { this.launchArguments = launchArguments; }
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isProcessSupervisionApproved() { return processSupervisionApproved; }
    public void setProcessSupervisionApproved(boolean approved) { this.processSupervisionApproved = approved; }
}
