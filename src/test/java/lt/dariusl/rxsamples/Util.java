package lt.dariusl.rxsamples;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

class Util {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
            .withZone( ZoneId.systemDefault() )
            .withLocale(Locale.US);

    static String time() {
        Instant instant = Instant.now();
        return FORMATTER.format(instant);
    }
}
