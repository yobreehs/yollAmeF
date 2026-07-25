package io.github.nexalloy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SharedFingerprintsProvider {
    public static List<String> getSharedFingerprints(String app) {
        return switch (app) {
            case "youtube", "music" -> Stream.of(
                    io.github.nexalloy.morphe.shared.misc.audio.tracks.FingerprintsKt.class,
                    io.github.nexalloy.morphe.shared.misc.debugging.FingerprintsKt.class,
                    io.github.nexalloy.morphe.shared.misc.initialization.FingerprintsKt.class,
                    io.github.nexalloy.morphe.shared.misc.litho.context.FingerprintsKt.class,
                    io.github.nexalloy.morphe.shared.misc.litho.filter.FingerprintsKt.class,
                    io.github.nexalloy.morphe.shared.FingerprintsKt.class
            ).map(Class::getName).toList();
            default -> new ArrayList<>();
        };
    }
}
