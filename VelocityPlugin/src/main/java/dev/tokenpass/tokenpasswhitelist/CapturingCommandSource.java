package dev.tokenpass.tokenpasswhitelist;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CapturingCommandSource implements CommandSource {
    private final CommandSource delegate;
    private final List<String> capturedOutput = new CopyOnWriteArrayList<>();

    public CapturingCommandSource(CommandSource delegate) {
        this.delegate = delegate;
    }

    public List<String> getCapturedOutput() {
        return capturedOutput;
    }

    @Override
    public void sendMessage(Component message) {
        capturedOutput.add(PlainTextComponentSerializer.plainText().serialize(message));
    }

    @Override
    public void sendMessage(Identity source, Component message) {
        capturedOutput.add(PlainTextComponentSerializer.plainText().serialize(message));
    }

    @Override
    public Tristate getPermissionValue(String permission) {
        return delegate.getPermissionValue(permission);
    }
}
